package com.opentext.partners.service;

import com.opentext.partners.model.JoinedPartnerDto.PartnerSolution;
import com.opentext.partners.model.JoinedPartnerDto.Solution;
import com.opentext.partners.model.PartnerModels.PartnerDirectoryRoot;
import com.opentext.partners.model.PartnerModels.RawPartner;
import com.opentext.partners.model.SolutionModels.RawSolution;
import com.opentext.partners.model.SolutionModels.SolutionCatalogRoot;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.time.Duration;

@Slf4j
@Service
public class PartnerService {

    /**
     * Partner and Solution data source URLs.
     * Support pagination via the 'max' parameter.
     */
    private static final String PARTNERS_URL_TEMPLATE =
            "https://www.opentext.com/en/partners/partners-directory-overview/1716790338234.ajax?q=&start=%d&max=%d&sorter=Default_Sort";
    private static final String SOLUTIONS_URL_TEMPLATE =
            "https://www.opentext.com/en/partners/ApplicationMarketplace/1754971906819.ajax?q=&start=%d&max=%d&sorter=Name";

    private static final int BATCH_SIZE = 200; // Fetch in manageable batches

    private static final Duration API_TIMEOUT = Duration.ofSeconds(50);

    private final WebClient webClient;

    /**
     * Cached in-memory Data: joined partner-solution data. Ensures thread-safe.
     * volatile ensures that changes made by one thread (the refresh thread) are immediately visible to others
     * */
    private volatile List<PartnerSolution> joinedPartnersCache = Collections.emptyList();

    public PartnerService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /** Runs once after startup to initialize cache */
    @PostConstruct
    public void loadAndJoinData() {
        refreshPartnerSolutionData();
    }

    /** Scheduled refresh : The task will run every 3rd minute*/
    @Scheduled(cron = "0 */3 * * * *")
    public void scheduledDataRefresh() {
        log.info("Scheduled partner-solution data refresh triggered...");
        refreshPartnerSolutionData();
    }

    /** Refreshes the partner-solution cache */
    private void refreshPartnerSolutionData() {
        log.info("Starting data load & join process...");

        Mono<List<RawPartner>> partnersMono = fetchAllPartners();
        Mono<List<RawSolution>> solutionsMono = fetchAllSolutions();

        Mono.zip(partnersMono, solutionsMono)
                .map(tuple -> joinPartnerAndSolution(tuple.getT1(), tuple.getT2()))
                .subscribe(
                        joinedList -> {
                            this.joinedPartnersCache = joinedList;
                            log.info("Cached {} joined partners successfully.", joinedList.size());
                        },
                        error -> log.error("Failed to refresh partner/solution data!", error)
                );
    }

    /** Joins partners with solutions (case-insensitive) */
    private List<PartnerSolution> joinPartnerAndSolution(List<RawPartner> partners, List<RawSolution> solutions) {
        log.info("Joining {} partners with {} solutions...", partners.size(), solutions.size());

        // --- ASSUMPTIONS ---
        log.info("JOIN ASSUMPTIONS:");
        log.info("1. Join key: Partner.name ↔ Solution.partnerName (case-insensitive).");
        log.info("2. A partner can have zero, one, or multiple solutions.");
        log.info("3. Null or blank names are ignored in joining.");
        log.info("4. Solutions without valid partnerName remain unlinked.");
        log.info("5. The joined data is cached in-memory for API pagination.");

        Map<String, List<RawSolution>> solutionsByPartner = solutions.stream()
                .filter(s -> s.partnerName() != null && !s.partnerName().isBlank())
                .collect(Collectors.groupingBy(s -> s.partnerName().toLowerCase(Locale.ROOT)));

        return partners.stream()
                .map(partner -> {
                    String partnerKey = partner.name() != null ? partner.name().toLowerCase(Locale.ROOT) : "";
                    List<Solution> partnerSolutions = solutionsByPartner
                            .getOrDefault(partnerKey, Collections.emptyList())
                            .stream()
                            .map(raw -> new Solution(raw.displayName(), raw.shortDescription()))
                            .toList();

                    return new PartnerSolution(
                            partner.name(),
                            partner.id(),
                            partner.partnerLevel(),
                            partner.partnerType(),
                            partner.shortDescription(),
                            partner.companyOverview(),
                            partnerSolutions
                    );
                })
                .toList();
    }

    /** Fetches all partners with batched pagination
     * Mono: represents a single asynchronous result (list of partners/solutions).
     * Flux: represents multiple asynchronous streams (used to combine batches).
     * */
    private Mono<List<RawPartner>> fetchAllPartners() {
        String initialUrl = String.format(PARTNERS_URL_TEMPLATE, 0, 1);
        return webClient.get().uri(initialUrl)
                .retrieve()
                .bodyToMono(PartnerDirectoryRoot.class).timeout(API_TIMEOUT)
                .flatMap(initial -> {
                    int total = safeParseInt(initial.total());
                    if (total == 0) return Mono.just(Collections.<RawPartner>emptyList());

                    List<Mono<List<RawPartner>>> batchMonos = new ArrayList<>();
                    for (int start = 0; start < total; start += BATCH_SIZE) {
                        String url = String.format(PARTNERS_URL_TEMPLATE, start, BATCH_SIZE);
                        log.info("Partner: Fetch data start: {} and the url: {}", start, url);
                        Mono<List<RawPartner>> batchMono = webClient.get()
                                .uri(url)
                                .retrieve()
                                .bodyToMono(PartnerDirectoryRoot.class)
                                .map(PartnerDirectoryRoot::getAllPartners)
                                .onErrorReturn(Collections.<RawPartner>emptyList());
                        batchMonos.add(batchMono);
                    }

                    // Merge batches type-safely
                    return Flux.fromIterable(batchMonos)
                            .flatMap(batch -> batch.flatMapMany(Flux::fromIterable))
                            .collectList();
                })
                .onErrorReturn(Collections.<RawPartner>emptyList());
    }

    /** Fetches all solutions with batched pagination
     * Mono: represents a single asynchronous result (list of partners/solutions).
     * Flux: represents multiple asynchronous streams (used to combine batches).
     * */
    private Mono<List<RawSolution>> fetchAllSolutions() {
        String initialUrl = String.format(SOLUTIONS_URL_TEMPLATE, 0, 1);
        return webClient.get().uri(initialUrl)
                .retrieve()
                .bodyToMono(SolutionCatalogRoot.class).timeout(API_TIMEOUT)
                .flatMap(initial -> {
                    int total = safeParseInt(initial.total());
                    if (total == 0) return Mono.just(Collections.<RawSolution>emptyList());

                    List<Mono<List<RawSolution>>> batchMonos = new ArrayList<>();
                    for (int start = 0; start < total; start += BATCH_SIZE) {
                        String url = String.format(SOLUTIONS_URL_TEMPLATE, start, BATCH_SIZE);
                        log.info("Solution: Fetch data start: {} and the url: {}", start, url);
                        Mono<List<RawSolution>> batchMono = webClient.get()
                                .uri(url)
                                .retrieve()
                                .bodyToMono(SolutionCatalogRoot.class)
                                .map(SolutionCatalogRoot::getAllSolutions)
                                .onErrorReturn(Collections.<RawSolution>emptyList());
                        batchMonos.add(batchMono);
                    }

                    return Flux.fromIterable(batchMonos)
                            .flatMap(batch -> batch.flatMapMany(Flux::fromIterable))
                            .collectList();
                })
                .onErrorReturn(Collections.<RawSolution>emptyList());
    }

    /** Safe parsing of integer 'total' fields */
    private int safeParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.warn("Invalid total value '{}', defaulting to 0", value);
            return 0;
        }
    }

    /**
     * Example: Redis Caching (Commented out)
     * Redis is useful for fast, distributed, in-memory caching to reduce API/db load
     * and provide low-latency access to frequently used data.
     *
     * If we wanted to cache the joined partners in Redis:
     *
     * @Cacheable(value = "joinedPartners")
     * public List<PartnerSolution> getJoinedPartners() {
     *     return List.copyOf(this.joinedPartnersCache);
     * }
     *
     * To refresh the cache whenever data updates (every 3 minutes in our scheduler):
     *
     * @CachePut(value = "joinedPartners", unless = "#result == null")
     * private List<PartnerSolution> refreshCache(List<PartnerSolution> updatedList) {
     *     return updatedList;
     * }
     *
     * Note: We are not using Redis now, as our in-memory volatile list is sufficient for this use case.
     */

    /** Returns cached joined partners */
    public List<PartnerSolution> getJoinedPartners() {
        return List.copyOf(this.joinedPartnersCache);
    }

    /** Returns paginated partners with optional filtering for those with solutions */
    public Page<PartnerSolution> getPartners(Pageable pageable, boolean hasSolutions) {
        List<PartnerSolution> filtered = joinedPartnersCache.stream()
                .filter(p -> !hasSolutions || (p.solutions() != null && !p.solutions().isEmpty()))
                .toList();

        int total = filtered.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);

        if (start >= total) return Page.empty(pageable);

        return new PageImpl<>(filtered.subList(start, end), pageable, total);
    }
}

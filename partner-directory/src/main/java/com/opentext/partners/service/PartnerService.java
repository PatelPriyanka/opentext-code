package com.opentext.partners.service;

import com.opentext.partners.model.JoinedPartnerDto.PartnerSolution;
import com.opentext.partners.model.JoinedPartnerDto.Solution;
import com.opentext.partners.model.PartnerModels.PartnerDirectoryRoot;
import com.opentext.partners.model.PartnerModels.RawPartner;
import com.opentext.partners.model.SolutionModels.RawSolution;
import com.opentext.partners.model.SolutionModels.SolutionCatalogRoot;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// Removed unused @Cacheable import
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PartnerService {

    private static final Logger log = LoggerFactory.getLogger(PartnerService.class);

    // --- UPDATED: URLs are now templates with a %d placeholder ---
    private static final String PARTNERS_URL_TEMPLATE =
            "https://www.opentext.com/en/partners/partners-directory-overview/1716790338234.ajax?q=&start=0&max=%d&sorter=Default_Sort";
    private static final String SOLUTIONS_URL_TEMPLATE =
            "https://www.opentext.com/en/partners/ApplicationMarketplace/1754971906819.ajax?q=&start=0&max=%d&sorter=Name";

    private final WebClient webClient;

    // Using volatile for thread-safe writes to the cache after initialization
    private volatile List<PartnerSolution> joinedPartnersCache = Collections.emptyList();

    /**
     * --- NEW / FIXED CONSTRUCTOR ---
     * This injects the WebClient.Builder and initializes the webClient field.
     */
    public PartnerService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * --- UPDATED ---
     * This method runs on application startup.
     */
    @PostConstruct
    public void loadAndJoinData() {
        log.info("Starting dynamic data load and join...");

        // 1. Call the new dynamic fetch methods
        Mono<List<RawPartner>> partnersMono = fetchAllPartners();
        Mono<List<RawSolution>> solutionsMono = fetchAllSolutions();

        // 2. Zip results and perform the join (your original logic)
        Mono.zip(partnersMono, solutionsMono)
                .map(tuple -> {
                    List<RawPartner> partners = tuple.getT1();
                    List<RawSolution> solutions = tuple.getT2();

                    log.info("Successfully fetched {} partners and {} solutions.", partners.size(), solutions.size());
                    log.info("--- JOIN ASSUMPTIONS (for Q-2) ---");
                    log.info("1. Joining on Partner Name: 'Partner.Name' and 'Solution.solutionpartnername'.");
                    log.info("2. Case-Sensitivity: The join is case-sensitive.");
                    log.info("3. Cardinality: A partner can have zero, one, or many solutions.");
                    log.info("--- END ASSUMPTIONS ---");

                    Map<String, List<RawSolution>> solutionsByPartner = solutions.stream()
                            .filter(s -> s.partnerName() != null && !s.partnerName().isBlank())
                            .collect(Collectors.groupingBy(RawSolution::partnerName));

                    return partners.stream()
                            .map(partner -> {
                                List<Solution> partnerSolutions = solutionsByPartner
                                        .getOrDefault(partner.name(), Collections.emptyList())
                                        .stream()
                                        .map(raw -> new Solution(raw.displayName(), raw.shortDescription()))
                                        .toList();

                                // Using your JoinedPartnerDto structure
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
                })
                .subscribe(
                        joinedList -> {
                            this.joinedPartnersCache = joinedList; // Populate the cache
                            log.info("Successfully cached {} joined partners.", this.joinedPartnersCache.size());
                        },
                        error -> {
                            log.error("Failed to load or join partner data on startup!", error);
                        }
                );
    }

    /**
     * --- NEW METHOD ---
     * Exposes the full list of joined partners from the cache for the /partners/joined-json endpoint (Q-2).
     *
     * @return An immutable copy of the full list of joined PartnerSolution objects.
     */
    public List<PartnerSolution> getJoinedPartners() {
        return List.copyOf(this.joinedPartnersCache);
    }

    /**
     * --- NEW METHOD with GENERIC FIX ---
     * Dynamically fetches ALL partners from the API.
     * 1. Fetches 1 record to get the 'total' count.
     * 2. Uses the 'total' to fetch all records.
     */
    private Mono<List<RawPartner>> fetchAllPartners() {
        String initialUrl = String.format(PARTNERS_URL_TEMPLATE, 1);
        log.info("Fetching total partner count from: {}", initialUrl);

        return this.webClient.get().uri(initialUrl)
                .retrieve()
                .bodyToMono(PartnerDirectoryRoot.class) // Gets the DTO with the 'total' field
                .flatMap(initialResponse -> {
                    // Parse total. Use a default just in case.
                    int total = 0;
                    try {
                        total = Integer.parseInt(initialResponse.total());
                    } catch (NumberFormatException e) {
                        log.error("Could not parse partner total: {}", initialResponse.total(), e);
                        // FIX: Specify generic type for an empty list
                        return Mono.just(Collections.<RawPartner>emptyList());
                    }

                    log.info("Discovered total of {} partners.", total);
                    if (total == 0) {
                        // FIX: Specify generic type for an empty list
                        return Mono.just(Collections.<RawPartner>emptyList());
                    }

                    // Step 2: Make the real call to get all partners
                    String fullUrl = String.format(PARTNERS_URL_TEMPLATE, total);
                    log.info("Fetching all partners from: {}", fullUrl);
                    return this.webClient.get().uri(fullUrl)
                            .retrieve()
                            .bodyToMono(PartnerDirectoryRoot.class)
                            .map(PartnerDirectoryRoot::getAllPartners); // Use the new helper
                })
                .doOnError(e -> log.error("Failed to fetch partners", e))
                // FIX: Specify generic type for the empty list on error
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * --- NEW METHOD with GENERIC FIX ---
     * Dynamically fetches ALL solutions from the API.
     * 1. Fetches 1 record to get the 'total' count.
     * 2. Uses the 'total' to fetch all records.
     */
    private Mono<List<RawSolution>> fetchAllSolutions() {
        String initialUrl = String.format(SOLUTIONS_URL_TEMPLATE, 1);
        log.info("Fetching total solution count from: {}", initialUrl);

        return this.webClient.get().uri(initialUrl)
                .retrieve()
                .bodyToMono(SolutionCatalogRoot.class)
                .flatMap(initialResponse -> {
                    // Parse total. Use a default just in case.
                    int total = 0;
                    try {
                        total = Integer.parseInt(initialResponse.total());
                    } catch (NumberFormatException e) {
                        log.error("Could not parse solution total: {}", initialResponse.total(), e);
                        // FIX: Specify generic type for an empty list
                        return Mono.just(Collections.<RawSolution>emptyList());
                    }

                    log.info("Discovered total of {} solutions.", total);
                    if (total == 0) {
                        // FIX: Specify generic type for an empty list
                        return Mono.just(Collections.<RawSolution>emptyList());
                    }

                    // Step 2: Make the real call to get all solutions
                    String fullUrl = String.format(SOLUTIONS_URL_TEMPLATE, total);
                    log.info("Fetching all solutions from: {}", fullUrl);
                    return this.webClient.get().uri(fullUrl)
                            .retrieve()
                            .bodyToMono(SolutionCatalogRoot.class)
                            .map(SolutionCatalogRoot::getAllSolutions); // Use the new helper
                })
                .doOnError(e -> log.error("Failed to fetch solutions", e))
                // FIX: Specify generic type for the empty list on error
                .onErrorReturn(Collections.emptyList());
    }

    /**
     * --- Mandatory pagination (for Q-3).
     * Uses the pre-fetched list, ensuring fast response times after init.
     *
     * @param pageable The pagination information (page number, size, sort).
     * @param hasSolutions (Bonus) If true, filters for partners with 1 or more solutions.
     * @return A Page object containing the PartnerSolution DTOs.
     */
    public Page<PartnerSolution> getPartners(Pageable pageable, boolean hasSolutions) {

        // 1. Filter the entire cached list based on the 'hasSolutions' flag
        List<PartnerSolution> filteredList;
        if (hasSolutions) {
            filteredList = this.joinedPartnersCache.stream()
                    .filter(partner -> partner.solutions() != null && !partner.solutions().isEmpty())
                    .toList();
        } else {
            // Use an immutable copy for thread safety
            filteredList = List.copyOf(this.joinedPartnersCache);
        }

        // 2. Determine pagination boundaries
        int totalSize = filteredList.size();
        int start = (int) pageable.getOffset();

        // Handle edge case where requested page is beyond the data
        if (start >= totalSize) {
            return Page.empty(pageable);
        }

        int end = Math.min(start + pageable.getPageSize(), totalSize);

        // 3. Extract the sublist for the current page
        List<PartnerSolution> pageContent = filteredList.subList(start, end);

        // 4. Return the result wrapped in a Page object
        return new PageImpl<>(pageContent, pageable, totalSize);
    }


}

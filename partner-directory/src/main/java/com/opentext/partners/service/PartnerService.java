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
import org.springframework.cache.annotation.Cacheable;
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

    // Added SLF4J Logger for proper application logging
    private static final Logger log = LoggerFactory.getLogger(PartnerService.class);

    // Constants for external resource URLs
    private static final String PARTNERS_URL =
            "https://www.opentext.com/en/partners/partners-directory-overview/1716790338234.ajax?q=&start=0&max=563&sorter=Default_Sort";
    private static final String SOLUTIONS_URL =
            "https://www.opentext.com/en/partners/ApplicationMarketplace/1754971906819.ajax?q=&start=0&max=174&sorter=Name";

    private final WebClient webClient;

    // Local cache stores all joined data after initial fetch for synchronous access
    private List<PartnerSolution> joinedPartnersCache = Collections.emptyList();

    public PartnerService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("").build();
    }

    /**
     * Eagerly loads the data into the local cache on startup using the @Cacheable method.
     * This ensures synchronous controller methods have data available immediately and are fast.
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing and caching partner data on startup...");
            // Blocks once on startup to populate the local cache list from the Spring Cache.
            List<PartnerSolution> cachedData = getCachedPartners().block();
            if (cachedData != null) {
                this.joinedPartnersCache = cachedData;
                log.info("Successfully fetched and cached {} joined partner records.", cachedData.size());
            } else {
                log.warn("Partner data initialization returned null.");
            }
        } catch (Exception e) {
            // Use SLF4J logger for error logging
            log.error("CRITICAL: Failed to initialize joined partner data. Data may be unavailable.", e);
        }
    }

    /**
     * Implements the core caching mechanism. The result of fetchAndJoinDataInternal()
     * is stored in the cache named "partners" after the first call (Q-2).
     */
    @Cacheable(value = "partners")
    public Mono<List<PartnerSolution>> getCachedPartners() {
        log.debug("Cache miss. Fetching and joining partner data from external URLs...");
        return fetchAndJoinDataInternal();
    }

    /**
     * Executes the network fetch and join logic. (Question 2 Logic)
     * @return A Mono emitting the list of joined PartnerSolution objects.
     */
    private Mono<List<PartnerSolution>> fetchAndJoinDataInternal() {
        // 1. Fetch Partners Data and Solutions Data concurrently
        Mono<PartnerDirectoryRoot> partnersMono = webClient.get().uri(PARTNERS_URL)
                .retrieve().bodyToMono(PartnerDirectoryRoot.class);
        Mono<SolutionCatalogRoot> solutionsMono = webClient.get().uri(SOLUTIONS_URL)
                .retrieve().bodyToMono(SolutionCatalogRoot.class);

        // 2. Combine results and perform the join
        return Mono.zip(partnersMono, solutionsMono)
                .map(tuple -> {
                    // Extract raw partners and solutions from the API response structure
                    // Added null checks for safety
                    List<RawPartner> partners = tuple.getT1().results().assets().stream()
                            .map(asset -> asset.contentJson().partnerRoot().partner())
                            .filter(Objects::nonNull) // Ensure no null partners
                            .toList();

                    List<RawSolution> solutions = tuple.getT2().results().assets().stream()
                            .map(asset -> asset.contentJson().solutionRoot().solution())
                            .filter(Objects::nonNull) // Ensure no null solutions
                            .toList();

                    log.info("Fetched {} raw partners and {} raw solutions.", partners.size(), solutions.size());

                    // --- JOINING LOGIC: Group solutions first for O(1) lookup ---
                    Map<String, List<RawSolution>> solutionsByPartnerName = solutions.stream()
                            .filter(s -> s.partnerName() != null && !s.partnerName().isBlank())
                            .collect(Collectors.groupingBy(
                                    // Normalize key for robust joining (case-insensitive and trim)
                                    solution -> solution.partnerName().trim().toLowerCase()
                            ));

                    // 3. Join partners with their corresponding solutions (Left Join)
                    return partners.stream()
                            .map(partner -> {
                                // Normalize the partner name for lookup
                                String partnerKey = (partner.name() != null) ? partner.name().trim().toLowerCase() : "";

                                // Find matching solutions using the pre-built map
                                List<RawSolution> partnerRawSolutions = solutionsByPartnerName.getOrDefault(
                                        partnerKey,
                                        Collections.emptyList() // Use emptyList for safety
                                );

                                // Map raw solutions to the final DTO format
                                List<Solution> partnerSolutions = partnerRawSolutions.stream()
                                        .map(rawSolution -> new Solution(
                                                rawSolution.displayName(),
                                                rawSolution.shortDescription()
                                        ))
                                        .toList();

                                // Create the final joined object
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
                });
    }

    /**
     * Exposed method for the controller to get all cached data (for Q-2).
     * Returns the locally stored, pre-fetched list.
     */
    public List<PartnerSolution> getJoinedPartners() {
        return joinedPartnersCache;
    }

    /**
     * Retrieves partners with optional filtering and mandatory pagination (for Q-3).
     * Uses the pre-fetched list, ensuring fast response times after init.
     *
     * @param pageable     The pagination information (page number, size, sort).
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
            filteredList = this.joinedPartnersCache;
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

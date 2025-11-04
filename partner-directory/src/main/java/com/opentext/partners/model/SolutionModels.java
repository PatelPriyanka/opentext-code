package com.opentext.partners.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Models for deserializing the JSON data from the partner-solutions-catalog API.
 * Uses @JsonIgnoreProperties and @JsonProperty as requested for clean mapping.
 */
public class SolutionModels {

    /**
     * --- UPDATED ---
     * Maps the root of the partner-solutions-catalog JSON.
     * Added @JsonProperty("total") to capture the total count.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolutionCatalogRoot(
            @JsonProperty("total") String total, // <-- ADDED THIS LINE
            @JsonProperty("results") SolutionResults results
    ) {
        /**
         * --- NEW HELPER METHOD ---
         * A small helper to safely extract the final list of solutions
         * from the nested JSON structure.
         */
        public List<RawSolution> getAllSolutions() {
            if (results == null || results.assets == null) {
                return List.of();
            }
            return results.assets.stream()
                    .filter(Objects::nonNull)
                    .map(SolutionAsset::contentJson)
                    .filter(Objects::nonNull)
                    .map(SolutionContentJson::solutionRoot)
                    .filter(Objects::nonNull)
                    .map(SolutionRoot::solution)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    /** Maps the 'results' object */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolutionResults(@JsonProperty("assets") List<SolutionAsset> assets) {}

    /** Maps the objects inside the 'assets' array */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolutionAsset(
            @JsonProperty("contentJson") SolutionContentJson contentJson,
            @JsonProperty("metadata") SolutionMetadata metadata
    ) {}

    /** Maps the 'contentJson' object */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolutionContentJson(@JsonProperty("Solutions") SolutionRoot solutionRoot) {}

    /** Maps the 'Solutions' object */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolutionRoot(@JsonProperty("Solution") RawSolution solution) {}

    /** Maps the 'metadata' object, specifically for the product family field */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolutionMetadata(
            @JsonProperty("TeamSite/Metadata/SolutionProduct") String solutionProduct
    ) {}

    /**
     * Maps the core 'Solution' object with fields required for the DTO and joining.
     * We map SolutionPartnerName for the join.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawSolution(
            @JsonProperty("solutionpartnername") String partnerName, // Crucial for joining
            @JsonProperty("solutiondisplayname") String displayName,
            @JsonProperty("urlsolutionshortdescription") String shortDescription
    ) {
        /**
         * --- NEW CONSTRUCTOR ---
         * This constructor automatically runs to clean HTML tags from the description
         * field, ensuring the final JSON is clean.
         */
        public RawSolution {
            if (shortDescription != null) {
                shortDescription = shortDescription.replaceAll("<[^>]*>", "").trim();
            }
        }
    }
}


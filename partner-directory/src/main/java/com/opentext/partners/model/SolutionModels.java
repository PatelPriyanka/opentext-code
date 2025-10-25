package com.opentext.partners.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Models for deserializing the JSON data from the partner-solutions-catalog API.
 * Uses @JsonIgnoreProperties and @JsonProperty as requested for clean mapping.
 */
public class SolutionModels {

    /** Maps the root of the partner-solutions-catalog JSON */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SolutionCatalogRoot(@JsonProperty("results") SolutionResults results) {}

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
    ) {}
}

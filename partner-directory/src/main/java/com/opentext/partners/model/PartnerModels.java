package com.opentext.partners.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Models for deserializing the JSON data from the partner-directory API.
 * Uses @JsonIgnoreProperties and @JsonProperty as requested for clean mapping.
 */
public class PartnerModels {

    /**
     * --- UPDATED ---
     * Maps the root of the partner-directory JSON.
     * Added @JsonProperty("total") to capture the total count.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartnerDirectoryRoot(
            @JsonProperty("total") String total, // <-- ADDED THIS LINE
            @JsonProperty("results") PartnerResults results
    ) {
        /**
         * --- NEW HELPER METHOD ---
         * A small helper to safely extract the final list of partners
         * from the nested JSON structure.
         */
        public List<RawPartner> getAllPartners() {
            if (results == null || results.assets == null) {
                return List.of();
            }
            return results.assets.stream()
                    .filter(Objects::nonNull)
                    .map(PartnerAsset::contentJson)
                    .filter(Objects::nonNull)
                    .map(PartnerContentJson::partnerRoot)
                    .filter(Objects::nonNull)
                    .map(PartnerRoot::partner)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    /** Maps the 'results' object */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartnerResults(@JsonProperty("assets") List<PartnerAsset> assets) {}

    /** Maps the objects inside the 'assets' array */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartnerAsset(@JsonProperty("contentJson") PartnerContentJson contentJson) {}

    /** Maps the 'contentJson' object */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartnerContentJson(@JsonProperty("Partners") PartnerRoot partnerRoot) {}

    /** Maps the 'Partners' object */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartnerRoot(@JsonProperty("Partner") RawPartner partner) {}

    /**
     * Maps the core 'Partner' object with fields required for the DTO and joining.
     * We map Partner.Name and Partner.Id for the join and DTO, and get the descriptions.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawPartner(
            @JsonProperty("Id") String id,
            @JsonProperty("Name") String name,
            @JsonProperty("PartnerLevel__c") String partnerLevel,
            @JsonProperty("PartnerType__c") String partnerType,
            @JsonProperty("Short_Description") String shortDescription,
            @JsonProperty("PartnerCompanyOverview__c") String companyOverview // Fallback description
    ) {
        /**
         * --- NEW CONSTRUCTOR ---
         * This constructor automatically runs to clean HTML tags from the description
         * fields, ensuring the final JSON is clean.
         */
        public RawPartner {
            if (shortDescription != null) {
                shortDescription = shortDescription.replaceAll("<[^>]*>", "").trim();
            }
            if (companyOverview != null) {
                companyOverview = companyOverview.replaceAll("<[^>]*>", "").trim();
            }
        }
    }
}


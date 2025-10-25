package com.opentext.partners.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Models for deserializing the JSON data from the partner-directory API.
 * Uses @JsonIgnoreProperties and @JsonProperty as requested for clean mapping.
 */
public class PartnerModels {

    /** Maps the root of the partner-directory JSON */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartnerDirectoryRoot(@JsonProperty("results") PartnerResults results) {}

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
    ) {}
}

package com.opentext.partners.model;

import java.util.List;

/**
 * DTOs for the final joined output JSON structure.
 * This structure presents each partner with a nested list of their solutions.
 */
public class JoinedPartnerDto {

    /** The structure for a single Partner with its associated Solutions. (Q-2 and Q-3) */
    public record PartnerSolution(
            String partnerName,
            String partnerId,
            String partnerLevel,
            String partnerType,
            String shortDescription,
            String companyOverview,
            List<Solution> solutions // Nested list of solutions
    ) {}

    /** The structure for a single Solution. */
    public record Solution(
            String displayName,
            String shortDescription
    ) {}
}

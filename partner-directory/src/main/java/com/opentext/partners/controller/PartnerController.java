package com.opentext.partners.controller;

import com.opentext.partners.model.JoinedPartnerDto.PartnerSolution;
import com.opentext.partners.service.PartnerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PartnerController {

    // The PartnerService is injected via the constructor (constructor injection)
    // The @Autowired annotation on the field was redundant and has been removed.
    private final PartnerService partnerService;

    // @Autowired is implicit on public constructors in recent Spring versions,
    // but explicit constructor injection is clear.
    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    /**
     * Solves Question 3: Provides a paginated view of partners with an optional filter.
     *
     * @param page         The page number to retrieve (default 0).
     * @param size         The number of items per page (default 10).
     * @param hasSolutions (Bonus) If true, only returns partners with 1 or more solutions.
     * @return A paginated ResponseEntity of PartnerSolution objects.
     */
    @GetMapping("/partners")
    public ResponseEntity<Page<PartnerSolution>> getPaginatedPartners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean hasSolutions) {

        Pageable pageable = PageRequest.of(page, size);
        Page<PartnerSolution> partnerPage = partnerService.getPartners(pageable, hasSolutions);
        return ResponseEntity.ok(partnerPage);
    }

    /**
     * Solves Question 2: Outputs the complete joined JSON data.
     *
     * @return A ResponseEntity containing the list of all joined PartnerSolution objects.
     */
    @GetMapping("/partners/joined-json")
    public ResponseEntity<List<PartnerSolution>> getJoinedJson() {
        List<PartnerSolution> joinedData = partnerService.getJoinedPartners();
        return ResponseEntity.ok(joinedData);
    }
}

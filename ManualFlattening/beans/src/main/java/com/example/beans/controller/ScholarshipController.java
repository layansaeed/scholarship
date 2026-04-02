package com.example.beans.controller;

import com.example.beans.constant.IntegrationType;
import com.example.beans.service.pattern.IntegrationStrategyFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scholarship")
public class ScholarshipController {

    private final IntegrationStrategyFactory factory;

    public ScholarshipController(IntegrationStrategyFactory factory) {
        this.factory = factory;
    }

    /**
     * Inserts scholarship + stages for a specific NIN.
     * POST /api/scholarship/insert?nin=1000000001
     */
    @PostMapping("/insert")
    public ResponseEntity<?> insertSpecific(@RequestParam("nin") Long nin) {
        try {
            factory.getStrategy(IntegrationType.SCHOLARSHIP).insertForNin(nin);
            //integrationService.insertForNin(IntegrationType.SCHOLARSHIP, nin);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Inserts scholarship + stages for ALL NINs in Beneficiary table.
     * POST /api/scholarship/insert/all
     */
    @PostMapping("/insert/all")
    public ResponseEntity<?> insertAll() {
        try {
            factory.getStrategy(IntegrationType.SCHOLARSHIP).insertForAllNins();
            //integrationService.insertForAllNins(IntegrationType.SCHOLARSHIP);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
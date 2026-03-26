package com.example.beans.controller;

import com.example.beans.service.ScholarshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scholarship")
public class ScholarshipController {

    private final ScholarshipService scholarshipService;

    public ScholarshipController(ScholarshipService scholarshipService) {
        this.scholarshipService = scholarshipService;
    }

    /**
     * Calls Scholarship Mockoon API for a specific NIN (no DB).
     * POST /api/scholarship/mockoon?nin=1000000001
     */
    @PostMapping("/mockoon")
    public ResponseEntity<?> callMockoon(@RequestParam("nin") Long nin) {
        try {
            return ResponseEntity.ok(scholarshipService.callScholarshipApi(nin));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Calls Scholarship Mockoon API using the first NIN from DB.
     * GET /api/scholarship/mockoon/from-db
     */
    @GetMapping("/mockoon/from-db")
    public ResponseEntity<?> callMockoonFromDb() {
        try {
            Long nin = scholarshipService.getFirstNinFromDb();
            return ResponseEntity.ok(scholarshipService.callScholarshipApi(nin));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Inserts scholarship + stages for the first NIN from DB.
     * POST /api/scholarship/insert/one
     */
    @PostMapping("/insert/one")
    public ResponseEntity<?> insertOne() {
        try {
            return ResponseEntity.ok(scholarshipService.insertScholarshipForFirstNin());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Inserts scholarship + stages for a specific NIN.
     * POST /api/scholarship/insert?nin=1000000001
     */
    @PostMapping("/insert")
    public ResponseEntity<?> insertSpecific(@RequestParam("nin") Long nin) {
        try {
            return ResponseEntity.ok(scholarshipService.insertScholarshipForNin(nin));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Inserts scholarship + stages for ALL NINs in Beneficiary table.
     * POST /api/scholarship/insert/all
     */
    @PostMapping("/insert/all")
    public ResponseEntity<?> insertAll() {
        try {
            return ResponseEntity.ok(scholarshipService.insertScholarshipForAllNins());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
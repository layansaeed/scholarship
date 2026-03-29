package com.example.beans.controller;

import com.example.beans.service.DisabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/disability")
public class DisabilityController {

    private final DisabilityService disabilityService;

    public DisabilityController(DisabilityService disabilityService) {
        this.disabilityService = disabilityService;
    }
    /**
     * Calls Mockoon using the NIN you send manually.
     * Returns API response only (no DB insert).
     *
     * POST /api/disability/mockoon?nin=1127613683
     */
    @PostMapping("/mockoon")
    public Map<String, Object> callMockoon(@RequestParam("nin") Long nin) {
        return disabilityService.callMockoon(nin);
    }

    /**
     * Calls Mockoon using the first NIN found in DB.
     * Returns API response only (no DB insert).
     *
     * GET /api/disability/mockoon/from-db
     */
    @GetMapping("/mockoon/from-db")
    public Map<String, Object> callMockoonFromDb() {
        return disabilityService.callMockoonWithOneNinFromDb();
    }

    /**
     * Inserts disability data for the first NIN in DB.
     *
     * POST /api/disability/insert/one
     */
    @PostMapping("/insert/one")
    public ResponseEntity<?> insertOne() {
        try {
            int inserted = disabilityService.insertForOneNin();
            return ResponseEntity.ok(inserted);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Inserts disability data for the specific NIN you pass.
     *
     * POST /api/disability/insert?nin=1127613683
     */
    @PostMapping("/insert")
    public ResponseEntity<?> insertSpecific(@RequestParam("nin") Long nin) {
        try {
            int inserted = disabilityService.insertForSpecificNin(nin);
            return ResponseEntity.ok(inserted);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Inserts disability data for ALL NINs in Beneficiary table.
     *
     * POST /api/disability/insert/all
     */
    @PostMapping("/insert/all")
    public ResponseEntity<?> insertAll() {
        try {
            int inserted = disabilityService.insertForAllNins();
            return ResponseEntity.ok(inserted);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }


}
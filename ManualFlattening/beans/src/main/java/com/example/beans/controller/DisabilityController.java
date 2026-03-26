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
     *
     *Calls Mockoon using the NIN you send manually.
     * It only returns the API response.
     * No insert into DB.
     * @param nin
     * @return
     */
    @PostMapping("/mockoon")
    public Map<String, Object> callMockoon(@RequestParam("nin") Long nin) {
        return disabilityService.callMockoon(nin);
    }

    /**
     * Calls Mockoon using the NIN you send manually.
     * It only returns the API response.
     * No insert into DB.
     * @return
     */
    @GetMapping("/mockoon/from-db")
    public Map<String, Object> callMockoonFromDb() {
        return disabilityService.callMockoonWithOneNinFromDb();
    }

    /**
     * Reads one NIN from your Beneficiary table, calls Mockoon, maps the response, then inserts it into DB.
     * Insert happens for one NIN from DB.
     * @return
     */
    @PostMapping("/insert/one")
    public ResponseEntity<?> insertOne() {
        try {
            return ResponseEntity.ok(disabilityService.insertForOneNin());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Uses the NIN you send manually, calls Mockoon, maps the response, then inserts it into DB.
     * Insert happens for the specific NIN you passed.
     * @param nin
     * @return
     */
    @PostMapping("/insert")
    public ResponseEntity<?> insertSpecific(@RequestParam("nin") Long nin) {
        try {
            return ResponseEntity.ok(disabilityService.insertForSpecificNin(nin));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Reads all NINs from your Beneficiary table, calls Mockoon for each one, maps all responses, then inserts them into DB.
     * Insert happens for all NINs in DB.
     * @return
     */
    @PostMapping("/insert/all")
    public ResponseEntity<?> insertAll() {
        try {
            return ResponseEntity.ok(disabilityService.insertForAllNins());
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /**
     * Easy way to remember
     *
     * mockoon = test API only
     *
     * insert = API + mapping + DB insert
     *
     * from-db = take NIN from database
     *
     * ?nin=... = you send the NIN yourself
     *
     * all = run for every NIN in Beneficiary
     */
}
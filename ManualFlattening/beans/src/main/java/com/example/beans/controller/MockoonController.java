//package com.example.beans.controller;
//
//import com.example.beans.model.BeneficiaryEntity;
//import com.example.beans.repository.BeneficiaryJpaRepository;
//import com.example.beans.service.MockoonDemoService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * REST controller for testing the integration flow:
// * - Read NIN(s) from DB (via JPA)
// * - Call Mockoon API
// * - Insert results into DB using Generic SimpleJdbcInsert
// */
//@RestController
//@RequestMapping("/api")
//public class MockoonController {
//
//    private final MockoonDemoService mockoonService;
//    private final BeneficiaryJpaRepository beneficiaryJpaRepository;
//
//    public MockoonController(MockoonDemoService mockoonService, BeneficiaryJpaRepository beneficiaryJpaRepository) {
//        this.mockoonService = mockoonService;
//        this.beneficiaryJpaRepository = beneficiaryJpaRepository;
//    }
//
//    /**
//     * Calls Mockoon directly using a NIN provided by the request.
//     *
//     * Example:
//     * POST http://localhost:8080/api/mockoon?nin=1127613683
//     *
//     * @param nin beneficiary NIN (sent as request param)
//     * @return Mockoon JSON response as a Map
//     */
//    @PostMapping("/mockoon")
//    public Map<String, Object> callMockoon(@RequestParam("nin") Long nin) {
//        return mockoonService.callMockoon(nin);
//    }
//
//    /**
//     * Returns all NIN values from the Beneficiary table using JPA.
//     * This endpoint is only for testing/verifying DB connectivity.
//     *
//     * Example:
//     * GET http://localhost:8080/api/beneficiary/nins/jpa
//     *
//     * @return list of NIN values
//     */
//    @GetMapping("/beneficiary/nins/jpa")
//    public List<Long> getNinsJpa() {
//
//        List<BeneficiaryEntity> all =
//                beneficiaryJpaRepository.findAll(); //Sort.by("nin")
//
//        return all.stream()
//                .map(BeneficiaryEntity::getNin)
//                .toList();
//    }
//
//    /**
//     * Returns all NIN values from the Beneficiary table using JPA.
//     * This endpoint is only for testing/verifying DB connectivity.
//     *
//     * Example:
//     * GET http://localhost:8080/api/beneficiary/nins/jpa
//     *
//     * @return list of NIN values
//     */
//    @GetMapping("/test")
//    public Long getOneNin() {
//        return mockoonService.getOneNinFromDb();
//    }
//
//    /**
//     * Reads one NIN from DB and calls Mockoon with it.
//     *
//     * Example:
//     * GET http://localhost:8080/api/mockoon/from-db
//     *
//     * @return Mockoon response as map
//     */
//    @GetMapping("/mockoon/from-db")
//    public Map<String, Object> callMockoonFromDb() {
//        return mockoonService.callMockoonWithOneNinFromDb();
//    }
//
//    /**
//     * Full pipeline (all NINs):
//     * - entityName comes from XML (entities-config.xml)
//     * - for each NIN in Beneficiary table:
//     *   call Mockoon -> map response -> insert into DB table defined by entityName
//     *
//     * Example:
//     * POST http://localhost:8080/api/mockoon/DisabilityAssessment/insert/all
//     *
//     * @param entityName must exist in XML <entity name="...">
//     * @return inserted rows count (or error message)
//     */
//    @PostMapping("/mockoon/{entityName}/insert/all")
//        public ResponseEntity<?> fetchAndInsertAll(@PathVariable String entityName) {
//            try {
//                int inserted = mockoonService.fetchFromMockoonAndInsertDisabilityForAllNins(entityName);
//                return ResponseEntity.ok(inserted);
//            }catch (RuntimeException ex) {
//                return ResponseEntity.badRequest().body(ex.getMessage());
//            }
//    }
//
//    //----------------------
//    /**
//     * Insert Scholarship only using one NIN from DB.
//     */
//    @PostMapping("/mockoon/scholarship/insert/one")
//    public ResponseEntity<?> insertScholarshipForOneNin() {
//        try {
//            int inserted = mockoonService.fetchFromMockoonAndInsertScholarshipForOneNin();
//            return ResponseEntity.ok(inserted);
//        } catch (RuntimeException ex) {
//            return ResponseEntity.badRequest().body(ex.getMessage());
//        }
//    }
//
//    /**
//     * Insert Scholarship only for all NINs from DB.
//     */
//    @PostMapping("/mockoon/scholarship/insert/all")
//    public ResponseEntity<?> insertScholarshipForAllNins() {
//        try {
//            int inserted = mockoonService.fetchFromMockoonAndInsertScholarshipForAllNins();
//            return ResponseEntity.ok(inserted);
//        } catch (RuntimeException ex) {
//            return ResponseEntity.badRequest().body(ex.getMessage());
//        }
//    }
//
//
//}
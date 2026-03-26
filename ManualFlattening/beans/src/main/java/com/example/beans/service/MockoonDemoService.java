//package com.example.beans.service;
//
//import com.example.beans.model.BeneficiaryEntity;
//import com.example.beans.model.EntityDefinition;
//import com.example.beans.repository.BeneficiaryJpaRepository;
//import com.example.beans.repository.GenericEntityRepository;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class MockoonDemoService {
//
//    private final RestTemplate restTemplate;
//    private final BeneficiaryJpaRepository beneficiaryJpaRepository;
//    private final EntityDefinitionRegistry registry;
//    private final GenericEntityRepository genericRepo;
//
//    /** Mockoon endpoint URL (loaded from application.properties). */
//    private final String disabilityUrl;
//
//    public MockoonDemoService(
//            RestTemplate restTemplate,
//            BeneficiaryJpaRepository beneficiaryJpaRepository,
//            EntityDefinitionRegistry registry,
//            GenericEntityRepository genericRepo,
//            @Value("${hrsd.disability_assessment.url}") String disabilityUrl
//    ) {
//        this.restTemplate = restTemplate;
//        this.beneficiaryJpaRepository = beneficiaryJpaRepository;
//        this.registry = registry;
//        this.genericRepo = genericRepo;
//        this.disabilityUrl = disabilityUrl;
//    }
//
//    /**
//     * Calls Mockoon with body {"nin": <value>} and returns the JSON response as a Map.
//     *
//     * @param nin beneficiary NIN
//     * @return response body as map (JSON object)
//     */
//    public Map<String, Object> callMockoon(Long nin) {
//
//        Map<String, Object> body = new LinkedHashMap<>();
//        body.put("nin", nin);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
//
//        ResponseEntity<Map> response =
//                restTemplate.exchange(disabilityUrl, HttpMethod.POST, request, Map.class);
//
//        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
//            throw new RuntimeException("Mockoon failed. status=" + response.getStatusCode());
//        }
//
//        return response.getBody();
//    }
//
//    /**
//     * Reads the smallest NIN from Beneficiary table (sorted ascending).
//     *
//     * @return one NIN value
//     */
//    public Long getOneNinFromDb() {
//        BeneficiaryEntity b = beneficiaryJpaRepository.findFirstByOrderByNinAsc();
//        if (b == null) {
//            throw new RuntimeException("No NIN found in Beneficiary table");
//        }
//        return b.getNin();
//    }
//
//    /**
//     * Convenience method:
//     * - reads one NIN from DB
//     * - calls Mockoon with it
//     *
//     * @return Mockoon response as map
//     */
//    public Map<String, Object> callMockoonWithOneNinFromDb() {
//        Long nin = getOneNinFromDb();
//        return callMockoon(nin);
//    }
//
//    /**
//     * Full workflow:
//     * - Resolve entity definition by name from XML registry
//     * - Load all NINs from Beneficiary table
//     * - For each NIN: call Mockoon and convert response into a row map (matching XML field names)
//     * - Insert all rows using {@link GenericEntityRepository} (SimpleJdbcInsert)
//     *
//     * @param entityName entity name as exists in XML (e.g., "DisabilityAssessment")
//     * @return number of inserted rows
//     */
//    public int fetchFromMockoonAndInsertDisabilityForAllNins(String entityName) {
//
//        EntityDefinition def = getEntityDefinitionOrThrow(entityName);
//
//        List<Long> nins = getAllNinsSorted();
//        if (nins.isEmpty()) {
//            throw new RuntimeException("No NINs found in Beneficiary table");
//        }
//
//        List<Map<String, Object>> rows = buildRowsFromMockoon(nins);
//
//        EntityDefinition copy = buildEntityCopyWithRows(def, rows);
//
//        return genericRepo.insertAllRows(copy);
//    }
//
//    /**
//     * Loads entity definition from registry or throws a clear error message.
//     */
//    private EntityDefinition getEntityDefinitionOrThrow(String entityName) {
//        try {
//            return registry.get(entityName);
//        } catch (RuntimeException e) {
//            throw new RuntimeException("Entity '" + entityName + "' not found in XML (entities-config.xml)");
//        }
//    }
//
//    /**
//     * Reads all NIN values from Beneficiary table sorted ascending.
//     */
//    private List<Long> getAllNinsSorted() {
//        return beneficiaryJpaRepository.findAll(Sort.by("nin"))
//                .stream()
//                .map(BeneficiaryEntity::getNin)
//                .toList();
//    }
//
//    /**
//     * Calls Mockoon for each NIN and converts each response into a row map
//     * with keys matching XML field names.
//     */
//    private List<Map<String, Object>> buildRowsFromMockoon(List<Long> nins) {
//        List<Map<String, Object>> rows = new ArrayList<>();
//
//        for (Long nin : nins) {
//            Map<String, Object> result = callMockoon(nin);
//            rows.add(mapMockoonResultToDisabilityRow(result));
//        }
//
//        return rows;
//    }
//
//    /**
//     * Converts one Mockoon response map into one DB insert row
//     * using XML field names as keys.
//     */
//    private Map<String, Object> mapMockoonResultToDisabilityRow(Map<String, Object> result) {
//        Map<String, Object> row = new LinkedHashMap<>();
//        row.put("nationalId", result.get("nationalId"));
//        row.put("firstName", result.get("firstName"));
//        row.put("dob", result.get("dob"));
//        row.put("cityCode", result.get("cityCode"));
//        row.put("mobileNumber", result.get("mobileNumber"));
//        row.put("disabilityId", result.get("disabilityId"));
//        row.put("disabilityType", result.get("disabilityType"));
//        row.put("category", result.get("category"));
//        row.put("iq", result.get("iq"));
//        row.put("disabilityDate", result.get("disabilityDate"));
//        row.put("expiredDate", result.get("expiredDate"));
//        row.put("disabilityDescriptionBeneficiary", result.get("disabilityDescriptionBeneficiary"));
//        row.put("failure", result.getOrDefault("failure", "false"));
//        row.put("message", result.getOrDefault("message", null));
//        return row;
//    }
//
//    /**
//     * Creates a safe copy of the XML entity definition and sets the provided rows on it.
//     * Why copy?
//     * - To avoid mutating the shared definition stored in the registry (important for multi-requests).
//     */
//    private EntityDefinition buildEntityCopyWithRows(EntityDefinition def, List<Map<String, Object>> rows) {
//        EntityDefinition copy = new EntityDefinition();
//        copy.setEntityName(def.getEntityName());
//        copy.setTableName(def.getTableName());
//        copy.setSchema(def.getSchema());
//        copy.setFieldMapping(def.getFieldMapping());
//        copy.setRows(rows);
//        return copy;
//    }
//}
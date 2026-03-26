package com.example.beans.service;

import com.example.beans.model.BeneficiaryEntity;
import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.repository.GenericEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScholarshipService {

    private final RestTemplate restTemplate;
    private final BeneficiaryJpaRepository beneficiaryRepo;
    private final EntityDefinitionRegistry registry;
    private final GenericEntityRepository genericRepo;

    private final String scholarshipUrl;

    public ScholarshipService(
            RestTemplate restTemplate,
            BeneficiaryJpaRepository beneficiaryRepo,
            EntityDefinitionRegistry registry,
            GenericEntityRepository genericRepo,
            @Value("${hrsd.scholarship.url}") String scholarshipUrl
    ) {
        this.restTemplate = restTemplate;
        this.beneficiaryRepo = beneficiaryRepo;
        this.registry = registry;
        this.genericRepo = genericRepo;
        this.scholarshipUrl = scholarshipUrl;
    }

    /**
     * Calls Mockoon Scholarship API using NIN in request body (POST JSON).
     *
     * @param nin beneficiary national id
     * @return full JSON response as Map (contains scholarshipList and nested stageInformationList)
     */
    public Map<String, Object> callScholarshipApi(Long nin) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nin", nin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(scholarshipUrl, HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Scholarship Mockoon failed. status=" + response.getStatusCode());
        }

        log.debug("Scholarship response for NIN {} = {}", nin, response.getBody());
        return response.getBody();
    }

    /**
     * Gets one NIN from Beneficiary table (smallest NIN).
     * Useful for quick testing.
     *
     * @return one existing NIN
     */
    public Long getFirstNinFromDb() {
        BeneficiaryEntity b = beneficiaryRepo.findFirstByOrderByNinAsc();
        if (b == null) {
            throw new RuntimeException("No NIN found in Beneficiary table");
        }
        return b.getNin();
    }

    /**
     * Main use-case:
     * 1) Take a NIN
     * 2) Call Mockoon Scholarship API
     * 3) Insert into two tables:
     * - Scholarship (parent)
     * - ScholarshipStageInformation (child rows)
     * <p>
     * The stage rows are enriched with:
     * - studentIdentificationNumber
     * - scholarshipRowId (parent row_id generated in Scholarship table)
     *
     * @param nin beneficiary NIN used in request
     * @return insertion summary (scholarshipInserted, stageInserted, totalInserted)
     */
    public Map<String, Object> insertScholarshipForNin(Long nin) {


        EntityDefinition scholarshipDef = requireEntity("Scholarship");
        EntityDefinition stageDef = requireEntity("ScholarshipStageInformation");

        // response from mockoon as json
        Map<String, Object> response = callScholarshipApi(nin);

        //Split JSON into 2 sets of rows -> 2 tables
        List<Map<String, Object>> scholarships = extractScholarshipList(response);

        List<Map<String, Object>> scholarshipRows = mapScholarshipsToRows(scholarships);

        EntityDefinition scholarshipCopy = buildEntityCopyWithRows(scholarshipDef, scholarshipRows);
        List<Long> scholarshipRowIds = genericRepo.insertAllRowsReturnIds(scholarshipCopy);

        List<Map<String, Object>> stageRows = mapAllStagesToRows(scholarships, scholarshipRowIds);

        EntityDefinition stageCopy = buildEntityCopyWithRows(stageDef, stageRows);
        int stageInserted = genericRepo.insertAllRows(stageCopy);

        return buildInsertSummary(scholarshipRowIds.size(), stageInserted);
    }

    /**
     * Convenience method for testing:
     * uses first NIN in Beneficiary table.
     *
     * @return insertion summary
     */
    public Map<String, Object> insertScholarshipForFirstNin() {
        return insertScholarshipForNin(getFirstNinFromDb());
    }

    /**
     * Loads all NINs from Beneficiary table (sorted) and runs the same logic for each one.
     *
     * @return insertion summary for all NINs
     */
    public Map<String, Object> insertScholarshipForAllNins() {
        List<Long> nins = getAllNinsSorted();
        if (nins.isEmpty()) {
            throw new RuntimeException("No NINs found in Beneficiary table");
        }

        int totalScholarshipRows = 0;
        int totalStageRows = 0;

        for (Long nin : nins) {
            Map<String, Object> singleResult = insertScholarshipForNin(nin);
            totalScholarshipRows += (int) singleResult.get("scholarshipInserted");
            totalStageRows += (int) singleResult.get("stageInserted");
        }

        return buildInsertSummary(totalScholarshipRows, totalStageRows);
    }

    // -------------------- helpers --------------------

    /**
     * Gets an EntityDefinition from registry, or throws a readable error.
     */
    private EntityDefinition requireEntity(String entityName) {
        try {
            return registry.get(entityName);
        } catch (RuntimeException e) {
            throw new RuntimeException("Entity '" + entityName + "' not found in XML");
        }
    }

    /**
     * Reads all NINs from Beneficiary table sorted ascending.
     */
    private List<Long> getAllNinsSorted() {
        return beneficiaryRepo.findAll(Sort.by("nin"))
                .stream()
                .map(BeneficiaryEntity::getNin)
                .toList();
    }

    /**
     * Extracts scholarshipList from response and ensures correct structure.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractScholarshipList(Map<String, Object> response) {
        Object raw = response.get("scholarshipList");
        if (!(raw instanceof List<?> list)) {
            throw new RuntimeException("Response does not contain a valid 'scholarshipList'");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) {
                throw new RuntimeException("One item inside scholarshipList is not a valid JSON object");
            }
            result.add((Map<String, Object>) m);
        }
        return result;
    }

    /**
     *
     * Converts each scholarship object into a flat row map that matches XML field names and just has parent field without child
     * for Scholarship table (parent) -> removing nested stage lis (stageInformationList )
     */

    private List<Map<String, Object>> mapScholarshipsToRows(List<Map<String, Object>> scholarships) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> scholarship : scholarships) {
            Map<String, Object> row = new LinkedHashMap<>(scholarship); // copy JSON as-is
            row.remove("stageInformationList");                         // remove nested child list
            rows.add(row);
        }

        return rows;
    }

    /**
     * iterate over all scholarships and attach the correct parent IDs.
     * Builds all stage rows for all scholarships, and injects:
     * - studentIdentificationNumber
     * - scholarshipRowId (parent row_id)
     */
    private List<Map<String, Object>> mapAllStagesToRows(List<Map<String, Object>> scholarships,
                                                         List<Long> scholarshipRowIds) {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < scholarships.size(); i++) {
            Map<String, Object> scholarship = scholarships.get(i);

            Long studentId = ((Number) scholarship.get("studentIdentificationNumber")).longValue();
            Long scholarshipRowId = scholarshipRowIds.get(i);
            rows.addAll(mapStagesToRows(scholarship, studentId, scholarshipRowId));
        }
        return rows;
    }

    /**
     * Converts nested stageInformationList into rows for Stage table.
     * If stageInformationList is missing/null, returns empty list.
     * This method about flattening one parent’s nested children.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapStagesToRows(Map<String, Object> scholarship,
                                                      Long studentId,
                                                      Long scholarshipRowId) {

        Object rawStageList = scholarship.get("stageInformationList");
        if (rawStageList == null) {
            return List.of();
        }
        if (!(rawStageList instanceof List<?> stageList)) {
            throw new RuntimeException("'stageInformationList' is not a valid JSON array");
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        for (Object stageItem : stageList) {
            if (!(stageItem instanceof Map<?, ?> rawStage)) {
                throw new RuntimeException("One item inside stageInformationList is not a valid JSON object");
            }

            Map<String, Object> stage = (Map<String, Object>) rawStage;

            /**
             * edit stage and not create map copy-> modifying the original stage map
             * that came from the response (and still lives inside scholarship → stageInformationList).
             */
            Map<String, Object> row = new LinkedHashMap<>(stage); // copy stage JSON as-is
            row.put("studentIdentificationNumber", studentId);
            row.put("scholarshipRowId", scholarshipRowId);
            rows.add(row);
        }

        return rows;
    }

    /**
     * Copies XML metadata (table/schema/mapping) and attaches runtime rows.
     * We do this to avoid modifying the original EntityDefinition stored in registry.
     */
    private EntityDefinition buildEntityCopyWithRows(EntityDefinition def, List<Map<String, Object>> rows) {
        EntityDefinition copy = new EntityDefinition();
        copy.setEntityName(def.getEntityName());
        copy.setTableName(def.getTableName());
        copy.setSchema(def.getSchema());
        copy.setFieldMapping(def.getFieldMapping());
        copy.setRows(rows);
        return copy;
    }

    /**
     * Builds a consistent response for controller.
     */
    private Map<String, Object> buildInsertSummary(int scholarshipInserted, int stageInserted) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scholarshipInserted", scholarshipInserted);
        result.put("stageInserted", stageInserted);
        result.put("totalInserted", scholarshipInserted + stageInserted);
        return result;
    }
}
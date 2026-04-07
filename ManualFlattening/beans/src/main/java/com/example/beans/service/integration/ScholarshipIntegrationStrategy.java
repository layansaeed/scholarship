package com.example.beans.service.integration;

import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.repository.GenericEntityRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import com.example.beans.service.job.ParallelNinProcessorService;
import com.example.beans.service.pattern.IntegrationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScholarshipIntegrationStrategy implements IntegrationStrategy {

    private final RestTemplate restTemplate;
    private final BeneficiaryJpaRepository beneficiaryRepo;
    private final EntityDefinitionRegistry registry;
    private final GenericEntityRepository genericRepo;
    private final ParallelNinProcessorService parallelNinProcessorService;
    private final String scholarshipUrl;

    public ScholarshipIntegrationStrategy(
            RestTemplate restTemplate,
            BeneficiaryJpaRepository beneficiaryRepo,
            EntityDefinitionRegistry registry,
            GenericEntityRepository genericRepo,
            ParallelNinProcessorService parallelNinProcessorService,
            @Value("${hrsd.scholarship.url}") String scholarshipUrl
    ) {
        this.restTemplate = restTemplate;
        this.beneficiaryRepo = beneficiaryRepo;
        this.registry = registry;
        this.genericRepo = genericRepo;
        this.parallelNinProcessorService = parallelNinProcessorService;
        this.scholarshipUrl = scholarshipUrl;
    }

    @Override
    public BeneficiaryJpaRepository getBeneficiaryRepo() {
        return beneficiaryRepo;
    }

    @Override
    public EntityDefinitionRegistry getRegistry() {
        return registry;
    }

    @Override
    public ParallelNinProcessorService getParallelNinProcessorService() {
        return parallelNinProcessorService;
    }

    @Override
    public Object prepareForNin(Long nin) {
        log.info("Preparing scholarship data for NIN={}", nin);

        Map<String, Object> response = callApi(nin);
        List<Map<String, Object>> scholarships = extractScholarshipList(response);

        log.info("Prepared scholarship data for NIN={} with {} scholarship record(s)", nin, scholarships.size());
        return scholarships;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveBatch(List<Object> pageResults) {
        EntityDefinition scholarshipDef = requireEntity("SCHOLARSHIP");
        log.info("Entity loaded: {}", scholarshipDef.getFullTableName());

        EntityDefinition stageDef = requireEntity("ScholarshipStageInformation");
        log.info("Entity loaded: {}", stageDef.getFullTableName());

        List<Map<String, Object>> allScholarships = new ArrayList<Map<String, Object>>();

        for (Object result : pageResults) {
            allScholarships.addAll((List<Map<String, Object>>) result);
        }

        if (allScholarships.isEmpty()) {
            log.info("No scholarship rows to save in this page");
            return;
        }

        List<Map<String, Object>> scholarshipRows = mapScholarshipsToRows(allScholarships);

        List<Long> scholarshipRowIds =
                genericRepo.insertAllRowsReturnIds(scholarshipDef, scholarshipRows);

        List<Map<String, Object>> stageRows =
                mapAllStagesToRows(allScholarships, scholarshipRowIds);

        genericRepo.insertAllRows(stageDef, stageRows);

        log.info("Saved scholarship batch with {} parent row(s) and {} stage row(s)",
                scholarshipRows.size(), stageRows.size());
    }

    private Map<String, Object> callApi(Long nin) {
        log.info("Calling scholarship API for NIN {}", nin);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("nin", nin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<Map<String, Object>>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(scholarshipUrl, HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Scholarship Mockoon failed. status=" + response.getStatusCode());
        }

        log.debug("Scholarship response for NIN {} = {}", nin, response.getBody());
        return response.getBody();
    }

    private List<Map<String, Object>> extractScholarshipList(Map<String, Object> response) {
        Object raw = response.get("scholarshipList");

        List<?> list = requireList(raw, "Response does not contain a valid 'scholarshipList'");

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : list) {
            result.add(requireMap(item, "One item inside scholarshipList is not a valid JSON object"));
        }
        return result;
    }

    private List<Map<String, Object>> mapScholarshipsToRows(List<Map<String, Object>> scholarships) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> scholarship : scholarships) {
            Map<String, Object> row = new LinkedHashMap<String, Object>(scholarship);
            row.remove("stageInformationList");
            rows.add(row);
        }

        return rows;
    }

    private List<Map<String, Object>> mapAllStagesToRows(
            List<Map<String, Object>> scholarships,
            List<Long> scholarshipRowIds
    ) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < scholarships.size(); i++) {
            Map<String, Object> scholarship = scholarships.get(i);

            Long studentId = ((Number) scholarship.get("studentIdentificationNumber")).longValue();
            Long scholarshipRowId = scholarshipRowIds.get(i);

            rows.addAll(mapStagesToRows(scholarship, studentId, scholarshipRowId));
        }

        return rows;
    }

    private List<Map<String, Object>> mapStagesToRows(
            Map<String, Object> scholarship,
            Long studentId,
            Long scholarshipRowId
    ) {
        Object rawStageList = scholarship.get("stageInformationList");
        if (rawStageList == null) {
            return Collections.emptyList();
        }

        List<?> stageList = requireList(rawStageList, "'stageInformationList' is not a valid JSON array");

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        for (Object stageItem : stageList) {
            Map<String, Object> stage = requireMap(stageItem, "One item inside stageInformationList is not a valid JSON object");

            Map<String, Object> row = new LinkedHashMap<String, Object>(stage);
            row.put("studentIdentificationNumber", studentId);
            row.put("scholarshipRowId", scholarshipRowId);

            rows.add(row);
        }

        return rows;
    }

    private List<?> requireList(Object raw, String errorMessage) {
        if (!(raw instanceof List)) {
            throw new RuntimeException(errorMessage);
        }
        return (List<?>) raw;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireMap(Object raw, String errorMessage) {
        if (!(raw instanceof Map)) {
            throw new RuntimeException(errorMessage);
        }
        return (Map<String, Object>) raw;
    }
}
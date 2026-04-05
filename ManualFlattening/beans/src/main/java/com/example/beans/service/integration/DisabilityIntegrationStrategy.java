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
public class DisabilityIntegrationStrategy implements IntegrationStrategy {

    private final RestTemplate restTemplate;
    private final BeneficiaryJpaRepository beneficiaryRepo;
    private final EntityDefinitionRegistry registry;
    private final GenericEntityRepository genericRepo;
    private final ParallelNinProcessorService parallelNinProcessorService;
    private final String disabilityUrl;

    public DisabilityIntegrationStrategy(
            RestTemplate restTemplate,
            BeneficiaryJpaRepository beneficiaryRepo,
            EntityDefinitionRegistry registry,
            GenericEntityRepository genericRepo,
            ParallelNinProcessorService parallelNinProcessorService,
            @Value("${hrsd.disability_assessment.url}") String disabilityUrl
    ) {
        this.restTemplate = restTemplate;
        this.beneficiaryRepo = beneficiaryRepo;
        this.registry = registry;
        this.genericRepo = genericRepo;
        this.parallelNinProcessorService = parallelNinProcessorService;
        this.disabilityUrl = disabilityUrl;
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
        log.info("Preparing disability data for NIN={}", nin);

        EntityDefinition def = requireEntity("HRSD_DIS_ASS");
        Map<String, Object> response = callApi(nin);
        Map<String, Object> row = normalizeRowByXml(def, response);

        log.info("Prepared disability row for NIN={}", nin);
        return row;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveBatch(List<Object> pageResults) {
        EntityDefinition def = requireEntity("HRSD_DIS_ASS");

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        for (Object result : pageResults) {
            rows.add((Map<String, Object>) result);
        }

        if (rows.isEmpty()) {
            log.info("No disability rows to save in this page");
            return;
        }

        genericRepo.insertAllRows(def, rows);
        log.info("Saved disability batch with {} row(s)", rows.size());
    }

    private Map<String, Object> callApi(Long nin) {
        log.info("Calling disability API for NIN {}", nin);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("nin", nin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<Map<String, Object>>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(disabilityUrl, HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Disability Mockoon failed. status=" + response.getStatusCode());
        }

        log.info("Disability response for NIN {} = {}", nin, response.getBody());
        return response.getBody();
    }

    private Map<String, Object> normalizeRowByXml(EntityDefinition def, Map<String, Object> response) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();

        for (String javaFieldName : def.getFieldMapping().keySet()) {
            row.put(javaFieldName, response.get(javaFieldName));
        }

        return row;
    }
}
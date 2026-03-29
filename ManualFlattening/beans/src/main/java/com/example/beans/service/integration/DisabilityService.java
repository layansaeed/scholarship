package com.example.beans.service.integration;

import com.example.beans.model.BeneficiaryEntity;
import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.repository.GenericEntityRepository;
import com.example.beans.service.EntityDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DisabilityService {

    private final RestTemplate restTemplate;
    private final BeneficiaryJpaRepository beneficiaryJpaRepository;
    private final EntityDefinitionRegistry registry;
    private final GenericEntityRepository genericRepo;

    private final String disabilityUrl;

    public DisabilityService(
            RestTemplate restTemplate,
            BeneficiaryJpaRepository beneficiaryJpaRepository,
            EntityDefinitionRegistry registry,
            GenericEntityRepository genericRepo,
            @Value("${hrsd.disability_assessment.url}") String disabilityUrl
    ) {
        this.restTemplate = restTemplate;
        this.beneficiaryJpaRepository = beneficiaryJpaRepository;
        this.registry = registry;
        this.genericRepo = genericRepo;
        this.disabilityUrl = disabilityUrl;
    }

    public Map<String, Object> callMockoon(Long nin) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("nin", nin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON)); // Java 8 replacement

        HttpEntity<Map<String, Object>> request = new HttpEntity<Map<String, Object>>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(disabilityUrl, HttpMethod.POST, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Disability Mockoon failed. status=" + response.getStatusCode());
        }

        log.info("Disability response for NIN {} = {}", nin, response.getBody());
        return response.getBody();
    }

    public Long getOneNinFromDb() {
        BeneficiaryEntity b = beneficiaryJpaRepository.findFirstByOrderByNinAsc();
        if (b == null) {
            throw new RuntimeException("No NIN found in Beneficiary table");
        }
        return b.getNin();
    }

    /**
     * Calls Mockoon using the first NIN found in DB.
     */
    public Map<String, Object> callMockoonWithOneNinFromDb() {
        return callMockoon(getOneNinFromDb());
    }

    /**
     * Inserts disability data for the first NIN in DB.
     *
     * @return inserted rows count
     */
    public int insertForOneNin() {
        Long nin = getOneNinFromDb();
        return insertForSpecificNin(nin);
    }

    public int insertForSpecificNin(Long nin) {
        EntityDefinition def = requireEntity("DisabilityAssessment");

        Map<String, Object> response = callMockoon(nin);

        Map<String, Object> row = normalizeRowByXml(def, response);

        return genericRepo.insertAllRows(def, Collections.singletonList(row)); // Java 8 replacement
    }

    /**
     * Inserts disability data for ALL NINs in Beneficiary table.
     *
     * @return inserted rows count (equals number of NINs successfully processed)
     */
    public int insertForAllNins() {
        EntityDefinition def = requireEntity("DisabilityAssessment");

        List<Long> nins = getAllNinsSorted();
        if (nins.isEmpty()) {
            throw new RuntimeException("No NINs found in Beneficiary table");
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(nins.size());

        for (Long nin : nins) {
            Map<String, Object> response = callMockoon(nin);
            rows.add(normalizeRowByXml(def, response));
        }

        return genericRepo.insertAllRows(def, rows);
    }

    // -------------------- helpers --------------------

    private EntityDefinition requireEntity(String entityName) {
        try {
            return registry.get(entityName);
        } catch (RuntimeException e) {
            throw new RuntimeException("Entity '" + entityName + "' not found in XML");
        }
    }

    private List<Long> getAllNinsSorted() {
        return beneficiaryJpaRepository.findAll(Sort.by("nin"))
                .stream()
                .map(BeneficiaryEntity::getNin)
                .collect(Collectors.toList()); // Java 8 replacement for .toList()
    }

    private Map<String, Object> normalizeRowByXml(EntityDefinition def, Map<String, Object> response) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();

        for (String javaFieldName : def.getFieldMapping().keySet()) {
            row.put(javaFieldName, response.get(javaFieldName));
        }

        return row;
    }
}
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
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nin", nin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(disabilityUrl, HttpMethod.POST, request, Map.class);

//        System.out.println("Result is== ");
//        System.out.println(response.getBody());
//        System.out.println("");
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

    public Map<String, Object> callMockoonWithOneNinFromDb() {
        return callMockoon(getOneNinFromDb());
    }

    public int insertForOneNin() {
        Long nin = getOneNinFromDb();
        return insertForSpecificNin(nin);
    }

    public int insertForSpecificNin(Long nin) {
        EntityDefinition def = getEntityDefinitionOrThrow("DisabilityAssessment");

        Map<String, Object> response = callMockoon(nin);
        Map<String, Object> row = mapMockoonResultToDisabilityRow(response);

        EntityDefinition copy = buildEntityCopyWithRows(def, List.of(row));
        return genericRepo.insertAllRows(copy);
    }

    public int insertForAllNins() {
        EntityDefinition def = getEntityDefinitionOrThrow("DisabilityAssessment");

        List<Long> nins = getAllNinsSorted();
        if (nins.isEmpty()) {
            throw new RuntimeException("No NINs found in Beneficiary table");
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        for (Long nin : nins) {
            Map<String, Object> response = callMockoon(nin);
            rows.add(mapMockoonResultToDisabilityRow(response));
        }

        EntityDefinition copy = buildEntityCopyWithRows(def, rows);
        return genericRepo.insertAllRows(copy);
    }

    private EntityDefinition getEntityDefinitionOrThrow(String entityName) {
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
                .toList();
    }

    private Map<String, Object> mapMockoonResultToDisabilityRow(Map<String, Object> result) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("nationalId", result.get("nationalId"));
        row.put("firstName", result.get("firstName"));
        row.put("dob", result.get("dob"));
        row.put("cityCode", result.get("cityCode"));
        row.put("mobileNumber", result.get("mobileNumber"));
        row.put("disabilityId", result.get("disabilityId"));
        row.put("disabilityType", result.get("disabilityType"));
        row.put("category", result.get("category"));
        row.put("iq", result.get("iq"));
        row.put("disabilityDate", result.get("disabilityDate"));
        row.put("expiredDate", result.get("expiredDate"));
        row.put("disabilityDescriptionBeneficiary", result.get("disabilityDescriptionBeneficiary"));
        row.put("failure", result.getOrDefault("failure", "false"));
        row.put("message", result.getOrDefault("message", null));
        return row;
    }

    private EntityDefinition buildEntityCopyWithRows(EntityDefinition def, List<Map<String, Object>> rows) {
        EntityDefinition copy = new EntityDefinition();
        copy.setEntityName(def.getEntityName());
        copy.setTableName(def.getTableName());
        copy.setSchema(def.getSchema());
        copy.setFieldMapping(def.getFieldMapping());
        copy.setRows(rows);
        return copy;
    }
}
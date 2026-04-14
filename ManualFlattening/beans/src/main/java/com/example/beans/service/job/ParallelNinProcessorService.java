package com.example.beans.service.job;

import com.example.beans.model.BeneficiaryEntity;
import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.repository.GenericEntityRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import com.example.beans.service.integration.DynamicCallService;
import com.example.beans.service.integration.DynamicCallService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ParallelNinProcessorService {

    private final ExecutorService executorService;
    private final int chunkSize;
    private final long rangeChunkSize;

    private final BeneficiaryJpaRepository beneficiaryRepo;
    private final DynamicCallService dynamicCallService;
    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final GenericEntityRepository genericEntityRepository;

    public ParallelNinProcessorService(
            @Value("${job.db.chunk}") int chunkSize,
            @Value("${job.range.chunk}") long rangeChunkSize,
            BeneficiaryJpaRepository beneficiaryRepo,
            DynamicCallService dynamicCallService,
            EntityDefinitionRegistry entityDefinitionRegistry,
            GenericEntityRepository genericEntityRepository
    ) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.chunkSize = chunkSize;
        this.rangeChunkSize = rangeChunkSize;
        this.beneficiaryRepo = beneficiaryRepo;
        this.dynamicCallService = dynamicCallService;
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.genericEntityRepository = genericEntityRepository;
    }

    /**
     * Existing logic:
     * process one job within a specific NIN range.
     */
    public void processInParallel(String jobName, Long start, Long end) {
        validateJobName(jobName);
        validateRange(start, end);

        EntityDefinition def = entityDefinitionRegistry.get(jobName);

        log.info("Starting parallel processing for jobName={} rangeStart={} rangeEnd={}",
                jobName, start, end);

        long currentRangeStart = start;

        while (currentRangeStart <= end) {
            long currentRangeEnd = currentRangeStart + rangeChunkSize - 1;

            if (currentRangeEnd > end) {
                currentRangeEnd = end;
            }

            processOneRange(jobName, def, currentRangeStart, currentRangeEnd);

            currentRangeStart = currentRangeEnd + 1;
        }

        log.info("Completed parallel processing for jobName={} rangeStart={} rangeEnd={}",
                jobName, start, end);
    }

    /**
     * New logic:
     * process one job for all beneficiaries without range.
     */
    public void processInParallel(String jobName) {
        validateJobName(jobName);

        EntityDefinition def = entityDefinitionRegistry.get(jobName);

        log.info("Starting full parallel processing for jobName={}", jobName);

        processAllBeneficiaries(jobName, def);

        log.info("Completed full parallel processing for jobName={}", jobName);
    }

    private void validateJobName(String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new RuntimeException("Job name must not be null or blank");
        }
    }

    private void validateRange(Long start, Long end) {
        if (start == null || end == null) {
            throw new RuntimeException("NIN range start/end must not be null");
        }

        if (start > end) {
            throw new RuntimeException("NIN range start must not be greater than end");
        }
    }

    private void processOneRange(String jobName,
                                 EntityDefinition def,
                                 Long rangeStart,
                                 Long rangeEnd) {

        int pageNumber = 0;

        log.info("Processing range window {} - {} for jobName={}",
                rangeStart, rangeEnd, jobName);

        while (true) {
            Page<BeneficiaryEntity> page =
                    beneficiaryRepo.findBeneficiaries(PageRequest.of(pageNumber, chunkSize), rangeStart, rangeEnd);

            if (!page.hasContent()) {
                log.info("No more beneficiaries in range window {} - {} for jobName={}",
                        rangeStart, rangeEnd, jobName);
                break;
            }

            processBeneficiaryPage(jobName, def, page, pageNumber);

            pageNumber++;
        }
    }

    /**
     * New method for processing all beneficiaries page by page.
     */
    private void processAllBeneficiaries(String jobName, EntityDefinition def) {
        int pageNumber = 0;

        while (true) {
            Page<BeneficiaryEntity> page =
                    beneficiaryRepo.findAll(PageRequest.of(pageNumber, chunkSize));

            if (!page.hasContent()) {
                log.info("No more beneficiaries found for full jobName={}", jobName);
                break;
            }

            processBeneficiaryPage(jobName, def, page, pageNumber);

            pageNumber++;
        }
    }

    /**
     * Shared page processor used by both:
     * - range execution
     * - full execution
     */
    private void processBeneficiaryPage(String jobName,
                                        EntityDefinition def,
                                        Page<BeneficiaryEntity> page,
                                        int pageNumber) {

        log.info("Processing page {} for jobName={} with {} beneficiary record(s)",
                pageNumber, jobName, page.getNumberOfElements());

        List<CompletableFuture<Map<String, Object>>> futures =
                new ArrayList<CompletableFuture<Map<String, Object>>>();

        for (BeneficiaryEntity beneficiary : page.getContent()) {
            final Long nin = beneficiary.getNin();

            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> response = dynamicCallService.callApi(jobName, nin);
                    return normalizeRowByXml(def, response);
                } catch (Exception e) {
                    log.error("Failed processing NIN={} for jobName={}. Error={}",
                            nin, jobName, e.getMessage(), e);
                    return null;
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Map<String, Object>> pageResults = new ArrayList<Map<String, Object>>();

        for (CompletableFuture<Map<String, Object>> future : futures) {
            Map<String, Object> result = future.join();

            if (result != null) {
                pageResults.add(result);
            }
        }

        if (!pageResults.isEmpty()) {
            genericEntityRepository.insertAllRows(def, pageResults);
            log.info("Saved {} row(s) for jobName={} in page={}",
                    pageResults.size(), jobName, pageNumber);
        }
    }

    private Map<String, Object> normalizeRowByXml(EntityDefinition def, Map<String, Object> response) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();

        for (String javaFieldName : def.getFieldMapping().keySet()) {
            row.put(javaFieldName, response.get(javaFieldName));
        }

        return row;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        log.info("ParallelNinProcessorService thread pool shutdown completed");
    }
}
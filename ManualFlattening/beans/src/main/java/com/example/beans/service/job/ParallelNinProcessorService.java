package com.example.beans.service.job;

import com.example.beans.model.BeneficiaryEntity;
import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.repository.GenericEntityRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import com.example.beans.service.integration.DynamicJobApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
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
    private final DynamicJobApiService dynamicJobApiService;
    private final EntityDefinitionRegistry entityDefinitionRegistry;
    private final GenericEntityRepository genericEntityRepository;

    public ParallelNinProcessorService(
            @Value("${job.threads.count}") int threadCount,
            @Value("${job.db.chunk}") int chunkSize,
            @Value("${job.range.chunk}") long rangeChunkSize,
            BeneficiaryJpaRepository beneficiaryRepo,
            DynamicJobApiService dynamicJobApiService,
            EntityDefinitionRegistry entityDefinitionRegistry,
            GenericEntityRepository genericEntityRepository
    ) {
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.chunkSize = chunkSize;
        this.rangeChunkSize = rangeChunkSize;
        this.beneficiaryRepo = beneficiaryRepo;
        this.dynamicJobApiService = dynamicJobApiService;
        this.entityDefinitionRegistry = entityDefinitionRegistry;
        this.genericEntityRepository = genericEntityRepository;
    }

    /**
     * Processes one audit range for one job name.
     *
     * Flow:
     * 1. Validate range.
     * 2. Load XML entity definition by job name.
     * 3. Split big range into smaller windows.
     * 4. Process each window page by page.
     */
    public void processInParallel(String jobName, Long start, Long end) {
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new RuntimeException("Job name must not be null or blank");
        }

        if (start == null || end == null) {
            throw new RuntimeException("NIN range start/end must not be null");
        }

        if (start > end) {
            throw new RuntimeException("NIN range start must not be greater than end");
        }

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

            log.info("Processing page {} in range window {} - {} for jobName={} with {} beneficiary record(s)",
                    pageNumber, rangeStart, rangeEnd, jobName, page.getNumberOfElements());

            List<CompletableFuture<Map<String, Object>>> futures =
                    new ArrayList<>();

            for (BeneficiaryEntity beneficiary : page.getContent()) {
                final Long nin = beneficiary.getNin();

                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        Map<String, Object> response = dynamicJobApiService.callApi(jobName, nin);
                        return normalizeRowByXml(def, response);
                    } catch (Exception e) {
                        log.error("Failed processing NIN={} for jobName={}. Error={}",
                                nin, jobName, e.getMessage(), e);
                        return null;
                    }
                }, executorService));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<Map<String, Object>> pageResults = new ArrayList<>();

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

            pageNumber++;
        }
    }

    /**
     * Keeps only XML-defined fields and creates one normalized row map.
     *
     * Example:
     * XML fields:
     * - nationalId
     * - firstName
     * - dob
     *
     * API response may contain many keys, but this method only keeps XML fields.
     */
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
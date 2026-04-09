package com.example.beans.service.job;

import com.example.beans.model.BeneficiaryEntity;
import com.example.beans.service.pattern.IntegrationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ParallelNinProcessorService {

    private final ExecutorService executorService;
    private final int chunkSize;
    private final long rangeChunkSize;

    public ParallelNinProcessorService(
            @Value("${job.db.chunk}") int chunkSize,
            @Value("${job.range.chunk}") long rangeChunkSize
    ) {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.chunkSize = chunkSize;
        this.rangeChunkSize = rangeChunkSize;
    }

    /**
     * Processes one audit range.
     *
     * Flow:
     * 1. Split the big audit range into smaller range.
     * 2. For each small range, fetch beneficiaries page by page.
     * 3. For each page, prepare data in parallel using the thread pool.
     * 4. Save the page results in batch.
     */
    public void processInParallel(IntegrationStrategy strategy, Long start, Long end) {
        String strategyName = strategy.getClass().getSimpleName();

        if (start == null || end == null) {
            throw new RuntimeException("NIN range start/end must not be null");
        }

        if (start > end) {
            throw new RuntimeException("NIN range start must not be greater than end");
        }

        log.info("Starting parallel processing for strategy={} rangeStart={} rangeEnd={}",
                strategyName, start, end);

        long currentRangeStart = start;
        int rangeIndex = 1;

        while (currentRangeStart <= end) {
            long currentRangeEnd = currentRangeStart + rangeChunkSize - 1;

            if (currentRangeEnd > end) {
                currentRangeEnd = end;
            }

            log.info("Range window {} started for strategy={} rangeStart={} rangeEnd={}",
                    rangeIndex, strategyName, currentRangeStart, currentRangeEnd);

            boolean hasDataInThisRange = processOneRange(
                    strategy,
                    strategyName,
                    currentRangeStart,
                    currentRangeEnd
            );

            log.info("Range window {} finished for strategy={} rangeStart={} rangeEnd={}",
                    rangeIndex, strategyName, currentRangeStart, currentRangeEnd);

            if (!hasDataInThisRange) {
                log.info("Stopping range loop early because range {} - {} has no data for strategy={}",
                        currentRangeStart, currentRangeEnd, strategyName);
                break;
            }
            currentRangeStart = currentRangeEnd + 1;
            rangeIndex++;
        }

        log.info("Completed parallel processing for strategy={} rangeStart={} rangeEnd={}",
                strategyName, start, end);
    }
    /**
     * Processes one small range page by page.
     */
    private boolean processOneRange(IntegrationStrategy strategy,
                                 String strategyName,
                                 Long rangeStart,
                                 Long rangeEnd) {
        int pageNumber = 0;
        boolean hasAnyDataInThisRange = false;

        log.info("Processing range {} - {} for strategy={}",
                rangeStart, rangeEnd, strategyName);

        while (true) {
            Page<BeneficiaryEntity> page = loadPage(strategy, pageNumber, rangeStart, rangeEnd);

            if (!page.hasContent()) {
                log.info("No more beneficiaries in range  {} - {} for strategy={}",
                        rangeStart, rangeEnd, strategyName);
                break;
            }
            hasAnyDataInThisRange = true;
            log.info("Processing page {} in range {} - {} for strategy={} with {} beneficiary record(s)",
                    pageNumber, rangeStart, rangeEnd, strategyName, page.getNumberOfElements());

            //Call api integration side
            List<CompletableFuture<Object>> futures = createPageTasks(strategy, strategyName, page);

           //wait the join
            waitForPageTasks(futures);

            //prepare list of result
            List<Object> pageResults = collectPageResults(futures);


            //save the list
            savePageResults(strategy, pageResults);

            pageNumber++;
        }
        return hasAnyDataInThisRange;
    }

    /**
     * Loads one beneficiary page from the repository.
     */
    private Page<BeneficiaryEntity> loadPage(IntegrationStrategy strategy,
                                             int pageNumber,
                                             Long rangeStart,
                                             Long rangeEnd) {
        return strategy.getBeneficiaryRepo()
                .findBeneficiaries(PageRequest.of(pageNumber, chunkSize), rangeStart, rangeEnd);
    }

    /**
     * Creates one async task for each beneficiary in the page.
     */
    private List<CompletableFuture<Object>> createPageTasks(IntegrationStrategy strategy,
                                                            String strategyName,
                                                            Page<BeneficiaryEntity> page) {

        List<CompletableFuture<Object>> futures = new ArrayList<>();

        for (BeneficiaryEntity beneficiary : page.getContent()) {
            futures.add(createOneTask(strategy, strategyName, beneficiary.getNin()));
        }

        return futures;
    }

    /**
     * Creates one async task for one NIN.
     */
    private CompletableFuture<Object> createOneTask(IntegrationStrategy strategy,
                                                    String strategyName,
                                                    Long nin) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return strategy.getDataForNin(nin);
            } catch (Exception e) {
                log.error("Failed processing NIN={} in strategy={}. Error={}",
                        nin, strategyName, e.getMessage(), e);
                return null;
            }
        }, executorService);
    }

    /**
     * Waits until all page tasks finish.
     */
    private void waitForPageTasks(List<CompletableFuture<Object>> futures) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    /**
     * Collects only successful page results.
     */
    private List<Object> collectPageResults(List<CompletableFuture<Object>> futures) {
        List<Object> pageResults = new ArrayList<Object>();

        for (CompletableFuture<Object> future : futures) {
            Object result = future.join();

            if (result != null) {
                pageResults.add(result);
            }
        }

        return pageResults;
    }

    /**
     * Saves the page results in batch if there is data to save.
     */
    private void savePageResults(IntegrationStrategy strategy, List<Object> pageResults) {
        if (!pageResults.isEmpty()) {
            strategy.saveBatch(pageResults);
        }
    }

    /**
     * Shuts down the virtual thread executor when the application stops.
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        log.info("Virtual thread executor shutdown completed");
    }
}
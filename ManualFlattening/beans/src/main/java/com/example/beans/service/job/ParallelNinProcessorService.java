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
            @Value("${job.threads.count}") int threadCount,
            @Value("${job.db.chunk}") int chunkSize,
            @Value("${job.range.chunk}") long rangeChunkSize
    ) {
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.chunkSize = chunkSize;
        this.rangeChunkSize = rangeChunkSize;
    }

    /**
     * Processes one audit range.
     *
     * Flow:
     * 1. Split the big audit range into smaller range windows.
     * 2. For each small range window, fetch beneficiaries page by page.
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

        // Split the audit range into smaller windows
        while (currentRangeStart <= end) {
            //1 + 100 - 1 = 100 -> 100 numbers not 101 cus range.chunk=100 not 101
            //to make the range size exactly equal to rangeChunkSize.
            long currentRangeEnd = currentRangeStart + rangeChunkSize - 1;

            //This handles the last window: cus the last range may be smaller than rangeChunkSize.
            /**
             * currentRangeStart = 201
             * currentRangeEnd = 201 + 100 - 1 = 300
             *
             * But original audit end is only 230.
             */
            if (currentRangeEnd > end) {
                currentRangeEnd = end;
            }

            processOneRange(strategy, strategyName, currentRangeStart, currentRangeEnd);

            //next range starts(101) exactly after current range ends(100)
            currentRangeStart = currentRangeEnd + 1;
        }

        log.info("Completed parallel processing for strategy={} rangeStart={} rangeEnd={}",
                strategyName, start, end);
    }

    /**
     * Processes one small range window page by page.
     * Example:
     * If range window is 1..100 and page size is 25,
     * then pages will be processed as 25 + 25 + 25 + 25.
     */
    private void processOneRange(IntegrationStrategy strategy,
                                 String strategyName,
                                 Long rangeStart,
                                 Long rangeEnd) {

        int pageNumber = 0;

        log.info("Processing range window {} - {} for strategy={}",
                rangeStart, rangeEnd, strategyName);

        while (true) {
            // Fetch one DB page inside the current range window
            Page<BeneficiaryEntity> page = strategy.getBeneficiaryRepo()
                    .findBeneficiaries(PageRequest.of(pageNumber, chunkSize), rangeStart, rangeEnd);

            if (!page.hasContent()) {
                log.info("No more beneficiaries in range window {} - {} for strategy={}",
                        rangeStart, rangeEnd, strategyName);
                break;
            }

            log.info("Processing page {} in range window {} - {} for strategy={} with {} beneficiary record(s)",
                    pageNumber, rangeStart, rangeEnd, strategyName, page.getNumberOfElements());

            List<CompletableFuture<Object>> futures = new ArrayList<CompletableFuture<Object>>();

            // Create one async task for each beneficiary in this page
            for (BeneficiaryEntity beneficiary : page.getContent()) {
                final Long nin = beneficiary.getNin();

                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return strategy.prepareForNin(nin);
                    } catch (Exception e) {
                        log.error("Failed processing NIN={} in strategy={}. Error={}",
                                nin, strategyName, e.getMessage(), e);
                        return null;
                    }
                }, executorService));
            }

            // Wait until all tasks in this page finish
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<Object> pageResults = new ArrayList<Object>();

            // Collect only successful prepared results
            for (CompletableFuture<Object> future : futures) {
                Object result = future.join();

                if (result != null) {
                    pageResults.add(result);
                }
            }

            // Save this page in batch
            if (!pageResults.isEmpty()) {
                strategy.saveBatch(pageResults);
            }

            pageNumber++;
        }
    }

    /**
     * Shuts down the thread pool when the application stops.
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        log.info("ParallelNinProcessorService thread pool shutdown completed");
    }
}
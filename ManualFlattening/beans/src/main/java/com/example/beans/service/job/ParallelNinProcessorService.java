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

/**
 * ParallelNinProcessorService is a Spring @Service, so usually it is a singleton.
 * That means all requests share the same object, and therefore share the same pageNumber.
 * pageNumber is used only by the main loop thread inside one method call-> It is not shared by worker threads.
 * Different insert logic per service: some services insert one row, some services insert parent + child rows
 * some services need returned generated ids. So it is better that each strategy handles its own save logic.
 *That is why my design uses:
 * shared threading service for parallelism
 * separate strategy class for business logic
 */
@Slf4j
@Service
public class ParallelNinProcessorService {

    private final ExecutorService executorService;
    private final int chunkSize;

    public ParallelNinProcessorService(
            @Value("${job.threads.count}") int threadCount,
            @Value("${job.db.chunk}") int chunkSize
    ) {
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.chunkSize = chunkSize;
    }
//choose strategy → process NIN in parallel → each thread saves its own result()
    public void processInParallel(IntegrationStrategy strategy) {
        String strategyName = strategy.getClass().getSimpleName();
        int pageNumber = 0;

        log.info("Starting parallel processing for strategy={}", strategyName);

        //Main loop thread
        while (true) {
            Page<BeneficiaryEntity> page =
                    strategy.getBeneficiaryRepo()
                            .findAllByOrderByNinAsc(PageRequest.of(pageNumber, chunkSize));

            if (!page.hasContent()) {
                log.info("No more beneficiaries to process for strategy={}", strategyName);
                break;
            }

            log.info("Processing page {} for strategy={} with {} beneficiary record(s)",
                    pageNumber, strategyName, page.getNumberOfElements());

            List<CompletableFuture<Void>> futures = new ArrayList<CompletableFuture<Void>>();

            for (BeneficiaryEntity beneficiary : page.getContent()) {
                final Long nin = beneficiary.getNin();
//It creates one task per NIN, but those tasks are executed by a fixed-size thread pool.
                futures.add(CompletableFuture.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            strategy.insertForNin(nin);
                        } catch (Exception e) {
                            log.error("Failed processing NIN={} in strategy={}. Error={}",
                                    nin, strategyName, e.getMessage(), e);
                        }
                    }
                }, executorService));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            pageNumber++;
        }

        log.info("Completed parallel processing for strategy={}", strategyName);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        log.info("ParallelNinProcessorService thread pool shutdown completed");
    }
}
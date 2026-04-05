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

    public ParallelNinProcessorService(
            @Value("${job.threads.count}") int threadCount,
            @Value("${job.db.chunk}") int chunkSize
    ) {
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.chunkSize = chunkSize;
    }

    public void processInParallel(IntegrationStrategy strategy) {
        String strategyName = strategy.getClass().getSimpleName();
        int pageNumber = 0;

        log.info("Starting parallel processing for strategy={}", strategyName);

        //first time
        //page 0 loaded
        //25 beneficiaries in the page
        while (true) {
            Page<BeneficiaryEntity> page =
                    strategy.getBeneficiaryRepo()
                            .findAllByOrderByNinAsc(PageRequest.of(pageNumber, chunkSize)); //25 nin

            if (!page.hasContent()) {
                log.info("No more beneficiaries to process for strategy={}", strategyName);
                break;
            }

            log.info("Processing page {} for strategy={} with {} beneficiary record(s)",
                    pageNumber, strategyName, page.getNumberOfElements());

            List<CompletableFuture<Object>> futures = new ArrayList<CompletableFuture<Object>>();

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

            //do not save immediately when one thread finishes-> wait until all 25 NINs in the page finish preparing
            //Only after that, the processor collects all returned objects into pageResults.
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<Object> pageResults = new ArrayList<Object>();
            for (CompletableFuture<Object> future : futures) {
                Object result = future.join();
                if (result != null) {
                    pageResults.add(result);
                }
            }

            if (!pageResults.isEmpty()) {
                strategy.saveBatch(pageResults);
            }

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
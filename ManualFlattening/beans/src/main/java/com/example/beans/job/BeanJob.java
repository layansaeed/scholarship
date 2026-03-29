package com.example.beans.job;

import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.GenericEntityRepository;
import com.example.beans.service.EntityDefinitionRegistry;
import com.example.beans.service.XMLBeanLoaderService;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BeanJob {

    private final XMLBeanLoaderService xmlLoader;
    private final EntityDefinitionRegistry registry;

    public BeanJob(XMLBeanLoaderService xmlLoader,
                   EntityDefinitionRegistry registry) {

        this.xmlLoader = xmlLoader;
        this.registry = registry;
    }

    /**
     * Runs once at application startup.
     *
     * Purpose:
     * - Loads entity definitions from XML into the in-memory registry.
     * - Does NOT insert anything into the database (insert logic is triggered via controller/service).
     */
    @PostConstruct
    public void run() {
        try {
            log.info("Starting BeanJob to load XML entity...");
            xmlLoader.loadBeansFromXML();
            log.info(" XML loading complete into BeanJob: Loaded {} entity definition(s).", registry.getAll().size());

        } catch (Exception e) {
            log.error("Startup failed: {}", e.getMessage(), e);
            throw new RuntimeException("Startup failed", e);
        }
    }
}
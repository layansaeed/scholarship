package com.example.beans.service.pattern;

import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import com.example.beans.service.job.ParallelNinProcessorService;

public interface IntegrationStrategy {

    void insertForNin(Long nin);

    BeneficiaryJpaRepository getBeneficiaryRepo();

    EntityDefinitionRegistry getRegistry();

    ParallelNinProcessorService getParallelNinProcessorService();

    default void insertForAllNins() {
        getParallelNinProcessorService().processInParallel(this);
    }

    default EntityDefinition requireEntity(String entityName) {
        return getRegistry().get(entityName);
    }
}
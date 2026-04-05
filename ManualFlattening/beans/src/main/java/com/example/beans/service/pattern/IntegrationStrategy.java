package com.example.beans.service.pattern;

import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import com.example.beans.service.job.ParallelNinProcessorService;

import java.util.List;

public interface IntegrationStrategy {

    //thread prepares data only
    Object prepareForNin(Long nin);

    //after whole page finishes, save once
    void saveBatch(List<Object> pageResults);

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
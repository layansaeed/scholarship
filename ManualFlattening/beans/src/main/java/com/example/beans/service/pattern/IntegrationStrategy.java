package com.example.beans.service.pattern;

import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import com.example.beans.service.job.ParallelNinProcessorService;

import java.util.List;

public interface IntegrationStrategy {

    Object prepareForNin(Long nin);

    void saveBatch(List<Object> pageResults);

    BeneficiaryJpaRepository getBeneficiaryRepo();

    EntityDefinitionRegistry getRegistry();

    ParallelNinProcessorService getParallelNinProcessorService();

    default void insert(Long start, Long end) {
        getParallelNinProcessorService().processInParallel(this, start, end);
    }

    default EntityDefinition requireEntity(String entityName) {
        return getRegistry().get(entityName);
    }
}
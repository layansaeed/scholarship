package com.example.beans.service.pattern;

import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import com.example.beans.service.job.ParallelNinProcessorService;

import java.util.List;

public interface IntegrationStrategy {
    /**
     * Why getKey() is needed in this flow
     *
     * Because there are 2 separate lookups:
     *
     * Lookup A — registry chooses the correct strategy object
     * "SCHOLARSHIP" (from DB)-> ScholarshipIntegrationStrategy
     * Lookup B — strategy chooses the correct XML entity definition
     * "SCHOLARSHIP" (from xml)-> EntityDefinition from XML
     * getKey() gives one shared key for:
     * strategy registry
     * entity registry
     * @return
     */
    // dynamic key used for lookup
    String getKey();

    Object getDataForNin(Long nin);

    void saveBatch(List<Object> pageResults);

    BeneficiaryJpaRepository getBeneficiaryRepo();

    EntityDefinitionRegistry getRegistry();

    ParallelNinProcessorService getParallelNinProcessorService();

    default void insert(Long start, Long end) {
        getParallelNinProcessorService().processInParallel(this, start, end);
    }
    default EntityDefinition requirePrimaryEntity() {
        //Now EntityDefinitionRegistry looks in (XML-loaded) map and finds
        //we already apply entities.put(entityName, def); when call xml loader class
        //entity name -> job name -> key strategy / def -> from xml
        return getRegistry().get(getKey());
    }

    default EntityDefinition requireEntity(String entityName) {
        return getRegistry().get(entityName);
    }

}
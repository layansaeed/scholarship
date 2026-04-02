package com.example.beans.service.pattern;
import com.example.beans.model.EntityDefinition;
import com.example.beans.repository.BeneficiaryJpaRepository;
import com.example.beans.service.bean.EntityDefinitionRegistry;
import java.util.List;

/**
 * This is the strategy contract as part of its API.
 * Every integration service must implement these common operations.
 * "What must every strategy publicly promise"
 * default method has common body btw many strategies
 * public method without body cus each strategy has own logic in its class
 */
public interface IntegrationStrategy {

    void insertForNin(Long nin);

    BeneficiaryJpaRepository getBeneficiaryRepo();

    EntityDefinitionRegistry getRegistry();

    default void insertForAllNins() {
        List<Long> nins = getAllNinsSorted();

        if (nins.isEmpty()) {
            throw new RuntimeException("No NINs found in Beneficiary table");
        }

        for (Long nin : nins) {
            insertForNin(nin);
        }
    }

    default EntityDefinition requireEntity(String entityName) {
        return getRegistry().get(entityName);
    }

    default List<Long> getAllNinsSorted() {
        return getBeneficiaryRepo().findAllNinsSorted();
    }

}
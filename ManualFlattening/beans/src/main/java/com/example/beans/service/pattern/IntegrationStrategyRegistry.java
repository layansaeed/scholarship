package com.example.beans.service.pattern;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry already has all strategy beans collected automatically:
 *
 * "SCHOLARSHIP" -> ScholarshipIntegrationStrategy
 * "HRSD_DIS_ASS" -> DisabilityIntegrationStrategy
 *
 * so it returns the correct object.
 *
 * Because of constructor: Spring automatically gives this class all beans that implement IntegrationStrategy
 */
@Component
public class IntegrationStrategyRegistry {

    private final Map<String, IntegrationStrategy> strategies;

    public IntegrationStrategyRegistry(List<IntegrationStrategy> strategyList) {
        this.strategies = new LinkedHashMap<>();

        for (IntegrationStrategy strategy : strategyList) {
            String key = strategy.getKey();

            if (strategies.containsKey(key)) {
                throw new IllegalStateException("Duplicate strategy found for key: " + key);
            }

            strategies.put(key, strategy);
        }
    }

    public IntegrationStrategy getStrategy(String key) {
        IntegrationStrategy strategy = strategies.get(key);

        if (strategy == null) {
            throw new RuntimeException("No integration strategy found for key: " + key);
        }

        return strategy;
    }
}
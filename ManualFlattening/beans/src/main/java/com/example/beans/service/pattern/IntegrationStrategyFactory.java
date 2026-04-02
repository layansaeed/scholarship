package com.example.beans.service.pattern;

import com.example.beans.constant.IntegrationType;
import com.example.beans.service.integration.DisabilityIntegrationStrategy;
import com.example.beans.service.integration.ScholarshipIntegrationStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class IntegrationStrategyFactory {

    private final Map<IntegrationType, IntegrationStrategy> strategies;

    public IntegrationStrategyFactory(
            DisabilityIntegrationStrategy disabilityIntegrationStrategy,
            ScholarshipIntegrationStrategy scholarshipIntegrationStrategy
    ) {
        this.strategies = new EnumMap<>(IntegrationType.class);

        this.strategies.put(IntegrationType.HRSD_DIS_ASS, disabilityIntegrationStrategy);

        this.strategies.put(IntegrationType.SCHOLARSHIP, scholarshipIntegrationStrategy);
    }

    public IntegrationStrategy getStrategy(IntegrationType type) {
        IntegrationStrategy strategy = strategies.get(type);

        if (strategy == null) {
            throw new RuntimeException("No integration strategy found for type: " + type);
        }

        return strategy;
    }
}
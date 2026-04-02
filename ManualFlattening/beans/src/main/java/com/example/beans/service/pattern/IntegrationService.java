//package com.example.beans.service.pattern;
//
//import com.example.beans.constant.IntegrationType;
//import org.springframework.stereotype.Service;
//
//@Service
//public class IntegrationService {
//
//    private final IntegrationStrategyFactory strategyFactory;
//
//    public IntegrationService(IntegrationStrategyFactory strategyFactory) {
//        this.strategyFactory = strategyFactory;
//    }
//
//    public void insertForNin(IntegrationType type, Long nin) {
//        strategyFactory.getStrategy(type).insertForNin(nin);
//    }
//
//    public void insertForAllNins(IntegrationType type) {
//        strategyFactory.getStrategy(type).insertForAllNins();
//
//    }
//}
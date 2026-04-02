package com.example.beans.controller;

import com.example.beans.constant.IntegrationType;
import com.example.beans.service.pattern.IntegrationStrategyFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disability")
public class DisabilityController {


    private final IntegrationStrategyFactory factory;

    public DisabilityController(IntegrationStrategyFactory factory) {

        this.factory = factory;
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insertSpecific(@RequestParam("nin") Long nin) {
        try {
            factory.getStrategy(IntegrationType.HRSD_DIS_ASS).insertForNin(nin);
          //  integrationService.insertForNin(IntegrationType.HRSD_DIS_ASS, nin);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/insert/all")
    public ResponseEntity<?> insertAll() {
        try {
            factory.getStrategy(IntegrationType.HRSD_DIS_ASS).insertForAllNins();
            //integrationService.insertForAllNins(IntegrationType.HRSD_DIS_ASS);
            return ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
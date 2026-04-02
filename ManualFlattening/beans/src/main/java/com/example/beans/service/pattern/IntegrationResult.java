//package com.example.beans.service.pattern;
//
//
//import com.example.beans.constant.IntegrationType;
//import lombok.Getter;
//
//
///**
// * This gives all strategies the same return type.
// * not use @AllArgsConstructor; totalInserted is derived, nobody should set it manually.
// *
// * @Data generates setters for non-final fields, equals, hashCode, toString, so since fields are final, you do not need @Data.
// */
//
//@Getter
////as audit class
//public class IntegrationResult {
//
//    private final IntegrationType type;
//    private final int processedNins;
//    private final int mainRowsInserted;
//    private final int childRowsInserted;
//    private final int totalInserted;
//
//    public IntegrationResult(IntegrationType type, int processedNins, int mainRowsInserted, int childRowsInserted) {
//        this.type = type;
//        this.processedNins = processedNins;
//        this.mainRowsInserted = mainRowsInserted;
//        this.childRowsInserted = childRowsInserted;
//        this.totalInserted = mainRowsInserted + childRowsInserted;
//    }
//
//    public IntegrationResult add(IntegrationResult other) {
//        if (other == null) {
//            return this;
//        }
//
//        if (this.type != other.type) {
//            throw new IllegalArgumentException("Cannot merge results of different integration types");
//        }
//
//        return new IntegrationResult(this.type, this.processedNins + other.processedNins, this.mainRowsInserted + other.mainRowsInserted, this.childRowsInserted + other.childRowsInserted);
//    }
//}
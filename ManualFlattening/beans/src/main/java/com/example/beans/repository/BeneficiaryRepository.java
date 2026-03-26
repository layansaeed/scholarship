//package com.example.beans.repository;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.simple.SimpleJdbcCall;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Repository
//public class BeneficiaryRepository {
//
//    private final SimpleJdbcCall getNinsCall;
//
//    public BeneficiaryRepository(JdbcTemplate jdbcTemplate) {
//        this.getNinsCall = new SimpleJdbcCall(jdbcTemplate)
//                .withSchemaName("dbo")
//                .withProcedureName("sp_get_beneficiary_nins")
//                // avoids metadata/signature issues
//                .withoutProcedureColumnMetaDataAccess()
//                // define how to read the result set
//                .returningResultSet("nins", (rs, rowNum) -> rs.getLong("BEN_ID"));
//    }
//
//    public List<Long> getAllNins() {
//        Map<String, Object> result = getNinsCall.execute();
//
//        @SuppressWarnings("unchecked")
//        List<Long> nins = (List<Long>) result.get("nins");
//
//        return (nins != null) ? nins : List.of();
//    }
//}
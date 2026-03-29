package com.example.beans.repository;

import com.example.beans.model.EntityDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class GenericEntityRepository {

    private final JdbcTemplate jdbcTemplate;

    public GenericEntityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Inserts all rows for a given entity definition (metadata is from XML),
     * but the runtime rows are passed as a parameter (not stored inside EntityDefinition).
     *
     * This keeps EntityDefinition immutable-ish (metadata only) and avoids per-request copies.
     *
     * @param entity EntityDefinition loaded from XML (table/schema/fieldMapping)
     * @param rows   runtime rows (each map key = javaFieldName from XML)
     * @return number of inserted rows
     */
    @Transactional
    public int insertAllRows(EntityDefinition entity, List<Map<String, Object>> rows) {

        int rowsCount = (rows == null) ? 0 : rows.size();

        log.info("Inserting entity '{}' into table '{}' (rows passed = {})",
                entity.getEntityName(), entity.getFullTableName(), rowsCount);

        if (rowsCount == 0) {
            log.info("No rows provided for entity '{}'. Nothing to insert.", entity.getEntityName());
            return 0;
        }

        // 1) Create insert object ONCE
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(entity.getTableName());

        if (entity.getSchema() != null && !entity.getSchema().trim().isEmpty()) {
            insert = insert.withSchemaName(entity.getSchema());
        }

        // 2) Configure columns ONCE (before first execute)
        String[] columns = entity.getFieldMapping().values().toArray(new String[0]);
        insert = insert.usingColumns(columns);

        // 3) Configure generated key column ONCE (safe even if we don't return it)
        insert = insert.usingGeneratedKeyColumns("row_id");

        // 4) Execute for each row (NO MORE configuration changes)
        int inserted = 0;

        for (int i = 0; i < rowsCount; i++) {
            Map<String, Object> rowData = rows.get(i);
            Map<String, Object> values = buildInsertValues(entity, rowData);

            insert.execute(values); // no key returned

            inserted++;
            log.info("Inserted row {}/{} for entity '{}'",
                    i + 1, rowsCount, entity.getEntityName());
        }

        log.info("Finished entity '{}': inserted {} row(s).", entity.getEntityName(), inserted);
        return inserted;
    }

    /**
     * Inserts all rows for a given entity definition and returns the generated row_id for each inserted row.
     *
     * @param entity EntityDefinition loaded from XML (table/schema/fieldMapping)
     * @param rows   runtime rows (each map key = javaFieldName from XML)
     * @return list of generated row_id values (one per inserted row)
     */
    @Transactional
    public List<Long> insertAllRowsReturnIds(EntityDefinition entity, List<Map<String, Object>> rows) {

        int rowsCount = (rows == null) ? 0 : rows.size();

        log.info("Inserting entity '{}' into table '{}' and returning row_id(s) (rows passed = {})",
                entity.getEntityName(), entity.getFullTableName(), rowsCount);

        if (rowsCount == 0) {
            log.info("No rows provided for entity '{}'. Nothing to insert.", entity.getEntityName());
            return Collections.emptyList();
        }

        // 1) Create insert object ONCE
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(entity.getTableName());

        if (entity.getSchema() != null && !entity.getSchema().trim().isEmpty()) {
            insert = insert.withSchemaName(entity.getSchema());
        }

        // 2) Configure columns ONCE
        String[] columns = entity.getFieldMapping().values().toArray(new String[0]);
        insert = insert.usingColumns(columns);

        // 3) Configure generated key column ONCE
        insert = insert.usingGeneratedKeyColumns("row_id");

        // 4) Execute for each row and collect ids
        List<Long> generatedIds = new ArrayList<>(rowsCount);

        for (int i = 0; i < rowsCount; i++) {
            Map<String, Object> rowData = rows.get(i);
            Map<String, Object> values = buildInsertValues(entity, rowData);

            Number id = insert.executeAndReturnKey(values);
            generatedIds.add(id.longValue());

            log.info("Inserted row {}/{} for entity '{}' -> generated row_id={}",
                    i + 1, rowsCount, entity.getEntityName(), id);
        }

        log.info("Finished entity '{}': inserted {} row(s). Returned {} row_id(s).",
                entity.getEntityName(), rowsCount, generatedIds.size());

        return generatedIds;
    }

    /**
     * Converts one runtime row (keys = javaFieldName) into DB insert values (keys = dbColumnName),
     * based on the mapping loaded from XML.
     *
     * @param entity  EntityDefinition metadata (fieldMapping)
     * @param rowData one runtime row, keyed by javaFieldName
     * @return map keyed by DB column names, ready for SimpleJdbcInsert
     */
    private Map<String, Object> buildInsertValues(EntityDefinition entity, Map<String, Object> rowData) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : entity.getFieldMapping().entrySet()) {
            String javaFieldName = entry.getKey();
            String dbColumnName = entry.getValue();

            Object value = rowData.get(javaFieldName);
            values.put(dbColumnName, value);
        }

        return values;
    }
}
package com.example.beans.repository;

import com.example.beans.model.EntityDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
     * Inserts all rows for a given entity definition using {@link SimpleJdbcInsert}.
     * Key points:
     * - No SQL query text is written manually.
     * - Column list comes from XML fieldMapping.
     * - Uses DB-generated identity column "row_id".
     *
     * @param entity entity definition loaded from XML (with rows filled)
     * @return number of rows inserted
     */
    @Transactional
    public int insertAllRows(EntityDefinition entity) {

        int rowsInXml = (entity.getRows() == null) ? 0 : entity.getRows().size();
        log.info("Inserting entity '{}' into table '{}' (rows = {})",
                entity.getEntityName(), entity.getFullTableName(), rowsInXml);

        if (rowsInXml == 0) {
            log.info("No rows found for entity '{}'. Nothing to insert.", entity.getEntityName());
            return 0;
        }

        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(entity.getTableName());

        if (entity.getSchema() != null && !entity.getSchema().isBlank()) {
            insert = insert.withSchemaName(entity.getSchema());
        }

        String[] columns = entity.getFieldMapping().values().toArray(new String[0]);
        insert = insert.usingColumns(columns);

        insert = insert.usingGeneratedKeyColumns("row_id");

        int inserted = 0;

        for (int i = 0; i < rowsInXml; i++) {
            Map<String, Object> rowData = entity.getRows().get(i);
            Map<String, Object> values = buildInsertValues(entity, rowData);

            log.info("Entity '{}' rowData = {}", entity.getEntityName(), rowData);
            log.info("Entity '{}' DB values = {}", entity.getEntityName(), values);

            Number id = insert.executeAndReturnKey(values);

            inserted++;
            log.info("Inserted row {}/{} for entity '{}' -> row_id={}",
                    i + 1, rowsInXml, entity.getEntityName(), id);
        }

        log.info("Finished entity '{}': inserted {} row(s).", entity.getEntityName(), inserted);
        return inserted;
    }

    /**
     * Same as insertAllRows(...) but returns the generated row_id for each inserted row.
     * Useful when child tables need the parent row_id (like ScholarshipStageInformation).
     *
     * @return list of generated row_id values in the same order as entity.getRows()
     */
    @Transactional
    public List<Long> insertAllRowsReturnIds(EntityDefinition entity) {

        int rowsInXml = (entity.getRows() == null) ? 0 : entity.getRows().size();
        log.info("Inserting entity '{}' into table '{}' (rows in XML = {})",
                entity.getEntityName(), entity.getFullTableName(), rowsInXml);

        if (rowsInXml == 0) {
            log.info("No rows found for entity '{}'. Nothing to insert.", entity.getEntityName());
            return List.of();
        }

        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName(entity.getTableName());

        if (entity.getSchema() != null && !entity.getSchema().isBlank()) {
            insert = insert.withSchemaName(entity.getSchema());
        }

        String[] columns = entity.getFieldMapping().values().toArray(new String[0]);
        insert = insert.usingColumns(columns);

        insert = insert.usingGeneratedKeyColumns("row_id");

        List<Long> generatedIds = new ArrayList<>(rowsInXml);

        for (int i = 0; i < rowsInXml; i++) {
            Map<String, Object> rowData = entity.getRows().get(i);
            Map<String, Object> values = buildInsertValues(entity, rowData);

            Number id = insert.executeAndReturnKey(values);
            generatedIds.add(id.longValue());

            log.info("Inserted row {}/{} for entity '{}' -> generated row_id={}",
                    (i + 1), rowsInXml, entity.getEntityName(), id);
        }

        return generatedIds;
    }

    /**
     * Builds the DB insert map: dbColumnName -> value
     * using the XML mapping (javaFieldName -> dbColumnName).
     *
     * @param entity entity definition (contains fieldMapping)
     * @param rowData one row (keys are javaFieldName)
     * @return db values map (keys are dbColumnName)
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
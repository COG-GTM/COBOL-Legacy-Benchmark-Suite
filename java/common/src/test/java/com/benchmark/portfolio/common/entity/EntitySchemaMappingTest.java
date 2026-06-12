package com.benchmark.portfolio.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Validates that every column of the baseline DDL (java/db/ddl/V1__baseline_schema.sql,
 * ticket 0.2) is mapped by a JPA entity attribute with a compatible Java type and, for
 * NUMERIC columns, matching precision/scale. The DDL is executed against an in-memory
 * H2 database (PostgreSQL mode) and compared with the entity mappings via reflection
 * on the JPA annotations.
 */
class EntitySchemaMappingTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:schema_mapping;MODE=PostgreSQL;DATABASE_TO_LOWER=FALSE;DB_CLOSE_DELAY=-1";

    private static final List<Class<?>> ENTITY_CLASSES = List.of(
            PortfolioMaster.class,
            PortfolioTransaction.class,
            PortfolioPosition.class,
            HistoryRecord.class,
            ErrorLog.class,
            AuditLog.class);

    /** Java attribute type -> acceptable H2/PostgreSQL DATA_TYPE values. */
    private static final Map<Class<?>, Set<String>> COMPATIBLE_SQL_TYPES = Map.of(
            String.class, Set.of("CHARACTER", "CHARACTER VARYING"),
            BigDecimal.class, Set.of("NUMERIC"),
            LocalDate.class, Set.of("DATE"),
            LocalTime.class, Set.of("TIME"),
            LocalDateTime.class, Set.of("TIMESTAMP"),
            Long.class, Set.of("BIGINT"),
            Short.class, Set.of("SMALLINT"));

    private static Connection connection;

    @BeforeAll
    static void createSchema() throws Exception {
        connection = DriverManager.getConnection(JDBC_URL);
        Path ddl = Path.of("..", "db", "ddl", "V1__baseline_schema.sql");
        String sql = Files.readString(ddl);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @AfterAll
    static void close() throws Exception {
        connection.close();
    }

    @Test
    void everyEntityAttributeMapsToAnExistingColumnWithCompatibleType() throws Exception {
        for (MappedColumn mapped : collectMappedColumns()) {
            DbColumn db = findDbColumn(mapped.table(), mapped.column());
            assertThat(db)
                    .as("column %s.%s (attribute %s) must exist in DDL", mapped.table(), mapped.column(), mapped.attribute())
                    .isNotNull();
            assertThat(COMPATIBLE_SQL_TYPES.get(mapped.javaType()))
                    .as("attribute %s of type %s must have a known SQL type mapping", mapped.attribute(), mapped.javaType())
                    .contains(db.dataType());
            if (mapped.javaType() == BigDecimal.class) {
                assertThat(db.precision())
                        .as("precision of %s.%s (attribute %s)", mapped.table(), mapped.column(), mapped.attribute())
                        .isEqualTo(mapped.precision());
                assertThat(db.scale())
                        .as("scale of %s.%s (attribute %s)", mapped.table(), mapped.column(), mapped.attribute())
                        .isEqualTo(mapped.scale());
            }
            if (mapped.javaType() == String.class) {
                assertThat(db.precision())
                        .as("length of %s.%s (attribute %s)", mapped.table(), mapped.column(), mapped.attribute())
                        .isEqualTo(mapped.length());
            }
        }
    }

    @Test
    void everyDdlColumnIsMappedByAnEntityAttribute() throws Exception {
        Set<String> mapped = new HashSet<>();
        for (MappedColumn m : collectMappedColumns()) {
            mapped.add(m.table() + "." + m.column());
        }
        List<String> unmapped = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC'");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getString(1) + "." + rs.getString(2);
                if (!mapped.contains(key)) {
                    unmapped.add(key);
                }
            }
        }
        assertThat(unmapped).as("DDL columns without a mapped entity attribute").isEmpty();
    }

    private record MappedColumn(String table, String column, String attribute,
            Class<?> javaType, int precision, int scale, int length) {
    }

    private record DbColumn(String dataType, int precision, int scale) {
    }

    private static List<MappedColumn> collectMappedColumns() {
        List<MappedColumn> result = new ArrayList<>();
        for (Class<?> entity : ENTITY_CLASSES) {
            String table = entity.getAnnotation(Table.class).name();
            for (Field field : entity.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.isAnnotationPresent(EmbeddedId.class)) {
                    Class<?> idClass = field.getType();
                    assertThat(idClass.isAnnotationPresent(Embeddable.class)).isTrue();
                    for (Field idField : idClass.getDeclaredFields()) {
                        if (Modifier.isStatic(idField.getModifiers())) {
                            continue;
                        }
                        addColumn(result, table, entity, idField);
                    }
                } else {
                    addColumn(result, table, entity, field);
                }
            }
        }
        return result;
    }

    private static void addColumn(List<MappedColumn> result, String table, Class<?> entity, Field field) {
        Column column = field.getAnnotation(Column.class);
        assertThat(column)
                .as("field %s.%s must carry @Column", entity.getSimpleName(), field.getName())
                .isNotNull();
        result.add(new MappedColumn(table, column.name(),
                entity.getSimpleName() + "." + field.getName(),
                field.getType(), column.precision(), column.scale(), column.length()));
    }

    private static DbColumn findDbColumn(String table, String column) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DATA_TYPE, COALESCE(NUMERIC_PRECISION, CHARACTER_MAXIMUM_LENGTH), COALESCE(NUMERIC_SCALE, 0) "
                        + "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new DbColumn(rs.getString(1), rs.getInt(2), rs.getInt(3));
            }
        }
    }
}

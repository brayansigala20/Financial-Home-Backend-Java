package com.financialhome.financial_home_backend;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("db-test")
class PostgresqlMigrationTests {

    @Autowired
    DataSource dataSource;

    @Autowired
    Flyway flyway;

    @Test
    void createsExactlyTenModelTablesAndAllForeignKeys() throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            var tables = new HashSet<String>();
            try (var result = statement.executeQuery("""
                    SELECT tablename FROM pg_tables WHERE schemaname = 'public'
                    """)) {
                while (result.next()) {
                    tables.add(result.getString(1));
                }
            }
            assertEquals(Set.of("usuarios", "personas", "hogares", "integrantes_hogar",
                    "invitaciones_hogar", "cuentas_financieras", "cuentas_hogar", "tarjetas",
                    "movimientos_financieros", "detalles_movimiento", "flyway_schema_history"), tables);

            var relations = new HashSet<String>();
            try (var result = statement.executeQuery("""
                    SELECT conrelid::regclass::text || ':' || pg_get_constraintdef(oid)
                    FROM pg_constraint WHERE contype = 'f' AND connamespace = 'public'::regnamespace
                    """)) {
                while (result.next()) {
                    relations.add(result.getString(1));
                }
            }
            assertEquals(Set.of(
                    "personas:FOREIGN KEY (usuario_id) REFERENCES usuarios(id)",
                    "integrantes_hogar:FOREIGN KEY (hogar_id) REFERENCES hogares(id)",
                    "integrantes_hogar:FOREIGN KEY (persona_id) REFERENCES personas(id)",
                    "invitaciones_hogar:FOREIGN KEY (hogar_id) REFERENCES hogares(id)",
                    "invitaciones_hogar:FOREIGN KEY (invitado_usuario_id) REFERENCES usuarios(id)",
                    "invitaciones_hogar:FOREIGN KEY (invitado_por_usuario_id) REFERENCES usuarios(id)",
                    "cuentas_financieras:FOREIGN KEY (propietario_persona_id) REFERENCES personas(id)",
                    "cuentas_financieras:FOREIGN KEY (propietario_hogar_id) REFERENCES hogares(id)",
                    "cuentas_hogar:FOREIGN KEY (cuenta_id) REFERENCES cuentas_financieras(id)",
                    "cuentas_hogar:FOREIGN KEY (hogar_id) REFERENCES hogares(id)",
                    "tarjetas:FOREIGN KEY (cuenta_id) REFERENCES cuentas_financieras(id)",
                    "movimientos_financieros:FOREIGN KEY (hogar_id) REFERENCES hogares(id)",
                    "movimientos_financieros:FOREIGN KEY (registrado_por_persona_id) REFERENCES personas(id)",
                    "movimientos_financieros:FOREIGN KEY (realizado_por_persona_id) REFERENCES personas(id)",
                    "movimientos_financieros:FOREIGN KEY (movimiento_revertido_id) REFERENCES movimientos_financieros(id)",
                    "detalles_movimiento:FOREIGN KEY (movimiento_id) REFERENCES movimientos_financieros(id)",
                    "detalles_movimiento:FOREIGN KEY (cuenta_id) REFERENCES cuentas_financieras(id)",
                    "detalles_movimiento:FOREIGN KEY (tarjeta_id, cuenta_id) REFERENCES tarjetas(id, cuenta_id)"
            ), relations);
        }
    }

    @Test
    void validatesMigrationAndDoesNotApplyItTwice() {
        flyway.validate();
        assertEquals("1", flyway.info().current().getVersion().toString());
        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    @Test
    void acceptsValidDataAndRejectsInvalidRelationshipsAndChecks() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                execute(connection, """
                        INSERT INTO usuarios (id, email) VALUES
                          ('00000000-0000-0000-0000-000000000001', 'migration@example.test');
                        INSERT INTO personas (id, usuario_id, nombre) VALUES
                          ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Prueba');
                        INSERT INTO hogares (id, nombre) VALUES
                          ('00000000-0000-0000-0000-000000000003', 'Prueba');
                        INSERT INTO integrantes_hogar (id, hogar_id, persona_id) VALUES
                          ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002');
                        INSERT INTO cuentas_financieras (id, nombre, tipo, tipo_titular, propietario_hogar_id) VALUES
                          ('00000000-0000-0000-0000-000000000005', 'Uno', 'BANCARIA', 'HOGAR', '00000000-0000-0000-0000-000000000003'),
                          ('00000000-0000-0000-0000-000000000006', 'Dos', 'BANCARIA', 'HOGAR', '00000000-0000-0000-0000-000000000003');
                        INSERT INTO tarjetas (id, cuenta_id, nombre, tipo) VALUES
                          ('00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000005', 'Prueba', 'FISICA');
                        INSERT INTO movimientos_financieros
                          (id, hogar_id, tipo, concepto, registrado_por_persona_id, realizado_por_persona_id, fecha_movimiento) VALUES
                          ('00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000003', 'GASTO', 'Prueba',
                           '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', now());
                        INSERT INTO detalles_movimiento (id, movimiento_id, cuenta_id, forma_pago, tarjeta_id, monto) VALUES
                          ('00000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000008',
                           '00000000-0000-0000-0000-000000000005', 'TARJETA_DEBITO', '00000000-0000-0000-0000-000000000007', -100);
                        """);
                rejects(connection, "23514", "UPDATE detalles_movimiento SET forma_pago = NULL");
                rejects(connection, "23514", "UPDATE detalles_movimiento SET forma_pago = 'EFECTIVO'");
                rejects(connection, "23503", "UPDATE detalles_movimiento SET cuenta_id = '00000000-0000-0000-0000-000000000006'");
                rejects(connection, "23514", "UPDATE detalles_movimiento SET monto = 0");
                rejects(connection, "23514", "UPDATE cuentas_financieras SET propietario_hogar_id = NULL");
                rejects(connection, "23514", "UPDATE usuarios SET estado = 'DESCONOCIDO'");
                rejects(connection, "23514", "UPDATE movimientos_financieros SET estado = 'CONFIRMADO'");
                rejects(connection, "23505", """
                        INSERT INTO integrantes_hogar (id, hogar_id, persona_id) VALUES
                        ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002')
                        """);
            } finally {
                connection.rollback();
            }
        }
    }

    private void rejects(Connection connection, String sqlState, String sql) throws SQLException {
        var savepoint = connection.setSavepoint();
        try {
            var exception = assertThrows(SQLException.class, () -> execute(connection, sql));
            assertEquals(sqlState, exception.getSQLState(), sql);
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}

package io.github.rmannibucau.quarkus.jdbc.generic.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * The application is configured against a MySQL driver and URL in {@code application.properties}
 * and repointed to H2 with the {@code QUARKUS_DATASOURCE_JDBC_GENERIC_DRIVER} and
 * {@code QUARKUS_DATASOURCE_JDBC_URL} environment variables set by the surefire configuration of
 * this module: the datasource must use the environment provided driver and URL, not the build-time
 * ones.
 */
@QuarkusTest
public class GenericJdbcDataSourceTest {

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void driverIsResolvedFromTheEnvironmentAtRuntime() throws SQLException {
        final var connectionFactoryConfiguration = dataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        assertThat(connectionFactoryConfiguration.connectionProviderClass().getName())
                .isNotEqualTo("com.mysql.cj.jdbc.Driver")
                .isEqualTo("org.h2.Driver");
        assertThat(connectionFactoryConfiguration.jdbcUrl())
                .isNotEqualTo("jdbc:mysql://build-time-host:3306/ignored")
                .isEqualTo("jdbc:h2:mem:generic-it");

        try (final var connection = dataSource.getConnection();
                final var statement = connection.createStatement();
                final var resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
    }
}

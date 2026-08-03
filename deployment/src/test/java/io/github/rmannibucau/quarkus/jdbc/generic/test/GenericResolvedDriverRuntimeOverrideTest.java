package io.quarkiverse.jdbc.jdbc.generic.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Validates the driver is a real runtime decision: the build-time visible configuration points to a
 * MySQL driver which is not even present in the test classpath, and the runtime configuration, only
 * applied after augmentation, overrides it with the H2 driver actually used by the datasource.
 * <p>
 * If the build-time value leaked into the recorded datasource, the startup would fail to load the
 * MySQL driver class instead of using H2.
 */
public class GenericResolvedDriverRuntimeOverrideTest {

    @Inject
    AgroalDataSource dataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "generic")
            // build-time visible values, the driver is intentionally absent from the test classpath
            .overrideConfigKey("quarkus.datasource.jdbc-generic.driver", "com.mysql.cj.jdbc.Driver")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:mysql://build-time-host:3306/ignored")
            // runtime only configuration, applied after the application is built
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc-generic.driver", "org.h2.Driver")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:generic-runtime-override");

    @Test
    public void runtimeConfigurationOverridesBuildTimeConfiguration() throws SQLException {
        final var connectionFactoryConfiguration = dataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        assertThat(connectionFactoryConfiguration.connectionProviderClass().getName()).isEqualTo("org.h2.Driver");
        assertThat(connectionFactoryConfiguration.jdbcUrl()).isEqualTo("jdbc:h2:mem:generic-runtime-override");

        try (final var connection = dataSource.getConnection();
                final var statement = connection.createStatement();
                final var resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}

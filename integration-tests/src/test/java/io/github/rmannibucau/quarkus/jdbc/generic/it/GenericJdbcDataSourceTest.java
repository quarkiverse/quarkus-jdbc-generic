package io.github.rmannibucau.quarkus.jdbc.generic.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GenericJdbcDataSourceTest {

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void driverIsResolvedAtRuntime() throws SQLException {
        final var connectionFactoryConfiguration = dataSource.getConfiguration().connectionPoolConfiguration()
                .connectionFactoryConfiguration();
        assertThat(connectionFactoryConfiguration.connectionProviderClass().getName()).isEqualTo("org.h2.Driver");

        try (final var connection = dataSource.getConnection();
                final var statement = connection.createStatement();
                final var resultSet = statement.executeQuery("SELECT 1")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }
}

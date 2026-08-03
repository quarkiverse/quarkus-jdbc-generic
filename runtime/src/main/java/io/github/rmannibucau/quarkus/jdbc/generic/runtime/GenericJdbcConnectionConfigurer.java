package io.quarkiverse.jdbc.jdbc.generic.runtime;

import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.quarkus.agroal.runtime.AgroalConnectionConfigurer;
import io.quarkus.agroal.runtime.JdbcDriver;

/**
 * Connection configurer of the {@link GenericJdbc#DB_KIND} database kind.
 * <p>
 * No database specific tuning can be provided for a driver only known at runtime, so this
 * implementation only avoids the warnings the default implementation logs for unknown kinds.
 */
@JdbcDriver(GenericJdbc.DB_KIND)
public class GenericJdbcConnectionConfigurer implements AgroalConnectionConfigurer {

    @Override
    public void setExceptionSorter(final String databaseKind,
            final AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        // no exception sorter can be provided for a driver only known at runtime, keep the Agroal default
    }
}

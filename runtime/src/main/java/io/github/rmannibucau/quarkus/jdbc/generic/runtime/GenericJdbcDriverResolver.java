package io.quarkiverse.jdbc.jdbc.generic.runtime;

import java.sql.Driver;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.AgroalDataSourceSupport;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Resolves the JDBC driver of datasources of database kind {@link GenericJdbc#DB_KIND} at runtime,
 * from the {@code quarkus.datasource[."datasource-name"].jdbc-generic.driver} configuration
 * property, instead of the placeholder driver class recorded at build time.
 * <p>
 * The resolution substitutes the {@link AgroalDataSourceSupport.Entry} of the datasource, which the
 * Agroal extension reads when it creates the {@code AgroalDataSource}, with an entry pointing to the
 * configured driver class. It is invoked from two places:
 * <ul>
 * <li>eagerly at runtime init, before application startup logic runs, with {@code failFast=true} so
 * configuration errors abort the boot with an actionable message;</li>
 * <li>lazily from {@link GenericJdbcResolvingDriver}, instantiated by the Agroal extension right
 * before it reads the entry, which makes the substitution independent of the startup ordering of
 * datasource consumers such as schema migration tools.</li>
 * </ul>
 */
public class GenericJdbcDriverResolver {

    private static final Logger log = Logger.getLogger(GenericJdbcDriverResolver.class);

    private final DataSourcesJdbcGenericDriverConfig genericDriverConfig;
    private final DataSourcesJdbcBuildTimeConfig jdbcBuildTimeConfig;
    private final DataSourcesJdbcRuntimeConfig jdbcRuntimeConfig;
    private final DataSourcesRuntimeConfig dataSourcesRuntimeConfig;
    private final Instance<AgroalDataSourceSupport> agroalDataSourceSupport;

    public GenericJdbcDriverResolver(final DataSourcesJdbcGenericDriverConfig genericDriverConfig,
            final DataSourcesJdbcBuildTimeConfig jdbcBuildTimeConfig,
            final DataSourcesJdbcRuntimeConfig jdbcRuntimeConfig,
            final DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            final Instance<AgroalDataSourceSupport> agroalDataSourceSupport) {
        this.genericDriverConfig = genericDriverConfig;
        this.jdbcBuildTimeConfig = jdbcBuildTimeConfig;
        this.jdbcRuntimeConfig = jdbcRuntimeConfig;
        this.dataSourcesRuntimeConfig = dataSourcesRuntimeConfig;
        this.agroalDataSourceSupport = agroalDataSourceSupport;
    }

    /**
     * Resolves the driver of every active datasource of database kind {@link GenericJdbc#DB_KIND}.
     *
     * @param failFast when {@code true}, configuration errors are thrown as {@link ConfigurationException},
     *        when {@code false} they are only logged at debug level and the datasource entry is left untouched
     */
    public void resolveAll(final boolean failFast) {
        if (!agroalDataSourceSupport.isResolvable()) {
            return;
        }
        final var entries = agroalDataSourceSupport.get().entries;
        for (final var entry : List.copyOf(entries.values())) {
            if (!GenericJdbc.DB_KIND.equals(entry.resolvedDbKind) || !isActive(entry.dataSourceName)) {
                continue;
            }
            final String driverClassName;
            try {
                driverClassName = resolveDriverClassName(entry.dataSourceName);
            } catch (final ConfigurationException e) {
                if (failFast) {
                    throw e;
                }
                log.debugf("Skipping the JDBC driver resolution of datasource '%s': %s",
                        entry.dataSourceName, e.getMessage());
                continue;
            }
            if (!driverClassName.equals(entry.resolvedDriverClass)) {
                entries.put(entry.dataSourceName, new AgroalDataSourceSupport.Entry(
                        entry.dataSourceName, entry.resolvedDbKind, entry.dbVersion, driverClassName, entry.isDefault));
                log.debugf("Resolved the JDBC driver of datasource '%s' to '%s'", entry.dataSourceName, driverClassName);
            }
        }
    }

    private boolean isActive(final String dataSourceName) {
        final var dataSourceRuntimeConfig = dataSourcesRuntimeConfig.dataSources().get(dataSourceName);
        if (dataSourceRuntimeConfig != null && dataSourceRuntimeConfig.active().isPresent()
                && !dataSourceRuntimeConfig.active().get()) {
            return false;
        }
        // a datasource without a JDBC URL is never created by the Agroal extension
        final var dataSourceJdbcRuntimeConfig = jdbcRuntimeConfig.dataSources().get(dataSourceName);
        return dataSourceJdbcRuntimeConfig == null || dataSourceJdbcRuntimeConfig.jdbc().url().isPresent();
    }

    private String resolveDriverClassName(final String dataSourceName) {
        final var driverProperty = DataSourceUtil.dataSourcePropertyKey(dataSourceName, GenericJdbc.DRIVER_PROPERTY_RADICAL);
        final var driverClassName = genericDriverConfig.dataSources().get(dataSourceName).jdbcGeneric().driver()
                .orElseThrow(() -> new ConfigurationException(
                        "Datasource '" + dataSourceName + "' uses the '" + GenericJdbc.DB_KIND + "' database kind"
                                + " but does not configure the JDBC driver class to load."
                                + " To solve this, set the '" + driverProperty
                                + "' configuration property to the fully qualified name of a JDBC driver class"
                                + " present in the application classpath.",
                        Set.of(driverProperty)));
        validate(dataSourceName, driverProperty, driverClassName);
        return driverClassName;
    }

    private void validate(final String dataSourceName, final String driverProperty, final String driverClassName) {
        final Class<?> driverClass;
        try {
            driverClass = Class.forName(driverClassName, true, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Unable to load the JDBC driver class '" + driverClassName + "' configured with '" + driverProperty
                            + "' for datasource '" + dataSourceName
                            + "'. Ensure the driver is present in the application classpath.",
                    e, Set.of(driverProperty));
        }

        final var xa = jdbcBuildTimeConfig.dataSources().get(dataSourceName).jdbc()
                .transactions() == TransactionIntegration.XA;
        if (xa) {
            if (!XADataSource.class.isAssignableFrom(driverClass)) {
                throw new ConfigurationException(
                        "Datasource '" + dataSourceName + "' uses XA transactions but the driver class '" + driverClassName
                                + "' configured with '" + driverProperty + "' is not an implementation of "
                                + XADataSource.class.getName() + ".",
                        Set.of(driverProperty));
            }
        } else if (!Driver.class.isAssignableFrom(driverClass) && !DataSource.class.isAssignableFrom(driverClass)) {
            throw new ConfigurationException(
                    "The driver class '" + driverClassName + "' configured with '" + driverProperty + "' for datasource '"
                            + dataSourceName + "' is neither an implementation of " + Driver.class.getName() + " nor of "
                            + DataSource.class.getName() + ".",
                    Set.of(driverProperty));
        }
    }
}

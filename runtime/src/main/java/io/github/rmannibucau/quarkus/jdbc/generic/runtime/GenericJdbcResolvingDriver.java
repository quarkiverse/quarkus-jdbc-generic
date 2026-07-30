package io.github.rmannibucau.quarkus.jdbc.generic.runtime;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.util.Properties;
import java.util.logging.Logger;

import io.quarkus.arc.Arc;

/**
 * Inert {@link Driver} registered in {@code META-INF/services/java.sql.Driver} whose only purpose
 * is to trigger {@link GenericJdbcDriverResolver#resolveAll(boolean)} when it is instantiated.
 * <p>
 * The Agroal extension loads all {@code java.sql.Driver} service providers of the TCCL right before
 * it reads the driver class of the datasource it creates (see {@code DataSources#loadDriversInTCCL()}).
 * Instantiating this driver at that exact point guarantees the placeholder entry recorded at build time
 * is substituted with the configured driver before it is used, whatever the startup ordering of the
 * datasource consumers (schema migration tools, health checks, application startup logic, ...).
 * <p>
 * As a plain JDBC driver it is strictly well behaved and inert: it accepts no URL and connects to
 * nothing, so it is transparent for direct {@link java.sql.DriverManager} usage.
 */
public final class GenericJdbcResolvingDriver implements Driver {

    public GenericJdbcResolvingDriver() {
        try {
            final var container = Arc.container();
            if (container == null || !container.isRunning()) {
                // loaded outside a running Quarkus application, e.g. by DriverManager: stay inert
                return;
            }
            final var resolver = container.instance(GenericJdbcDriverResolver.class);
            if (resolver.isAvailable()) {
                resolver.get().resolveAll(false);
            }
        } catch (final RuntimeException e) {
            // never break the driver service loading, the eager startup validation reports actionable errors
            org.jboss.logging.Logger.getLogger(GenericJdbcResolvingDriver.class)
                    .debug("Generic JDBC driver resolution skipped", e);
        }
    }

    @Override
    public Connection connect(final String url, final Properties info) {
        return null;
    }

    @Override
    public boolean acceptsURL(final String url) {
        return false;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(GenericJdbcResolvingDriver.class.getName());
    }
}

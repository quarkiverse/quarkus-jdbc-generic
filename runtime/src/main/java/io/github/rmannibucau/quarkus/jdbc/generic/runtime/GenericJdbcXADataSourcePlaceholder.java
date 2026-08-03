package io.quarkiverse.jdbc.jdbc.generic.runtime;

import java.io.PrintWriter;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

/**
 * Build-time placeholder XA datasource for datasources of database kind {@link GenericJdbc#DB_KIND}
 * using XA transactions.
 * <p>
 * It only exists to satisfy the build-time validation and recording of the Agroal extension:
 * before the pool is created, {@link GenericJdbcDriverResolver} substitutes it with the
 * {@link XADataSource} implementation configured with
 * {@code quarkus.datasource[."datasource-name"].jdbc-generic.driver}, so this class is never
 * instantiated.
 */
public final class GenericJdbcXADataSourcePlaceholder implements XADataSource {

    @Override
    public XAConnection getXAConnection() {
        throw placeholderUsed();
    }

    @Override
    public XAConnection getXAConnection(final String user, final String password) {
        throw placeholderUsed();
    }

    @Override
    public PrintWriter getLogWriter() {
        throw placeholderUsed();
    }

    @Override
    public void setLogWriter(final PrintWriter out) {
        throw placeholderUsed();
    }

    @Override
    public void setLoginTimeout(final int seconds) {
        throw placeholderUsed();
    }

    @Override
    public int getLoginTimeout() {
        throw placeholderUsed();
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
        throw placeholderUsed();
    }

    private static IllegalStateException placeholderUsed() {
        return new IllegalStateException(GenericJdbcXADataSourcePlaceholder.class.getName()
                + " is a build-time placeholder and should never be used at runtime."
                + " The actual XA datasource implementation must be set with the"
                + " 'quarkus.datasource[.\"datasource-name\"].jdbc-generic.driver' configuration property.");
    }
}

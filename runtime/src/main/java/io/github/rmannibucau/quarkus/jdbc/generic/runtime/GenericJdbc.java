package io.quarkiverse.jdbc.jdbc.generic.runtime;

/**
 * Constants for the generic, runtime-resolved JDBC driver support.
 */
public final class GenericJdbc {

    /**
     * The database kind of datasources whose JDBC driver is resolved at runtime from the
     * {@code quarkus.datasource[."datasource-name"].jdbc-generic.driver} configuration property.
     */
    public static final String DB_KIND = "generic";

    /**
     * The datasource property radical of the runtime configuration property holding the driver class name,
     * i.e. the part after {@code quarkus.datasource[."datasource-name"].}.
     */
    public static final String DRIVER_PROPERTY_RADICAL = "jdbc-generic.driver";

    private GenericJdbc() {
    }
}

package io.quarkiverse.jdbc.jdbc.generic.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class GenericJdbcRecorder {

    /**
     * Eagerly resolves and validates the driver of every active datasource of database kind
     * {@link GenericJdbc#DB_KIND} so misconfiguration aborts the boot with an actionable error.
     */
    public void resolveDrivers() {
        final var resolver = Arc.container().instance(GenericJdbcDriverResolver.class);
        if (resolver.isAvailable()) {
            resolver.get().resolveAll(true);
        }
    }
}

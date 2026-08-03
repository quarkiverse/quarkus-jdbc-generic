package io.quarkiverse.jdbc.jdbc.generic.deployment;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.jdbc.jdbc.generic.runtime.GenericJdbc;
import io.quarkiverse.jdbc.jdbc.generic.runtime.GenericJdbcConnectionConfigurer;
import io.quarkiverse.jdbc.jdbc.generic.runtime.GenericJdbcDriverPlaceholder;
import io.quarkiverse.jdbc.jdbc.generic.runtime.GenericJdbcDriverResolver;
import io.quarkiverse.jdbc.jdbc.generic.runtime.GenericJdbcRecorder;
import io.quarkiverse.jdbc.jdbc.generic.runtime.GenericJdbcXADataSourcePlaceholder;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class GenericJdbcProcessor {

    private static final String FEATURE = "jdbc-generic";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    JdbcDriverBuildItem registerPlaceholderDriver() {
        // placeholder classes satisfying the build-time validation of the Agroal extension,
        // the actual driver is substituted at runtime by GenericJdbcDriverResolver
        return new JdbcDriverBuildItem(GenericJdbc.DB_KIND, GenericJdbcDriverPlaceholder.class.getName(),
                GenericJdbcXADataSourcePlaceholder.class.getName());
    }

    @BuildStep
    DefaultDataSourceDbKindBuildItem registerDefaultDbKind() {
        return new DefaultDataSourceDbKindBuildItem(GenericJdbc.DB_KIND);
    }

    @BuildStep
    void registerConfiguredDriversForReflection(final DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final var config = ConfigProvider.getConfig();
        for (final var dataSourceName : dataSourcesBuildTimeConfig.dataSources().keySet()) {
            for (final var key : DataSourceUtil.dataSourcePropertyKeys(dataSourceName, GenericJdbc.DRIVER_PROPERTY_RADICAL)) {
                final var driver = config.getOptionalValue(key, String.class);
                if (driver.isPresent()) {
                    // best effort for native images: the driver is a runtime choice but when the property is
                    // already visible at build time (e.g. set in application.properties) the configured class
                    // is registered for reflection; methods are needed for the XADataSource/DataSource
                    // property injection done by Agroal
                    reflectiveClass.produce(ReflectiveClassBuildItem.builder(driver.get()).methods().build());
                    break;
                }
            }
        }
    }

    @BuildStep
    AdditionalBeanBuildItem registerDriverResolver() {
        return new AdditionalBeanBuildItem.Builder()
                .addBeanClasses(GenericJdbcDriverResolver.class, GenericJdbcConnectionConfigurer.class)
                .setDefaultScope(BuiltinScope.APPLICATION.getName())
                .setUnremovable()
                .build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Produce(ServiceStartBuildItem.class)
    void eagerlyResolveDrivers(final GenericJdbcRecorder recorder) {
        // resolve and validate the drivers before the application startup logic can use the datasources;
        // GenericJdbcResolvingDriver additionally guarantees the substitution happened whenever a datasource
        // is created, whatever the runtime init ordering
        recorder.resolveDrivers();
    }
}

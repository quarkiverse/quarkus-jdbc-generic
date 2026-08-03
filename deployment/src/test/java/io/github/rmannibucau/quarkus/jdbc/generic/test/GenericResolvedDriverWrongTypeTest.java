package io.quarkiverse.jdbc.jdbc.generic.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class GenericResolvedDriverWrongTypeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "generic")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:generic-wrong-type")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc-generic.driver", "java.lang.String")
            .assertException(t -> assertThat(t)
                    .hasStackTraceContaining("java.lang.String")
                    .hasStackTraceContaining("is neither an implementation of"));

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void startupShouldFail() {
        Assertions.fail("Startup should have failed");
    }
}

/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.qualification.accounting;

import io.fireflysoftware.domain.accounting.web.DomainApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

class DomainPersistenceBoundaryTest {
    @Test void owningMainStartsWithoutABusinessConnectionFactoryOrRepositoryTemplate() {
        try (var context=new SpringApplicationBuilder(DomainApplication.class).run(
                "--server.address=127.0.0.1", "--server.port=0", "--firefly.cache.default-cache-type=CAFFEINE",
                "--firefly.eda.enabled=false", "--firefly.stepevents.enabled=false", "--logging.level.root=ERROR")) {
            assertTrue(context.getBeansOfType(ConnectionFactory.class).isEmpty());
            assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
            assertFalse(context.containsBean("r2dbcEntityTemplate"));
        }
    }
    @Test void owningMainExplicitlyExcludesBusinessStorageWhenTheClasspathChanges() {
        var application=DomainApplication.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(application);
        assertEquals(Set.of("org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration",
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"),
                Set.of(application.excludeName()));
    }
}

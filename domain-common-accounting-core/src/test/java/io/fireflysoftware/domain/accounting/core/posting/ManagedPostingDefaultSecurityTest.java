/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import static org.junit.jupiter.api.Assertions.*;
import org.fireflyframework.security.rs.ResourceServerAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.config.EnableWebFlux;
class ManagedPostingDefaultSecurityTest {
    @Configuration(proxyBeanMethods=false) @EnableWebFlux static class ProbeConfiguration {@Bean ProbeController probe(){return new ProbeController();}}
    @RestController static class ProbeController {@GetMapping("/unrelated-security-probe") String get(){return "protected";}}
    @Test void absentManagedFlagKeepsActualGlobalReceiverAndRequiresNoManagedCredentials(){new ReactiveWebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ResourceServerAutoConfiguration.class)).withUserConfiguration(ManagedPostingConfiguration.class,ProbeConfiguration.class).run(context->{assertNull(context.getStartupFailure());assertEquals(0,context.getBeanNamesForType(ManagedPostingClientFactory.class).length);assertEquals(1,context.getBeanNamesForType(org.springframework.security.web.server.SecurityWebFilterChain.class).length);WebTestClient.bindToApplicationContext(context).build().get().uri("/unrelated-security-probe").exchange().expectStatus().isUnauthorized();});}
}

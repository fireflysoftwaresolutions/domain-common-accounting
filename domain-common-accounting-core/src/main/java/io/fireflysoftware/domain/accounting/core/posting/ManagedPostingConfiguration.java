/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import io.fireflysoftware.domain.accounting.core.posting.impl.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.security.rs.ResourceServerProperties;
import org.fireflyframework.security.spi.*;
import org.fireflyframework.security.adapter.opa.OpaPolicyDecisionAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;
/** Explicit adapters preserve the framework's global default-deny receiver and use standard registrations. */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="firefly.accounting.managed-posting",name="enabled",havingValue="true")
@EnableConfigurationProperties({ManagedPostingSettings.class,ResourceServerProperties.class})
public class ManagedPostingConfiguration {
    @Bean ManagedPostingClientFactory managedPostingClients(ManagedPostingSettings settings,ResourceServerProperties trust,SecurityContextPort security,ReactiveClientRegistrationRepository registrations,ReactiveJwtDecoder decoder){
        if(!trust.isStrictPolicy()||!settings.issuer().equals(trust.getIssuer())||!List.of(settings.domainAudience()).equals(trust.getAudiences())||!trust.isTenantRequired()||!List.of(settings.tenantClaim()).equals(trust.getTenantClaimPaths())||!List.of(settings.scopeClaim()).equals(trust.getScopeClaimPaths()))throw new IllegalStateException("Exact global managed posting receiver trust required");
        return new ExchangedManagedPostingClientFactoryImpl(settings,security,registrations,decoder);
    }
    @Bean PolicyDecisionPort managedPostingProjectPolicy(ManagedPostingSettings settings){
        var connector=new JdkClientHttpConnector(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(2)).build());
        return new OpaPolicyDecisionAdapter(WebClient.builder().clientConnector(connector).baseUrl(settings.policyEndpoint().toASCIIString()).codecs(c->{c.defaultCodecs().maxInMemorySize(16384);c.defaultCodecs().enableLoggingRequestDetails(false);}).build(),settings.policyDecisionPath());
    }
    @Bean ManagedPostingExecution managedPostingExecution(ManagedPostingClientFactory clients){return new GeneratedManagedPostingExecutionImpl(clients);}
    @Bean ManagedPostingService managedPostingService(CommandBus commands){return new ManagedPostingServiceImpl(commands);}
}

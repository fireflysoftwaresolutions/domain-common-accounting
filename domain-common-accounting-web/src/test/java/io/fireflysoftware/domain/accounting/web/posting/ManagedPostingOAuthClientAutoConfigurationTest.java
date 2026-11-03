/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.web.posting;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.oauth2.client.autoconfigure.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import static org.assertj.core.api.Assertions.assertThat;

class ManagedPostingOAuthClientAutoConfigurationTest {
    private ReactiveWebApplicationContextRunner standardBootClient(){
        return new ReactiveWebApplicationContextRunner().withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientAutoConfiguration.class));
    }

    @Test void standardBootCreatesTheActualTokenExchangeRegistrationFromIssuerDiscovery()throws Exception{
        var discoveryCalls=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        String origin="http://127.0.0.1:"+server.getAddress().getPort(),issuer=origin+"/issuer";
        server.createContext("/",request->{try{
            discoveryCalls.incrementAndGet();byte[] body=("{\"issuer\":\""+issuer+"\",\"authorization_endpoint\":\""+origin+"/authorize\",\"token_endpoint\":\""+origin+"/token\",\"jwks_uri\":\""+origin+"/jwks\",\"response_types_supported\":[\"code\"],\"subject_types_supported\":[\"public\"],\"id_token_signing_alg_values_supported\":[\"RS256\"]}").getBytes(StandardCharsets.UTF_8);
            request.getResponseHeaders().set("Content-Type","application/json");request.sendResponseHeaders(200,body.length);request.getResponseBody().write(body);
        }finally{request.close();}});server.start();
        try{
            standardBootClient().withPropertyValues(
                "spring.security.oauth2.client.registration.accounting-gl-bridge.provider=accounting-gl-bridge",
                "spring.security.oauth2.client.registration.accounting-gl-bridge.client-id=accounting-gl-bridge",
                "spring.security.oauth2.client.registration.accounting-gl-bridge.client-secret="+UUID.randomUUID(),
                "spring.security.oauth2.client.registration.accounting-gl-bridge.client-authentication-method=client_secret_basic",
                "spring.security.oauth2.client.registration.accounting-gl-bridge.authorization-grant-type=urn:ietf:params:oauth:grant-type:token-exchange",
                "spring.security.oauth2.client.registration.accounting-gl-bridge.scope=accounting:gl:post,accounting:gl:recover",
                "spring.security.oauth2.client.provider.accounting-gl-bridge.issuer-uri="+issuer,
                "spring.security.oauth2.client.provider.accounting-gl-bridge.token-uri="+origin+"/token"
            ).run(context->{
                assertThat(context).hasNotFailed().hasSingleBean(ReactiveClientRegistrationRepository.class);
                var repository=context.getBean(ReactiveClientRegistrationRepository.class);
                assertThat(repository).isExactlyInstanceOf(InMemoryReactiveClientRegistrationRepository.class);
                var registration=repository.findByRegistrationId("accounting-gl-bridge").block(Duration.ofSeconds(2));
                assertThat(registration).isNotNull();assertThat(registration.getRegistrationId()).isEqualTo("accounting-gl-bridge");
                assertThat(registration.getClientId()).isEqualTo("accounting-gl-bridge");
                assertThat(registration.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.TOKEN_EXCHANGE);
                assertThat(registration.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                assertThat(registration.getProviderDetails().getIssuerUri()).isEqualTo(issuer);
                assertThat(registration.getProviderDetails().getTokenUri()).isEqualTo(origin+"/token");
                assertThat(registration.getScopes()).containsExactlyInAnyOrderElementsOf(Set.of("accounting:gl:post","accounting:gl:recover"));
                assertThat(discoveryCalls.get()).isPositive();
            });
        }finally{server.stop(0);}
    }

    @Test void noRegistrationPropertiesRequireNoCredentialsOrCustomRepository(){
        standardBootClient().run(context->assertThat(context).hasNotFailed().doesNotHaveBean(ReactiveClientRegistrationRepository.class));
    }

    @Test void explicitExistingRepositoryIsPreservedWithoutDuplicateOrDiscovery(){
        var registration=ClientRegistration.withRegistrationId("accounting-gl-bridge").clientId("accounting-gl-bridge")
            .clientSecret(UUID.randomUUID().toString()).authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE)
            .tokenUri("https://example.invalid/token").build();
        ReactiveClientRegistrationRepository existing=new InMemoryReactiveClientRegistrationRepository(registration);
        standardBootClient().withBean(ReactiveClientRegistrationRepository.class,()->existing)
            .withPropertyValues("spring.security.oauth2.client.registration.accounting-gl-bridge.provider=accounting-gl-bridge",
                "spring.security.oauth2.client.provider.accounting-gl-bridge.issuer-uri=https://unreachable.invalid/issuer")
            .run(context->{assertThat(context).hasNotFailed().hasSingleBean(ReactiveClientRegistrationRepository.class);
                assertThat(context.getBean(ReactiveClientRegistrationRepository.class)).isSameAs(existing);});
    }
}

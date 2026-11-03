/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting.impl;
import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.domain.accounting.core.posting.*;
import io.fireflysoftware.core.accounting.sdk.model.GlPostingRequest;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.UnaryOperator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.rs.*;
import org.fireflyframework.security.webflux.authentication.PrincipalSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import reactor.core.publisher.Mono;

/** Real JWKS, signed native projection, standard RFC8693 HTTP and actual generated SDK; not owner Main. */
class ExchangedManagedPostingClientFactoryTest {
    static final UUID TENANT=new UUID(0,1),BOOK=new UUID(0,2),STAFF=new UUID(0,3),ACTION=new UUID(0,4);
    static final String POST=ExchangedManagedPostingClientFactoryImpl.POST_SCOPE,RECOVER=ExchangedManagedPostingClientFactoryImpl.RECOVER_SCOPE;
    static final ObjectMapper JSON=new ObjectMapper();
    final class Fixture implements AutoCloseable {
        final RSAKey key=new RSAKeyGenerator(2048).keyID("managed-posting-controlled").generate();
        final HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        final AtomicInteger exchanges=new AtomicInteger(),owners=new AtomicInteger();
        final AtomicLong ticks=new AtomicLong();
        final AtomicReference<SecurityPrincipal> principal=new AtomicReference<>();
        final AtomicReference<Map<String,String>> form=new AtomicReference<>();
        final AtomicReference<String> targetToken=new AtomicReference<>();
        volatile UnaryOperator<Map<String,Object>> mutate=claims->claims;
        volatile Runnable onExchange=()->{};
        final Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String original;
        Fixture()throws Exception{
            server.createContext("/jwks",exchange->{try(exchange){var bytes=new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);}});
            server.createContext("/token",exchange->{try(exchange){
                exchanges.incrementAndGet();assertEquals("POST",exchange.getRequestMethod());assertEquals("Basic "+Base64.getEncoder().encodeToString("domain-accounting:synthetic-test-only".getBytes(StandardCharsets.UTF_8)),exchange.getRequestHeaders().getFirst("Authorization"));
                var fields=new LinkedHashMap<String,String>();var bytes=exchange.getRequestBody().readNBytes(16385);assertTrue(bytes.length<=16384);for(var part:new String(bytes,StandardCharsets.UTF_8).split("&")){var pair=part.split("=",2);assertNull(fields.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),URLDecoder.decode(pair.length==2?pair[1]:"",StandardCharsets.UTF_8)));}form.set(Map.copyOf(fields));
                assertEquals("urn:ietf:params:oauth:grant-type:token-exchange",fields.get("grant_type"));assertEquals(original,fields.get("subject_token"));assertEquals("core-accounting-gl",fields.get("audience"));
                try{var claims=claims("core-accounting-gl","domain-accounting",fields.get("scope"),"STAFF");claims.put("exp",Date.from(now.plusSeconds(90)));var token=sign(mutate.apply(claims));targetToken.set(token);onExchange.run();var response=JSON.writeValueAsBytes(Map.of("access_token",token,"token_type","Bearer","issued_token_type","urn:ietf:params:oauth:token-type:access_token","expires_in",30,"scope",fields.get("scope")));exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);}catch(Exception error){throw new IllegalStateException("Controlled token fixture failed",error);}
            }});
            server.createContext("/api/v1/",exchange->{try(exchange){owners.incrementAndGet();assertEquals("Bearer "+targetToken.get(),exchange.getRequestHeaders().getFirst("Authorization"));assertNotEquals("Bearer "+original,exchange.getRequestHeaders().getFirst("Authorization"));var response=JSON.writeValueAsBytes(Map.of("receiptId",new UUID(0,5).toString(),"actionId",ACTION.toString(),"postingId",new UUID(0,6).toString(),"journalSequence",1,"intentDigest","a".repeat(64),"recovered",false,"recordedAt",now.toString()));exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);}});
            server.start();original=sign(claims("domain-accounting","finance-terminal",POST+" "+RECOVER,"STAFF"));project(original);
        }
        URI base(){return URI.create("http://127.0.0.1:"+server.getAddress().getPort());}
        String issuer(){return base()+"/issuer";}
        Map<String,Object> claims(String audience,String client,String scopes,String kind){var values=new HashMap<String,Object>();values.put("iss",issuer());values.put("sub","original-finance-staff");values.put("aud",List.of(audience));values.put("tenant","opaque-bank-east");values.put("azp",client);values.put("scope",scopes);values.put("principal_kind",kind);values.put("staff_actor_id",STAFF.toString());values.put("nbf",Date.from(now.minusSeconds(1)));values.put("exp",Date.from(now.plusSeconds(120)));return values;}
        String sign(Map<String,Object> values)throws Exception{var builder=new JWTClaimsSet.Builder();for(var entry:values.entrySet()){if(entry.getKey().equals("exp"))builder.expirationTime((Date)entry.getValue());else if(entry.getKey().equals("nbf"))builder.notBeforeTime((Date)entry.getValue());else builder.claim(entry.getKey(),entry.getValue());}var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),builder.build());jwt.sign(new RSASSASigner(key));return jwt.serialize();}
        ResourceServerProperties trust(String audience){var p=new ResourceServerProperties();p.setIssuer(issuer());p.setAudiences(List.of(audience));p.setJwkSetUri(base()+"/jwks");p.setTenantClaimPaths(List.of("tenant"));p.setScopeClaimPaths(List.of("scope"));p.setTenantRequired(true);return p;}
        org.springframework.security.oauth2.jwt.ReactiveJwtDecoder decoder(String audience){return new ResourceServerAutoConfiguration().fireflyReactiveJwtDecoder(new org.fireflyframework.security.core.key.InMemoryKeyManagementAdapter(),trust(audience));}
        void project(String token)throws Exception{var p=trust("domain-accounting");var configuration=new ResourceServerAutoConfiguration();var jwt=decoder("domain-accounting").decode(token).block(Duration.ofSeconds(5));var principals=configuration.fireflyPrincipalFactory(configuration.fireflyAuthorityMappingPort(p));principal.set(PrincipalSupport.extract(configuration.fireflyJwtAuthenticationConverter(principals,p).convert(jwt).block(Duration.ofSeconds(5))));}
        ManagedPostingSettings settings(){return new ManagedPostingSettings(true,base(),issuer(),URI.create(base()+"/jwks"),"domain-accounting","core-accounting-gl","finance-terminal","domain-accounting","managed-gl","opaque-bank-east",TENANT,BOOK,"tenant","azp","principal_kind","staff_actor_id","scope",base(),"domain/allow",true);}
        ExchangedManagedPostingClientFactoryImpl factory(){var registration=ClientRegistration.withRegistrationId("managed-gl").clientId("domain-accounting").clientSecret("synthetic-test-only").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.TOKEN_EXCHANGE).tokenUri(base()+"/token").issuerUri(issuer()).scope(POST,RECOVER).build();return new ExchangedManagedPostingClientFactoryImpl(settings(),()->Mono.justOrEmpty(principal.get()),new InMemoryReactiveClientRegistrationRepository(registration),decoder("domain-accounting"),decoder("core-accounting-gl"),new JdkClientHttpConnector(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()),Clock.systemUTC(),ticks::get);}
        ManagedPostingClientFactory.Client current(){return factory().current(TENANT.toString(),BOOK.toString(),false).contextWrite(context->context.put(ManagedPostingCallerToken.class,new ManagedPostingCallerToken(original))).block(Duration.ofSeconds(8));}
        @Override public void close(){server.stop(0);}
    }
    @Test void actualSignedStaffExchangeAndGeneratedOwnerCallPreserveOriginalIdentity()throws Exception{try(var f=new Fixture()){var original=f.principal.get();var client=f.current();assertNotNull(client);assertEquals(POST,f.form.get().get("scope"));assertEquals(1,f.exchanges.get());assertSame(original,f.principal.get());var receipt=client.api().postNativeJournal(TENANT.toString(),BOOK.toString(),new GlPostingRequest().actionId(ACTION.toString())).block(Duration.ofSeconds(5));assertEquals(ACTION,receipt.getActionId());assertEquals(1,f.owners.get());client.revalidate().block();}}
    @Test void machineCannotBecomeStaffThroughExchange()throws Exception{try(var f=new Fixture()){f.original=f.sign(f.claims("domain-accounting","finance-terminal",POST,"MACHINE"));f.project(f.original);assertThrows(RuntimeException.class,f::current);assertEquals(0,f.exchanges.get());assertEquals(0,f.owners.get());}}
    @Test void targetCannotChangeCanonicalStaffIdentity()throws Exception{try(var f=new Fixture()){f.mutate=claims->{claims.put("staff_actor_id",new UUID(0,99).toString());return claims;};assertThrows(RuntimeException.class,f::current);assertEquals(1,f.exchanges.get());assertEquals(0,f.owners.get());}}
    @Test void targetCannotExpandOriginalRequestedScopes()throws Exception{try(var f=new Fixture()){f.mutate=claims->{claims.put("scope",POST+" "+RECOVER);return claims;};assertThrows(RuntimeException.class,f::current);assertEquals(0,f.owners.get());}}
    @Test void changedCallerDuringExchangeCannotAcquireOwnerClient()throws Exception{try(var f=new Fixture()){f.onExchange=()->f.principal.set(f.principal.get().toBuilder().subject("changed-subject").build());assertThrows(RuntimeException.class,f::current);assertEquals(0,f.owners.get());}}
    @Test void opaqueNamespaceDoesNotGrantAnotherResourceTenant()throws Exception{try(var f=new Fixture()){assertThrows(RuntimeException.class,()->f.factory().current(new UUID(0,99).toString(),BOOK.toString(),false).contextWrite(context->context.put(ManagedPostingCallerToken.class,new ManagedPostingCallerToken(f.original))).block());assertEquals(0,f.exchanges.get());}}
    @Test void missingOrChangedOriginalBearerCannotExchange()throws Exception{try(var f=new Fixture()){assertThrows(RuntimeException.class,()->f.factory().current(TENANT.toString(),BOOK.toString(),false).block());var changed=f.claims("domain-accounting","finance-terminal",POST,"STAFF");changed.put("sub","another-subject");f.original=f.sign(changed);assertThrows(RuntimeException.class,f::current);assertEquals(0,f.exchanges.get());}}
    @Test void targetCannotRenewOriginalExpiryOrMonotonicWindow()throws Exception{try(var f=new Fixture()){f.mutate=claims->{claims.put("exp",Date.from(f.now.plusSeconds(600)));return claims;};assertThrows(RuntimeException.class,f::current);assertEquals(0,f.owners.get());}try(var f=new Fixture()){f.onExchange=()->f.ticks.set(5_000_000_000L);assertThrows(RuntimeException.class,f::current);assertEquals(0,f.owners.get());}}
}

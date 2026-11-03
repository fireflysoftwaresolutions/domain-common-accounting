/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting.impl;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.domain.accounting.core.posting.*;
import io.fireflysoftware.core.accounting.invoker.ApiClient;
import io.fireflysoftware.core.accounting.sdk.api.NativeJournalPostingsApi;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Actual generated GL requests and controlled HTTP; no owner Main or persisted money assertion. */
class GeneratedManagedPostingExecutionTest {
    static final String TENANT=new UUID(0,1).toString(),BOOK=new UUID(0,2).toString(),ACTION=new UUID(0,3).toString(),DIGEST="a".repeat(64);
    final AtomicInteger calls=new AtomicInteger(),revalidations=new AtomicInteger();
    final AtomicBoolean current=new AtomicBoolean(true);
    final AtomicReference<ClientRequest> actual=new AtomicReference<>();
    final AtomicReference<ManagedPostingCommand> incoming=new AtomicReference<>();
    java.util.function.Function<ClientRequest,Mono<ClientResponse>> exchange=request->Mono.just(response(false));
    ManagedPostingCommand command(boolean recovery){return new ManagedPostingCommand(TENANT,BOOK,UUID.fromString(ACTION),DIGEST,"{\"actionId\":\""+ACTION+"\"}",recovery);}
    ClientResponse response(boolean recovered){return ClientResponse.create(HttpStatus.OK,strategies()).header("Content-Type","application/json").body("{\"receiptId\":\""+new UUID(0,4)+"\",\"actionId\":\""+ACTION+"\",\"postingId\":\""+new UUID(0,5)+"\",\"journalSequence\":1,\"intentDigest\":\""+DIGEST+"\",\"recovered\":"+recovered+",\"recordedAt\":\"2026-11-01T12:00:00Z\"}").build();}
    ExchangeStrategies strategies(){var mapper=ApiClient.createDefaultMapper(ApiClient.createDefaultDateFormat());return ExchangeStrategies.builder().codecs(c->c.defaultCodecs().jackson2JsonDecoder(new org.springframework.http.codec.json.Jackson2JsonDecoder(mapper,MediaType.APPLICATION_JSON))).build();}
    GeneratedManagedPostingExecutionImpl execution(){return new GeneratedManagedPostingExecutionImpl((tenant,book,recovery)->{
        assertEquals(TENANT,tenant);assertEquals(BOOK,book);
        var mapper=ApiClient.createDefaultMapper(ApiClient.createDefaultDateFormat());var web=ApiClient.buildWebClientBuilder(mapper).exchangeFunction(request->{calls.incrementAndGet();actual.set(request);return exchange.apply(request);}).build();
        var client=new ApiClient(web,mapper,ApiClient.createDefaultDateFormat()).setBasePath("https://gl.example");client.addDefaultHeader("Authorization","Bearer controlled-owner-token");
        return Mono.just(new ManagedPostingClientFactory.Client(){public NativeJournalPostingsApi api(){return new NativeJournalPostingsApi(client);}public Duration remaining(){return Duration.ofSeconds(5);}public Mono<Void> revalidate(){return Mono.defer(()->{revalidations.incrementAndGet();return current.get()?Mono.empty():Mono.error(new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN));});}});
    });}
    @Test void actualGeneratedOwnerReceiptRetainsInstantWithoutTimezoneInference()throws Exception{assertEquals(Instant.class,io.fireflysoftware.core.accounting.sdk.model.GlPostingReceipt.class.getMethod("getRecordedAt").getReturnType());}
    @Test void exactGeneratedPostReturnsOnlyBoundCommittedReceipt(){var result=execution().execute(command(false)).block();assertEquals(ManagedPostingResponse.Status.COMMITTED,result.status());assertEquals(UUID.fromString(ACTION),result.actionId());assertEquals(DIGEST,result.intentDigest());assertNotNull(result.receipt());assertEquals(1,calls.get());assertEquals(HttpMethod.POST,actual.get().method());assertEquals("/api/v1/tenants/"+TENANT+"/books/"+BOOK+"/postings",actual.get().url().getPath());assertEquals("Bearer controlled-owner-token",actual.get().headers().getFirst("Authorization"));assertFalse(actual.get().headers().containsHeader("X-Idempotency-Key"));assertTrue(revalidations.get()>=2);}
    @Test void uncertainWriteKeepsOriginalIdentityAndNeverRetries(){exchange=request->Mono.error(new java.io.IOException("controlled transport interruption"));var result=execution().execute(command(false)).block();assertEquals(ManagedPostingResponse.Status.OUTCOME_UNKNOWN,result.status());assertEquals(UUID.fromString(ACTION),result.actionId());assertEquals(DIGEST,result.intentDigest());assertNull(result.receipt());assertEquals(1,calls.get());}
    @Test void unavailableOwnerDoesNotInventNoEffectOrReplacementAction(){exchange=request->Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());var result=execution().execute(command(false)).block();assertEquals(ManagedPostingResponse.Status.OUTCOME_UNKNOWN,result.status());assertEquals(UUID.fromString(ACTION),result.actionId());assertEquals(1,calls.get());}
    @Test void knownCommitErrorMustNameTheSameOriginalAction(){exchange=request->Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).header("Content-Type","application/json").body("{\"status\":503,\"code\":\"GL_POSTING_COMMIT_KNOWN_RECOVERY_REQUIRED\",\"actionReference\":\""+ACTION+"\"}").build());var result=execution().execute(command(false)).block();assertEquals(ManagedPostingResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,result.status());assertNull(result.receipt());assertEquals(1,calls.get());}
    @Test void unrelatedCommitErrorCannotBecomeKnownOutcome(){exchange=request->Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).header("Content-Type","application/json").body("{\"status\":503,\"code\":\"GL_POSTING_COMMIT_KNOWN_RECOVERY_REQUIRED\",\"actionReference\":\""+new UUID(0,99)+"\"}").build());assertEquals(ManagedPostingResponse.Status.OUTCOME_UNKNOWN,execution().execute(command(false)).block().status());}
    @Test void recoveryUsesSameCompleteOriginalActionAndNeverPostsAgain(){exchange=request->Mono.just(response(true));var result=execution().execute(command(true)).block();assertEquals(ManagedPostingResponse.Status.COMMITTED,result.status());assertTrue(result.receipt().recovered());assertEquals(1,calls.get());assertTrue(actual.get().url().getPath().endsWith("/postings/recovery"));}
    @Test void recoveryNotObservedRemainsAnObservationWithoutNoEffectClaim(){exchange=request->Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());var result=execution().execute(command(true)).block();assertEquals(ManagedPostingResponse.Status.NOT_OBSERVED,result.status());assertEquals(404,result.ownerStatus());assertEquals(UUID.fromString(ACTION),result.actionId());assertEquals(1,calls.get());}
    @Test void explicitOwnerRefusalPreservesStatusAndOriginalIdentity(){exchange=request->Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build());var result=execution().execute(command(false)).block();assertEquals(ManagedPostingResponse.Status.CURRENT_INVOCATION_REFUSED,result.status());assertEquals(403,result.ownerStatus());assertEquals(UUID.fromString(ACTION),result.actionId());assertNull(result.receipt());}
    @Test void callerChangedAfterKnownReceiptDoesNotLoseKnownCommit(){exchange=request->{current.set(false);return Mono.just(response(false));};var result=execution().execute(command(false)).block();assertEquals(ManagedPostingResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,result.status());assertNull(result.receipt());assertEquals(UUID.fromString(ACTION),result.actionId());}
    @Test void unboundReceiptCannotBecomeCommitted(){exchange=request->Mono.just(ClientResponse.create(HttpStatus.OK,strategies()).header("Content-Type","application/json").body("{\"actionId\":\""+new UUID(0,99)+"\"}").build());assertEquals(ManagedPostingResponse.Status.OUTCOME_UNKNOWN,execution().execute(command(false)).block().status());}
    @Test void recoveryMustReturnExplicitRecoveredReceipt(){assertEquals(ManagedPostingResponse.Status.OUTCOME_UNKNOWN,execution().execute(command(true)).block().status());}
    @Test void exhaustedCallerNeverDispatchesFinancialOperation(){current.set(false);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->execution().execute(command(false)).block());assertEquals(0,calls.get());}
    @Test void stalledWriteHasFiniteUncertainOutcomeAndOneAttempt(){exchange=request->Mono.never();StepVerifier.withVirtualTime(()->execution().execute(command(false))).thenAwait(Duration.ofSeconds(6)).assertNext(result->assertEquals(ManagedPostingResponse.Status.OUTCOME_UNKNOWN,result.status())).verifyComplete();assertEquals(1,calls.get());}
}

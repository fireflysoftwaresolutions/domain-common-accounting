/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting.impl;
import io.fireflysoftware.domain.accounting.core.posting.*;
import io.fireflysoftware.core.accounting.invoker.ApiClient;
import io.fireflysoftware.core.accounting.sdk.model.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
/** One genuine generated call per subscription; Core alone persists and recovers financial effects. */
public final class GeneratedManagedPostingExecutionImpl implements ManagedPostingExecution {
    private static final ObjectMapper JSON=ApiClient.createDefaultMapper(ApiClient.createDefaultDateFormat());
    private final ManagedPostingClientFactory clients;
    public GeneratedManagedPostingExecutionImpl(ManagedPostingClientFactory clients){this.clients=Objects.requireNonNull(clients);}
    @Override public Mono<ManagedPostingResponse> execute(ManagedPostingCommand command){return Mono.defer(()->{
        Objects.requireNonNull(command);GlPostingRequest input;
        try{input=JSON.readValue(command.originalPostingJson(),GlPostingRequest.class);if(!command.actionId().toString().equals(input.getActionId()))throw new IllegalArgumentException();}
        catch(Exception invalid){return Mono.error(new ManagedPostingPreDispatchException(HttpStatus.BAD_REQUEST));}
        return Mono.defer(()->clients.current(command.tenantId(),command.bookId(),command.recovery())).switchIfEmpty(Mono.error(unavailable())).onErrorMap(GeneratedManagedPostingExecutionImpl::preDispatch)
            .flatMap(client->Mono.defer(client::revalidate).onErrorMap(GeneratedManagedPostingExecutionImpl::preDispatch).then(Mono.defer(()->dispatch(client,command,input))));
    });}
    private Mono<ManagedPostingResponse> dispatch(ManagedPostingClientFactory.Client client,ManagedPostingCommand command,GlPostingRequest input){
        Supplier<Mono<ResponseEntity<GlPostingReceipt>>> call=()->command.recovery()?client.api().recoverNativeJournalWithHttpInfo(command.tenantId(),command.bookId(),input):client.api().postNativeJournalWithHttpInfo(command.tenantId(),command.bookId(),input);
        return Mono.defer(call).timeout(client.remaining()).map(reply->{
            if(reply.getStatusCode().value()!=200||!matches(command,reply.getBody()))return outcome(command,ManagedPostingResponse.Status.OUTCOME_UNKNOWN,null,null);
            return outcome(command,ManagedPostingResponse.Status.COMMITTED,200,reply.getBody());
        }).switchIfEmpty(Mono.fromSupplier(()->outcome(command,ManagedPostingResponse.Status.OUTCOME_UNKNOWN,null,null)))
            .onErrorResume(error->Mono.just(failure(command,error))).flatMap(value->{
                if(value.status()!=ManagedPostingResponse.Status.COMMITTED)return Mono.just(value);
                return Mono.defer(client::revalidate).thenReturn(value).onErrorReturn(outcome(command,ManagedPostingResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,null,null));
            });
    }
    private static boolean matches(ManagedPostingCommand command,GlPostingReceipt receipt){return receipt!=null&&command.actionId().equals(receipt.getActionId())&&command.expectedIntentDigest().equals(receipt.getIntentDigest())
        &&receipt.getReceiptId()!=null&&!receipt.getReceiptId().equals(new UUID(0,0))&&receipt.getPostingId()!=null&&!receipt.getPostingId().equals(new UUID(0,0))
        &&receipt.getJournalSequence()!=null&&receipt.getJournalSequence()>0&&receipt.getRecordedAt()!=null&&receipt.getRecovered()!=null&&(!command.recovery()||receipt.getRecovered());}
    private static ManagedPostingResponse failure(ManagedPostingCommand command,Throwable error){
        if(error instanceof WebClientResponseException response){
            int status=response.getStatusCode().value();
            if(status==503&&boundCommitKnown(command,response.getResponseBodyAsByteArray()))return outcome(command,ManagedPostingResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,status,null);
            if(command.recovery()&&status==404)return outcome(command,ManagedPostingResponse.Status.NOT_OBSERVED,status,null);
            if(status>=400&&status<500&&status!=408&&status!=429)return outcome(command,ManagedPostingResponse.Status.CURRENT_INVOCATION_REFUSED,status,null);
        }
        return outcome(command,ManagedPostingResponse.Status.OUTCOME_UNKNOWN,null,null);
    }
    private static boolean boundCommitKnown(ManagedPostingCommand command,byte[] body){
        if(body.length==0||body.length>65536)return false;
        try{var value=JSON.reader().with(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(body);
            var names=new HashSet<String>();value.fieldNames().forEachRemaining(names::add);
            return names.equals(Set.of("status","code","actionReference"))&&value.path("status").isIntegralNumber()&&value.path("status").intValue()==503
                &&"GL_POSTING_COMMIT_KNOWN_RECOVERY_REQUIRED".equals(value.path("code").textValue())&&command.actionId().toString().equals(value.path("actionReference").textValue());
        }catch(Exception invalid){return false;}
    }
    private static ManagedPostingResponse outcome(ManagedPostingCommand command,ManagedPostingResponse.Status status,Integer ownerStatus,GlPostingReceipt receipt){return new ManagedPostingResponse(command.actionId(),command.expectedIntentDigest(),status,ownerStatus,receipt==null?null:new ManagedPostingReceipt(receipt.getReceiptId(),receipt.getActionId(),receipt.getPostingId(),receipt.getJournalSequence(),receipt.getIntentDigest(),receipt.getRecovered(),receipt.getRecordedAt()));}
    private static Throwable preDispatch(Throwable error){return error instanceof ManagedPostingPreDispatchException?error:new ManagedPostingPreDispatchException(error instanceof ResponseStatusException known?known.getStatusCode():HttpStatus.SERVICE_UNAVAILABLE);}
    private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configured current managed posting client required");}
}

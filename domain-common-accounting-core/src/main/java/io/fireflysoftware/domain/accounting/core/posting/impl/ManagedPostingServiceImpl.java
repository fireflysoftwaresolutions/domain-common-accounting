/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting.impl;
import io.fireflysoftware.domain.accounting.core.posting.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import io.fireflysoftware.core.accounting.invoker.ApiClient;
import io.fireflysoftware.core.accounting.sdk.model.GlPostingRequest;
import java.util.*;
import org.fireflyframework.cqrs.command.CommandBus;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
/** Strict business boundary for the first supported USD Ledger transfer; no local financial state. */
public final class ManagedPostingServiceImpl implements ManagedPostingService {
    private static final ObjectMapper JSON=ApiClient.createDefaultMapper(ApiClient.createDefaultDateFormat())
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS,DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    private final CommandBus commands;
    public ManagedPostingServiceImpl(CommandBus commands){this.commands=Objects.requireNonNull(commands);}
    @Override public Mono<ManagedPostingResponse> submit(String tenant,String book,byte[] completeRequest,boolean recovery){
        final ManagedPostingCommand captured;
        try{
            if(completeRequest==null||completeRequest.length==0||completeRequest.length>1048576)throw new IllegalArgumentException();
            var root=JSON.readTree(completeRequest);var names=new HashSet<String>();root.fieldNames().forEachRemaining(names::add);if(!root.isObject()||!names.equals(Set.of("expectedIntentDigest","posting")))throw new IllegalArgumentException();
            var digest=root.path("expectedIntentDigest");if(!digest.isTextual()||!digest.textValue().matches("[0-9a-f]{64}"))throw new IllegalArgumentException();
            var posting=JSON.treeToValue(root.path("posting"),GlPostingRequest.class);supported(posting);
            captured=new ManagedPostingCommand(tenant,book,UUID.fromString(posting.getActionId()),digest.textValue(),JSON.writeValueAsString(posting),recovery);
        }catch(Exception invalid){return Mono.error(new ManagedPostingPreDispatchException(HttpStatus.BAD_REQUEST));}
        return Mono.defer(()->commands.<ManagedPostingResponse>send(captured)).switchIfEmpty(Mono.fromSupplier(()->unknown(captured))).onErrorResume(error->{
            Throwable at=error;for(int depth=0;at!=null&&depth<8;depth++,at=at.getCause())if(at instanceof ManagedPostingPreDispatchException)return Mono.error(at);
            return Mono.just(unknown(captured));
        });
    }
    private static ManagedPostingResponse unknown(ManagedPostingCommand command){return new ManagedPostingResponse(command.actionId(),command.expectedIntentDigest(),ManagedPostingResponse.Status.OUTCOME_UNKNOWN,null,null);}
    private static void supported(GlPostingRequest posting){
        if(posting==null||posting.getSource()==null||!"LEDGER".equals(posting.getSource().getOwner())||!"COMMIT".equals(posting.getSource().getEventKind())||!Long.valueOf(1).equals(posting.getSource().getRevision())||!Integer.valueOf(0).equals(posting.getSource().getEffectOrdinal())
                ||posting.getEntries()==null||posting.getEntries().size()!=1||posting.getEntries().getFirst().getLines()==null||posting.getEntries().getFirst().getLines().size()!=2)throw new IllegalArgumentException();
        for(var line:posting.getEntries().getFirst().getLines())if(line.getCurrency()==null||!"USD".equals(line.getCurrency().getIsoCode())||!Integer.valueOf(2).equals(line.getPostedScale()))throw new IllegalArgumentException();
    }
}

/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.web.posting;
import io.fireflysoftware.domain.accounting.core.posting.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/books/{bookId}/ledger-journals")
@ConditionalOnProperty(prefix="firefly.accounting.managed-posting",name="enabled",havingValue="true")
@Tag(name="ManagedLedgerJournals",description="First supported USD Ledger transfer; exact original action and protected Core recovery")
@SecurityScheme(name="accountingDomainBearer",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
@SecurityRequirement(name="accountingDomainBearer")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",content=@Content(mediaType="application/json",schema=@Schema(implementation=ManagedPostingResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400",description="Application pre-dispatch rejection or correlated owner observation; authentication filters can reject before an application body",content=@Content(mediaType="application/json",schema=@Schema(oneOf={ManagedPostingResponse.class,ManagedPostingPreDispatchError.class}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Application pre-dispatch rejection or correlated owner observation; authentication filters can reject before an application body",content=@Content(mediaType="application/json",schema=@Schema(oneOf={ManagedPostingResponse.class,ManagedPostingPreDispatchError.class}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Application pre-dispatch rejection or correlated owner observation; authentication filters can reject before an application body",content=@Content(mediaType="application/json",schema=@Schema(oneOf={ManagedPostingResponse.class,ManagedPostingPreDispatchError.class}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Application pre-dispatch rejection or correlated owner observation; authentication filters can reject before an application body",content=@Content(mediaType="application/json",schema=@Schema(oneOf={ManagedPostingResponse.class,ManagedPostingPreDispatchError.class}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409",description="Application pre-dispatch rejection or correlated owner observation; authentication filters can reject before an application body",content=@Content(mediaType="application/json",schema=@Schema(oneOf={ManagedPostingResponse.class,ManagedPostingPreDispatchError.class}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="422",description="Application pre-dispatch rejection or correlated owner observation; authentication filters can reject before an application body",content=@Content(mediaType="application/json",schema=@Schema(oneOf={ManagedPostingResponse.class,ManagedPostingPreDispatchError.class}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503",description="Application pre-dispatch rejection or correlated owner observation; authentication filters can reject before an application body",content=@Content(mediaType="application/json",schema=@Schema(oneOf={ManagedPostingResponse.class,ManagedPostingPreDispatchError.class}))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="4XX",description="Other exact owner current-invocation refusal status; prior financial effects remain unknown",content=@Content(mediaType="application/json",schema=@Schema(implementation=ManagedPostingResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="default",description="Transport/framework failure; retain the complete original action for protected Core recovery")
})
public class ManagedLedgerPostingsController {
    private final ManagedPostingService postings;
    public ManagedLedgerPostingsController(ManagedPostingService postings){this.postings=postings;}
    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    @DisableIdempotency
    @Operation(operationId="postLedgerJournal",summary="Submit the exact approved original transfer journal through Domain",
        description="Core independently observes actual current Ledger monetary evidence and approved mapping. Uncertain responses retain the original action and never authorize replacement or automatic compensation.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(mediaType="application/json",schema=@Schema(implementation=ManagedPostingRequest.class)))
    public Mono<ResponseEntity<ManagedPostingResponse>> post(@PathVariable String tenantId,@PathVariable String bookId,ServerHttpRequest request){return invoke(tenantId,bookId,request,false);}
    @PostMapping(value="/recovery",consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    @DisableIdempotency
    @Operation(operationId="recoverLedgerJournal",summary="Recover the same original Core journal without posting again",
        description="A missing observation is not an audited no-effect closure or permission to allocate another action.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(mediaType="application/json",schema=@Schema(implementation=ManagedPostingRequest.class)))
    public Mono<ResponseEntity<ManagedPostingResponse>> recover(@PathVariable String tenantId,@PathVariable String bookId,ServerHttpRequest request){return invoke(tenantId,bookId,request,true);}
    private Mono<ResponseEntity<ManagedPostingResponse>> invoke(String tenant,String book,ServerHttpRequest request,boolean recovery){
        var values=request.getHeaders().get(HttpHeaders.AUTHORIZATION);if(values==null||values.size()!=1||!values.getFirst().startsWith("Bearer "))return Mono.error(new ManagedPostingPreDispatchException(HttpStatus.UNAUTHORIZED));
        final ManagedPostingCallerToken token;try{token=new ManagedPostingCallerToken(values.getFirst().substring(7));}catch(IllegalArgumentException invalid){return Mono.error(new ManagedPostingPreDispatchException(HttpStatus.UNAUTHORIZED));}
        return DataBufferUtils.join(request.getBody(),1048576).switchIfEmpty(Mono.error(new ManagedPostingPreDispatchException(HttpStatus.BAD_REQUEST))).onErrorMap(error->new ManagedPostingPreDispatchException(HttpStatus.BAD_REQUEST))
            .map(buffer->{try{byte[] body=new byte[buffer.readableByteCount()];buffer.read(body);return body;}finally{DataBufferUtils.release(buffer);}})
            .flatMap(body->postings.submit(tenant,book,body,recovery)).map(value->{
                int status=switch(value.status()){case COMMITTED->200;case NOT_OBSERVED->404;case CURRENT_INVOCATION_REFUSED->value.ownerStatus();default->503;};
                return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore()).body(value);
            }).onErrorMap(DataBufferLimitException.class,error->new ManagedPostingPreDispatchException(HttpStatus.BAD_REQUEST)).contextWrite(context->context.put(ManagedPostingCallerToken.class,token));
    }
}

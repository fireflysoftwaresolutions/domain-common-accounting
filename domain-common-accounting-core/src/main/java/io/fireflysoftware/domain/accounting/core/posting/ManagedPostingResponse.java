/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
/** No failure or missing observation is a no-effect closure or permission to replace the action. */
@Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public record ManagedPostingResponse(UUID actionId,String intentDigest,Status status,Integer ownerStatus,ManagedPostingReceipt receipt){
    public enum Status {COMMITTED,OUTCOME_UNKNOWN,COMMITTED_RESPONSE_UNAVAILABLE,CURRENT_INVOCATION_REFUSED,NOT_OBSERVED}
}

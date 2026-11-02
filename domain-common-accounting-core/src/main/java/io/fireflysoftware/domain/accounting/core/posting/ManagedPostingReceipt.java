/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
/** Exact observed owner receipt projection; never constructed from a client claim. */
@Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public record ManagedPostingReceipt(UUID receiptId,UUID actionId,UUID postingId,long journalSequence,String intentDigest,boolean recovered,
        @Schema(ref="#/components/schemas/FireflyInstant") Instant recordedAt) {}

/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import io.fireflysoftware.core.accounting.sdk.model.GlPostingRequest;
import io.swagger.v3.oas.annotations.media.Schema;
/** Original approved intent expectation only; Core independently verifies its actual authorities and receipt. */
@Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public record ManagedPostingRequest(@Schema(requiredMode=Schema.RequiredMode.REQUIRED,pattern="[0-9a-f]{64}") String expectedIntentDigest,
        @Schema(requiredMode=Schema.RequiredMode.REQUIRED) GlPostingRequest posting) {}

/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public record ManagedPostingPreDispatchError(int status,@Schema(allowableValues={"MANAGED_POSTING_PRE_DISPATCH_REJECTED"}) String code,@Schema(allowableValues={"PRE_DISPATCH"}) String phase){}

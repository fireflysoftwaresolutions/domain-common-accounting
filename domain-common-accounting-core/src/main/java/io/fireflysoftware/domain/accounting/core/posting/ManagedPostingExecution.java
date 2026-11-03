/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import reactor.core.publisher.Mono;
public interface ManagedPostingExecution {Mono<ManagedPostingResponse> execute(ManagedPostingCommand command);}

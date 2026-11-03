/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import io.fireflysoftware.core.accounting.sdk.api.NativeJournalPostingsApi;
import java.time.Duration;
import reactor.core.publisher.Mono;
/** A fresh original-caller-bound generated owner client; no implicit machine grant or authority fallback. */
public interface ManagedPostingClientFactory {
    Mono<Client> current(String tenantId,String bookId,boolean recovery);
    interface Client {NativeJournalPostingsApi api();Duration remaining();Mono<Void> revalidate();}
}

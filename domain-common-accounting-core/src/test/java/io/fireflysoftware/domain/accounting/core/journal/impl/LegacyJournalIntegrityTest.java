/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.journal.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.fireflysoftware.core.accounting.sdk.api.*;
import io.fireflysoftware.core.accounting.sdk.model.*;
import io.fireflysoftware.domain.accounting.core.journal.InterestJournal;
import io.fireflysoftware.domain.accounting.core.journal.commands.RemoveInterestCommand;
import io.fireflysoftware.domain.accounting.core.journal.workflows.PostInterestSaga;
import org.fireflyframework.cqrs.command.CommandBus;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;

class LegacyJournalIntegrityTest {
    private final SandboxAdapterImpl adapter = new SandboxAdapterImpl();

    @Test
    void unconfiguredAccountReadCannotFabricateAnAccount() {
        refused(adapter.read(UUID.randomUUID()), 503);
    }

    @Test
    void localInterestPostCannotReturnAReceiptWithoutTheOwner() {
        refused(adapter.postInterest(LocalDate.of(2026, 11, 2), new BigDecimal("2.19"),
                "EUR", "retained-original-reference"), 422);
    }

    @Test
    void localDateLookupCannotAssertThatNoJournalExists() {
        refused(adapter.findInterest(LocalDate.of(2026, 11, 2)), 422);
    }

    @Test
    void localRemovalCannotAssertThatAStoredJournalWasReversed() {
        refused(adapter.removeInterest(UUID.randomUUID()), 409);
    }

    @Test
    void sdkCannotCreateAnUnapprovedJournalThroughSeparateCrudCalls() {
        var accounts = mock(GlAccountsApi.class);
        var batches = mock(JournalBatchesApi.class);
        var entries = mock(JournalEntriesApi.class);
        var lines = mock(JournalLinesApi.class);
        when(accounts.getById3(any(), any())).thenReturn(Mono.just(new GlAccountDTO()));
        var page = mock(PaginationResponseJournalBatchDTO.class);
        when(page.getContent()).thenReturn(List.of());
        when(batches.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(page));
        when(batches.create(any(), any())).thenReturn(Mono.just(new JournalBatchDTO(UUID.randomUUID())));
        var entry = mock(JournalEntryDTO.class);
        when(entry.getId()).thenReturn(UUID.randomUUID());
        when(entries.create1(any(), any(), any())).thenReturn(Mono.just(entry));
        when(lines.create2(any(), any(), any(), any())).thenReturn(Mono.just(new JournalLineDTO()));
        var live = new SdkAdapterImpl(accounts, batches, entries, lines, UUID.randomUUID(), UUID.randomUUID());
        refused(live.postInterest(LocalDate.of(2026, 11, 2), new BigDecimal("2.19"), "EUR", "original"), 422);
        verifyNoInteractions(accounts, batches, entries, lines);
    }

    @Test
    void sdkCannotDeleteAPostedBatchAsCompensation() {
        var accounts = mock(GlAccountsApi.class);
        var batches = mock(JournalBatchesApi.class);
        when(batches.delete(any(), any())).thenReturn(Mono.empty());
        var live = new SdkAdapterImpl(accounts, batches, null, null, UUID.randomUUID(), UUID.randomUUID());
        refused(live.removeInterest(UUID.randomUUID()), 409);
        verifyNoInteractions(accounts, batches);
    }

    @Test
    void sagaCannotDispatchDeletionForAPostedReceipt() {
        var bus = mock(CommandBus.class);
        when(bus.send(any(RemoveInterestCommand.class))).thenReturn(Mono.empty());
        var receipt = new InterestJournal(UUID.randomUUID(), LocalDate.of(2026, 11, 2), new BigDecimal("2.19"), "EUR");
        refused(new PostInterestSaga(bus).remove(receipt), 409);
        verifyNoInteractions(bus);
    }

    static void refused(Mono<?> operation, int status) {
        StepVerifier.create(operation).expectErrorMatches(error -> error instanceof ResponseStatusException refusal
                && refusal.getStatusCode().value() == status).verify();
    }
}

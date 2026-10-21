/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.domain.accounting.core.journal;

import io.fireflysoftware.core.accounting.sdk.api.GlAccountsApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalBatchesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalEntriesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalLinesApi;
import io.fireflysoftware.core.accounting.sdk.model.GlAccountDTO;
import io.fireflysoftware.core.accounting.sdk.model.JournalBatchDTO;
import io.fireflysoftware.core.accounting.sdk.model.JournalEntryDTO;
import io.fireflysoftware.core.accounting.sdk.model.JournalLineDTO;
import io.fireflysoftware.core.accounting.sdk.model.PaginationResponseJournalBatchDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SdkAdapterTest {
    @Test
    void sandboxIsTheDefaultAndDoesNotOpenASocket() {
        new ApplicationContextRunner()
                .withUserConfiguration(AdapterConfiguration.class)
                .run(context -> {
                    CorePort port = context.getBean(CorePort.class);
                    assertThat(port).isInstanceOf(SandboxAdapter.class);
                    assertThat(port.read(null).block().label()).isEqualTo("1000");
                });
    }

    @Test
    void corePropertySelectsTheSdkAdapter() {
        new ApplicationContextRunner()
                .withUserConfiguration(AdapterConfiguration.class)
                .withPropertyValues("firefly.accounting.gl.adapter=core")
                .run(context -> assertThat(context.getBean(CorePort.class)).isInstanceOf(SdkAdapter.class));
    }

    @Test
    void liveReadCallsTheCoreSdk() {
        UUID id = UUID.randomUUID();
        GlAccountsApi api = mock(GlAccountsApi.class);
        GlAccountDTO dto = mock(GlAccountDTO.class);
        when(dto.getId()).thenReturn(id);
        when(api.getById3(id, null)).thenReturn(Mono.just(dto));
        LedgerAccountView view = new SdkAdapter(api).read(id).block();
        assertThat(view.id()).isEqualTo(id);
        verify(api).getById3(id, null);
    }

    @Test
    void sandboxJournalIsIdempotentForTheSameBusinessDate() {
        SandboxAdapter sandbox = new SandboxAdapter();
        LocalDate date = LocalDate.of(2026, 10, 21);
        InterestJournal first = sandbox.postInterest(date, new BigDecimal("2.19"), "EUR", "LOAN-INTEREST-2026-10-21").block();
        InterestJournal second = sandbox.postInterest(date, new BigDecimal("2.19"), "EUR", "LOAN-INTEREST-2026-10-21").block();
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(sandbox.findInterest(date).block().amount()).isEqualByComparingTo("2.19");
        assertThat(sandbox.findInterest(date).block().businessDate()).isEqualTo(date);
    }

    @Test
    void liveJournalPostsOnceThroughTheGeneralLedgerSdk() {
        UUID receivable = UUID.fromString("00000000-0000-4000-8000-000000000011");
        UUID income = UUID.fromString("00000000-0000-4000-8000-000000000012");
        UUID batchId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 21);
        GlAccountsApi accounts = mock(GlAccountsApi.class);
        JournalBatchesApi batches = mock(JournalBatchesApi.class);
        JournalEntriesApi entries = mock(JournalEntriesApi.class);
        JournalLinesApi lines = mock(JournalLinesApi.class);
        when(accounts.getById3(any(), any())).thenReturn(Mono.just(new GlAccountDTO()));
        PaginationResponseJournalBatchDTO empty = mock(PaginationResponseJournalBatchDTO.class);
        when(empty.getContent()).thenReturn(List.of());
        when(batches.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(empty));
        JournalBatchDTO created = new JournalBatchDTO(batchId);
        when(batches.create(any(), any())).thenReturn(Mono.just(created));
        JournalEntryDTO entry = mock(JournalEntryDTO.class);
        when(entry.getId()).thenReturn(entryId);
        when(entries.create1(any(), any(), any())).thenReturn(Mono.just(entry));
        when(lines.create2(any(), any(), any(), any())).thenReturn(Mono.just(new JournalLineDTO()));

        SdkAdapter adapter = new SdkAdapter(accounts, batches, entries, lines, receivable, income);
        InterestJournal first = adapter.postInterest(date, new BigDecimal("2.19"), "EUR", "LOAN-INTEREST-2026-10-21").block();
        InterestJournal second = adapter.postInterest(date, new BigDecimal("2.19"), "EUR", "LOAN-INTEREST-2026-10-21").block();

        assertThat(first.id()).isEqualTo(batchId);
        assertThat(first.businessDate()).isEqualTo(date);
        assertThat(first.amount()).isEqualByComparingTo("2.19");
        assertThat(second.id()).isEqualTo(first.id());
        verify(batches, times(1)).create(any(), any());
        verify(accounts, times(1)).getById3(receivable, "LOAN-INTEREST-2026-10-21");
        verify(accounts, times(1)).getById3(income, "LOAN-INTEREST-2026-10-21");
    }
}

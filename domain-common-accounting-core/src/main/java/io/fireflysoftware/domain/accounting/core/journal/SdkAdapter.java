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

import io.fireflysoftware.core.accounting.invoker.ApiClient;
import io.fireflysoftware.core.accounting.sdk.api.GlAccountsApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalBatchesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalEntriesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalLinesApi;
import io.fireflysoftware.core.accounting.sdk.model.JournalBatchDTO;
import io.fireflysoftware.core.accounting.sdk.model.JournalEntryDTO;
import io.fireflysoftware.core.accounting.sdk.model.JournalLineDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Live adapter. Calls core-common-accounting-gl-sdk. Selected with firefly.accounting.gl.adapter=core. */
public final class SdkAdapter implements CorePort {
    private static final UUID DEFAULT_RECEIVABLE = UUID.fromString("00000000-0000-4000-8000-000000000011");
    private static final UUID DEFAULT_INCOME = UUID.fromString("00000000-0000-4000-8000-000000000012");

    private final GlAccountsApi accounts;
    private final JournalBatchesApi batches;
    private final JournalEntriesApi entries;
    private final JournalLinesApi lines;
    private final UUID receivableAccountId;
    private final UUID incomeAccountId;
    private final ConcurrentHashMap<LocalDate, InterestJournal> posted = new ConcurrentHashMap<>();

    public SdkAdapter(String basePath) {
        this(basePath, DEFAULT_RECEIVABLE, DEFAULT_INCOME);
    }

    public SdkAdapter(String basePath, UUID receivableAccountId, UUID incomeAccountId) {
        ApiClient client = new ApiClient();
        client.setBasePath(basePath);
        this.accounts = new GlAccountsApi(client);
        this.batches = new JournalBatchesApi(client);
        this.entries = new JournalEntriesApi(client);
        this.lines = new JournalLinesApi(client);
        this.receivableAccountId = receivableAccountId;
        this.incomeAccountId = incomeAccountId;
    }

    SdkAdapter(GlAccountsApi accounts) {
        this.accounts = accounts;
        this.batches = null;
        this.entries = null;
        this.lines = null;
        this.receivableAccountId = DEFAULT_RECEIVABLE;
        this.incomeAccountId = DEFAULT_INCOME;
    }

    SdkAdapter(
            GlAccountsApi accounts,
            JournalBatchesApi batches,
            JournalEntriesApi entries,
            JournalLinesApi lines,
            UUID receivableAccountId,
            UUID incomeAccountId) {
        this.accounts = accounts;
        this.batches = batches;
        this.entries = entries;
        this.lines = lines;
        this.receivableAccountId = receivableAccountId;
        this.incomeAccountId = incomeAccountId;
    }

    @Override
    public Mono<LedgerAccountView> read(UUID id) {
        return accounts.getById3(id, null).map(dto -> new LedgerAccountView(dto.getId(), dto.getCode()));
    }

    @Override
    public Mono<InterestJournal> postInterest(LocalDate businessDate, BigDecimal amount, String currency, String reference) {
        if (businessDate == null || amount == null || amount.signum() <= 0 || currency == null || !currency.matches("[A-Z]{3}")) {
            return Mono.error(new IllegalArgumentException("Business date, a positive amount, and a 3-letter currency are required"));
        }
        InterestJournal cached = posted.get(businessDate);
        if (cached != null) {
            return Mono.just(cached);
        }
        String key = reference == null || reference.isBlank() ? "LOAN-INTEREST-" + businessDate : reference;
        BigDecimal postedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        return findRemote(businessDate, key, postedAmount, currency)
                .switchIfEmpty(Mono.defer(() -> create(businessDate, postedAmount, currency, key)))
                .doOnNext(journal -> posted.put(businessDate, journal));
    }

    @Override
    public Mono<InterestJournal> findInterest(LocalDate businessDate) {
        InterestJournal cached = posted.get(businessDate);
        if (cached != null) {
            return Mono.just(cached);
        }
        if (batches == null || businessDate == null) {
            return Mono.empty();
        }
        return findRemote(businessDate, "LOAN-INTEREST-" + businessDate, null, null);
    }

    @Override
    public Mono<Void> removeInterest(UUID journalId) {
        posted.entrySet().removeIf(entry -> journalId != null && journalId.equals(entry.getValue().id()));
        if (journalId == null || batches == null) {
            return Mono.empty();
        }
        return batches.delete(journalId, journalId.toString()).then();
    }

    private Mono<InterestJournal> findRemote(LocalDate businessDate, String reference, BigDecimal amount, String currency) {
        if (batches == null) {
            return Mono.empty();
        }
        return batches.search(0, 100, null, null, null, null, reference)
                .flatMap(page -> {
                    if (page == null || page.getContent() == null) {
                        return Mono.empty();
                    }
                    return Flux.fromIterable(page.getContent())
                            .filter(batch -> reference.equals(batch.getReference()) || businessDate.equals(batch.getBatchDate()))
                            .next()
                            .map(batch -> new InterestJournal(
                                    batch.getId(),
                                    batch.getBatchDate() == null ? businessDate : batch.getBatchDate(),
                                    amount,
                                    currency));
                });
    }

    private Mono<InterestJournal> create(LocalDate businessDate, BigDecimal amount, String currency, String reference) {
        JournalBatchDTO batch = new JournalBatchDTO()
                .reference(reference)
                .status(JournalBatchDTO.StatusEnum.POSTED)
                .batchDate(businessDate)
                .remarks("Loan interest accrual");
        return accounts.getById3(receivableAccountId, reference)
                .then(accounts.getById3(incomeAccountId, reference))
                .then(batches.create(batch, reference))
                .flatMap(created -> {
                    JournalEntryDTO entry = new JournalEntryDTO()
                            .transactionId(reference)
                            .sourceModule("LENDING")
                            .totalAmount(amount)
                            .description("Loan interest accrued")
                            .entryDate(businessDate.atStartOfDay())
                            .posted(true);
                    return entries.create1(created.getId(), entry, reference)
                            .flatMap(saved -> lines.create2(created.getId(), saved.getId(),
                                            line(receivableAccountId, amount, currency, JournalLineDTO.DrCrEnum.DR, "Interest receivable"),
                                            reference)
                                    .then(lines.create2(created.getId(), saved.getId(),
                                            line(incomeAccountId, amount, currency, JournalLineDTO.DrCrEnum.CR, "Interest income"),
                                            reference)))
                            .thenReturn(new InterestJournal(created.getId(), businessDate, amount, currency));
                });
    }

    private static JournalLineDTO line(
            UUID accountId, BigDecimal amount, String currency, JournalLineDTO.DrCrEnum side, String memo) {
        return new JournalLineDTO()
                .accountId(accountId)
                .amount(amount)
                .drCr(side)
                .fxCurrency(currency)
                .memo(memo);
    }
}

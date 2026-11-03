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

package io.fireflysoftware.domain.accounting.core.journal.impl;

import io.fireflysoftware.domain.accounting.core.journal.CorePort;
import io.fireflysoftware.domain.accounting.core.journal.InterestJournal;
import io.fireflysoftware.domain.accounting.core.journal.LedgerAccountView;
import io.fireflysoftware.core.accounting.invoker.ApiClient;
import io.fireflysoftware.core.accounting.sdk.api.GlAccountsApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalBatchesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalEntriesApi;
import io.fireflysoftware.core.accounting.sdk.api.JournalLinesApi;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Core account reads only; the legacy date-only interface cannot represent a governed interest posting. */
public final class SdkAdapterImpl implements CorePort {
    private final GlAccountsApi accounts;

    public SdkAdapterImpl(String basePath) {
        ApiClient client = new ApiClient();
        client.setBasePath(basePath);
        this.accounts = new GlAccountsApi(client);
    }

    @Deprecated
    public SdkAdapterImpl(String basePath, UUID receivableAccountId, UUID incomeAccountId) {
        this(basePath);
    }

    SdkAdapterImpl(GlAccountsApi accounts) {
        this.accounts = accounts;
    }

    SdkAdapterImpl(GlAccountsApi accounts, JournalBatchesApi batches, JournalEntriesApi entries,
                   JournalLinesApi lines, UUID receivableAccountId, UUID incomeAccountId) {
        this(accounts);
    }

    @Override
    public Mono<LedgerAccountView> read(UUID id) {
        return accounts.getById3(id, null).map(dto -> new LedgerAccountView(dto.getId(), dto.getCode()));
    }

    @Override
    public Mono<InterestJournal> postInterest(LocalDate businessDate, BigDecimal amount, String currency, String reference) {
        return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "GL_INTEREST_ORIGINAL_OWNER_INTENT_REQUIRED"));
    }

    @Override
    public Mono<InterestJournal> findInterest(LocalDate businessDate) {
        return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "GL_INTEREST_ORIGINAL_OWNER_ACTION_REQUIRED"));
    }

    @Override
    public Mono<Void> removeInterest(UUID journalId) {
        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "GL_POSTED_JOURNAL_REQUIRES_REVERSAL"));
    }
}

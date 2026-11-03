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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Legacy configuration compatibility; no local account or financial effect is authoritative. */
@Deprecated
public final class SandboxAdapterImpl implements CorePort {
    @Override
    public Mono<LedgerAccountView> read(UUID id) {
        return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GL_OWNER_NOT_CONFIGURED"));
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

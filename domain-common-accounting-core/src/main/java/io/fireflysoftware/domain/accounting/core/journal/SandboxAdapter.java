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

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Default adapter. Returns one sample row, posts one journal per business date, and does not call the core. */
public final class SandboxAdapter implements CorePort {
    private final ConcurrentHashMap<LocalDate, InterestJournal> journals = new ConcurrentHashMap<>();

    @Override
    public Mono<LedgerAccountView> read(UUID id) {
        UUID resolved = id == null ? UUID.fromString("00000000-0000-4000-8000-000000000001") : id;
        return Mono.just(new LedgerAccountView(resolved, "1000"));
    }

    @Override
    public Mono<InterestJournal> postInterest(LocalDate businessDate, BigDecimal amount, String currency, String reference) {
        if (businessDate == null) {
            return Mono.error(new IllegalArgumentException("Business date is required"));
        }
        if (amount == null || amount.signum() <= 0) {
            return Mono.error(new IllegalArgumentException("Interest amount must be positive"));
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            return Mono.error(new IllegalArgumentException("Currency must be a 3-letter code"));
        }
        InterestJournal existing = journals.get(businessDate);
        if (existing != null) {
            return Mono.just(existing);
        }
        String key = reference == null || reference.isBlank() ? "LOAN-INTEREST-" + businessDate : reference;
        UUID id = UUID.nameUUIDFromBytes((key + ":" + currency).getBytes(StandardCharsets.UTF_8));
        InterestJournal created = new InterestJournal(id, businessDate, amount.setScale(2, RoundingMode.HALF_UP), currency);
        InterestJournal raced = journals.putIfAbsent(businessDate, created);
        return Mono.just(raced == null ? created : raced);
    }

    @Override
    public Mono<Void> removeInterest(UUID journalId) {
        if (journalId != null) {
            journals.entrySet().removeIf(entry -> journalId.equals(entry.getValue().id()));
        }
        return Mono.empty();
    }

    @Override
    public Mono<InterestJournal> findInterest(LocalDate businessDate) {
        InterestJournal journal = journals.get(businessDate);
        return journal == null ? Mono.empty() : Mono.just(journal);
    }
}

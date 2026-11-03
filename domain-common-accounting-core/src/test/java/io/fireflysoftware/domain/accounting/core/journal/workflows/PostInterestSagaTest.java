/*
 * Copyright 2026 Firefly Software Foundation
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

package io.fireflysoftware.domain.accounting.core.journal.workflows;

import io.fireflysoftware.domain.accounting.core.journal.InterestJournal;
import org.fireflyframework.cqrs.command.CommandBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


@ExtendWith(MockitoExtension.class)
class PostInterestSagaTest {
    @Mock
    private CommandBus commandBus;

    @Test
    void postedBatchRequiresAnExplicitReversalInsteadOfDeletion() {
        UUID id = UUID.randomUUID();
        InterestJournal created = new InterestJournal(id, LocalDate.of(2026, 10, 21), new BigDecimal("2.19"), "EUR");

        StepVerifier.create(new PostInterestSaga(commandBus).remove(created))
                .expectErrorMatches(error -> error instanceof org.springframework.web.server.ResponseStatusException refusal
                        && refusal.getStatusCode().value() == 409)
                .verify();
        org.mockito.Mockito.verifyNoInteractions(commandBus);
    }
}

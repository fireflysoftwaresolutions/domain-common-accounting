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
import io.fireflysoftware.domain.accounting.core.journal.commands.RemoveInterestCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostInterestSagaTest {
    @Mock
    private CommandBus commandBus;

    @Test
    void removeDeletesThePostedBatch() {
        UUID id = UUID.randomUUID();
        when(commandBus.send(any(RemoveInterestCommand.class))).thenReturn(Mono.empty());
        InterestJournal created = new InterestJournal(id, LocalDate.of(2026, 10, 21), new BigDecimal("2.19"), "EUR");

        StepVerifier.create(new PostInterestSaga(commandBus).remove(created)).verifyComplete();

        ArgumentCaptor<RemoveInterestCommand> captor = ArgumentCaptor.forClass(RemoveInterestCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().journalId()).isEqualTo(id);
    }
}

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
import io.fireflysoftware.domain.accounting.core.journal.commands.PostInterestCommand;
import io.fireflysoftware.domain.accounting.core.journal.commands.RemoveInterestCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Posts the interest journal and deletes that batch if a later step fails. */
@Saga(name = PostInterestSaga.NAME)
@Service
public class PostInterestSaga {
    public static final String NAME = "PostInterestSaga";
    public static final String STEP = "post";
    public static final String COMPENSATE = "remove";

    private final CommandBus commandBus;

    public PostInterestSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP, compensate = COMPENSATE)
    @StepEvent(type = "accounting.interest.posted")
    public Mono<InterestJournal> post(PostInterestCommand command, ExecutionContext ctx) {
        return commandBus.<InterestJournal>send(command)
                .doOnNext(created -> ctx.putVariable("created", created));
    }

    public Mono<Void> remove(InterestJournal created) {
        if (created == null || created.id() == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveInterestCommand(created.id()));
    }
}

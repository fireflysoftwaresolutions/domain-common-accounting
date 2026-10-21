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

package io.fireflysoftware.domain.accounting.core.journal;

import io.fireflysoftware.domain.accounting.core.journal.commands.PostInterestCommand;
import io.fireflysoftware.domain.accounting.core.journal.workflows.PostInterestSaga;
import org.fireflyframework.orchestration.saga.engine.SagaEngine;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.fireflyframework.orchestration.saga.engine.StepInputs;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class JournalService {
    private final SagaEngine engine;
    private final CorePort port;

    public JournalService(SagaEngine engine, CorePort port) {
        this.engine = engine;
        this.port = port;
    }

    public Mono<InterestJournal> post(JournalRequest request) {
        StepInputs inputs = StepInputs.builder()
                .forStepId(PostInterestSaga.STEP, new PostInterestCommand(
                        request.businessDate(), request.amount(), request.currency(), request.reference()))
                .build();
        return engine.execute(PostInterestSaga.NAME, inputs)
                .flatMap(result -> outcome(result));
    }

    public Mono<InterestJournal> find(LocalDate businessDate) {
        return port.findInterest(businessDate);
    }

    private static Mono<InterestJournal> outcome(SagaResult result) {
        if (result.isFailed()) {
            Throwable error = result.error().orElseGet(() ->
                    new IllegalStateException("Saga " + result.sagaName() + " failed"));
            return Mono.error(error);
        }
        return Mono.justOrEmpty(result.resultOf(PostInterestSaga.STEP, InterestJournal.class))
                .switchIfEmpty(Mono.error(new IllegalStateException("Interest journal was not posted")));
    }
}

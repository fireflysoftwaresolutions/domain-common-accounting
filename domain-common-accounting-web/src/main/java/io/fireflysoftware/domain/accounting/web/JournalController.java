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

package io.fireflysoftware.domain.accounting.web;

import io.fireflysoftware.domain.accounting.core.journal.CorePort;
import io.fireflysoftware.domain.accounting.core.journal.InterestJournal;
import io.fireflysoftware.domain.accounting.core.journal.JournalRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/journals")
public class JournalController {
    private final CorePort port;

    public JournalController(CorePort port) {
        this.port = port;
    }

    @PostMapping
    public Mono<InterestJournal> post(@RequestBody JournalRequest request) {
        return port.postInterest(request.businessDate(), request.amount(), request.currency(), request.reference());
    }

    @GetMapping("/{businessDate}")
    public Mono<InterestJournal> read(@PathVariable LocalDate businessDate) {
        return port.findInterest(businessDate);
    }
}

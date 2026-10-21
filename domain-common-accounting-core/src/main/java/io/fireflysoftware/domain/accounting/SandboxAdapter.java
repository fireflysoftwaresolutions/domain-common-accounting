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

package io.fireflysoftware.domain.accounting;

import reactor.core.publisher.Mono;

import java.util.UUID;

/** Default adapter. Returns one sample row and does not call the core. */
public final class SandboxAdapter implements CorePort {
    @Override
    public Mono<LedgerAccountView> read(UUID id) {
        UUID resolved = id == null ? UUID.fromString("00000000-0000-4000-8000-000000000001") : id;
        return Mono.just(new LedgerAccountView(resolved, "1000"));
    }
}

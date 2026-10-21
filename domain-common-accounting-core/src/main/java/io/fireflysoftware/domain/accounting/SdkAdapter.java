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

import io.fireflysoftware.core.accounting.sdk.api.GlAccountsApi;
import io.fireflysoftware.core.accounting.invoker.ApiClient;
import io.fireflysoftware.core.accounting.sdk.model.GlAccountDTO;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Live adapter. Calls core-common-accounting-gl-sdk. Selected with firefly.accounting.gl.adapter=core. */
public final class SdkAdapter implements CorePort {
    private final GlAccountsApi api;

    public SdkAdapter(String basePath) {
        ApiClient client = new ApiClient();
        client.setBasePath(basePath);
        this.api = new GlAccountsApi(client);
    }

    SdkAdapter(GlAccountsApi api) {
        this.api = api;
    }

    @Override
    public Mono<LedgerAccountView> read(UUID id) {
        return api.getById3(id, null).map(dto -> new LedgerAccountView(dto.getId(), dto.getCode()));
    }
}

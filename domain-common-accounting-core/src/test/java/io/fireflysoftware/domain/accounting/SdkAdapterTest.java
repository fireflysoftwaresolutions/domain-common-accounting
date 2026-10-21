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
import io.fireflysoftware.core.accounting.sdk.model.GlAccountDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SdkAdapterTest {
    @Test
    void sandboxIsTheDefaultAndDoesNotOpenASocket() {
        new ApplicationContextRunner()
                .withUserConfiguration(AdapterConfiguration.class)
                .run(context -> {
                    CorePort port = context.getBean(CorePort.class);
                    assertThat(port).isInstanceOf(SandboxAdapter.class);
                    assertThat(port.read(null).block().label()).isEqualTo("1000");
                });
    }

    @Test
    void corePropertySelectsTheSdkAdapter() {
        new ApplicationContextRunner()
                .withUserConfiguration(AdapterConfiguration.class)
                .withPropertyValues("firefly.accounting.gl.adapter=core")
                .run(context -> assertThat(context.getBean(CorePort.class)).isInstanceOf(SdkAdapter.class));
    }

    @Test
    void liveReadCallsTheCoreSdk() {
        UUID id = UUID.randomUUID();
        GlAccountsApi api = mock(GlAccountsApi.class);
        GlAccountDTO dto = mock(GlAccountDTO.class);
        when(dto.getId()).thenReturn(id);
        when(api.getById3(id, null)).thenReturn(Mono.just(dto));
        LedgerAccountView view = new SdkAdapter(api).read(id).block();
        assertThat(view.id()).isEqualTo(id);
        verify(api).getById3(id, null);
    }
}

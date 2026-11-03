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

import io.fireflysoftware.domain.accounting.core.journal.AdapterConfiguration;
import io.fireflysoftware.domain.accounting.core.journal.CorePort;
import io.fireflysoftware.core.accounting.sdk.api.GlAccountsApi;
import io.fireflysoftware.core.accounting.sdk.model.GlAccountDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SdkAdapterTest {
    @Test
    void defaultConfigurationCannotReturnASyntheticAccount() {
        new ApplicationContextRunner().withUserConfiguration(AdapterConfiguration.class).run(context -> {
            CorePort port = context.getBean(CorePort.class);
            LegacyJournalIntegrityTest.refused(port.read(UUID.randomUUID()), 503);
        });
    }

    @Test
    void corePropertySelectsTheSdkAdapter() {
        new ApplicationContextRunner().withUserConfiguration(AdapterConfiguration.class)
                .withPropertyValues("firefly.accounting.gl.adapter=core")
                .run(context -> assertThat(context.getBean(CorePort.class)).isInstanceOf(SdkAdapterImpl.class));
    }

    @Test
    void liveReadCallsTheCoreSdk() {
        UUID id = UUID.randomUUID();
        GlAccountsApi api = mock(GlAccountsApi.class);
        GlAccountDTO dto = mock(GlAccountDTO.class);
        when(dto.getId()).thenReturn(id);
        when(api.getById3(id, null)).thenReturn(Mono.just(dto));
        assertThat(new SdkAdapterImpl(api).read(id).block().id()).isEqualTo(id);
        verify(api).getById3(id, null);
    }

    @Test
    void repeatedLegacyDateCannotBecomeAStoredReceipt() {
        var sandbox = new SandboxAdapterImpl();
        var date = LocalDate.of(2026, 11, 2);
        for (String amount : new String[]{"2.19", "4.38"}) {
            LegacyJournalIntegrityTest.refused(sandbox.postInterest(date, new BigDecimal(amount), "EUR", "original"), 422);
        }
        LegacyJournalIntegrityTest.refused(sandbox.findInterest(date), 422);
    }

    @Test
    void liveDateOnlyReadCannotClaimAbsenceWithoutCurrentOwnerAuthority() {
        var accounts = mock(GlAccountsApi.class);
        LegacyJournalIntegrityTest.refused(new SdkAdapterImpl(accounts).findInterest(LocalDate.of(2026, 11, 2)), 422);
        verifyNoInteractions(accounts);
    }
}

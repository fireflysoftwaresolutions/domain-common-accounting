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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class AdapterConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "firefly.accounting.gl", name = "adapter", havingValue = "sandbox", matchIfMissing = true)
    public CorePort sandboxPort() {
        return new SandboxAdapter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "firefly.accounting.gl", name = "adapter", havingValue = "core")
    public CorePort corePort(
            @Value("${firefly.accounting.gl.core-base-url:http://localhost:8080}") String basePath,
            @Value("${firefly.accounting.gl.interest-receivable-account:00000000-0000-4000-8000-000000000011}") UUID receivableAccountId,
            @Value("${firefly.accounting.gl.interest-income-account:00000000-0000-4000-8000-000000000012}") UUID incomeAccountId) {
        return new SdkAdapter(basePath, receivableAccountId, incomeAccountId);
    }
}

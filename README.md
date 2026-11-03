<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain - Accounting" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain - Accounting

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

This domain orchestrates approved Ledger-to-GL posting and original-action recovery through the generated Core GL SDK. Experience clients call the generated Domain SDK; business state remains in Core.

## Governed posting

Enable `firefly.accounting.managed-posting.enabled` only with the required issuer, audience, tenant, policy endpoint and caller-bound OAuth2 registrations configured. The routes under `/api/v1/tenants/{tenantId}/books/{bookId}/ledger-journals` preserve the complete original action and intent. Core independently checks committed Ledger evidence and approved account mapping before writing a balanced journal and immutable audit evidence.

An uncertain response does not authorize a new action, automatic retry with a new identity, or deletion. Use `/recovery` with the same complete request. The current admitted flow covers the configured USD Ledger transfer; it does not implement interest accrual, reversal, reconciliation or financial close.

## Legacy routes

The historical `sandbox` selector is retained for configuration compatibility, but it no longer invents accounts or stores journals in Domain memory. Unconfigured account reads return `503 GL_OWNER_NOT_CONFIGURED`. The `core` selector retains the original generated account-read SDK path; configuring a URL alone is not authorization to a protected owner.

Date-only interest posting and lookup are rejected: these requests lack a tenant, accounting book, original owner action, current approval and immutable financial-source evidence. A posted journal cannot be deleted as compensation; the legacy adapter rejects removal with `GL_POSTED_JOURNAL_REQUIRES_REVERSAL` without issuing a Core delete. These are explicit rejections of an unsafe legacy contract, not implemented interest or reversal journeys.

Completing interest accounting requires authoritative accrual evidence, governed mapping and approval, atomic owner posting, and a linked reversal operation. Existing ledger-transfer acceptance must not be presented as acceptance of those operations.

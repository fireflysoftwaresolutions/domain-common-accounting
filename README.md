<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain - Accounting" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain - Accounting

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Reads a general-ledger account from core-common-accounting-gl through core-common-accounting-gl-sdk (`GlAccountsApi.getById3`).

`firefly.accounting.gl.adapter` defaults to `sandbox`. `core` selects the SDK adapter. The sandbox returns one sample account and does not open a socket.

## Governed posting runtime

The separate governed journal-posting path delegates through the genuine GL Core SDK. Its original application now includes the managed PostgreSQL technical-cache provider, the standard Spring Boot reactive OAuth2 client auto-configuration, and the YAML provider at runtime. This resolves the observed missing client-registration repository and YAMLFactory startup failures without creating a custom credentials repository.

Domain excludes Boot JDBC and R2DBC business datasource auto-configuration. It does not own business tables or a repository template. Configure restricted technical-cache storage separately from Core business databases. Deployment OAuth2 registrations must provide the actual trusted issuer metadata, registered client, token endpoint, scopes and externally supplied secret; current original identity and operation checks remain in force.

The owning source suite passes all14 cases, including original behavior and new Boot registration/discovery and business-persistence boundary checks. The actual original Domain Main reaches authenticated metadata readiness in the six-service PostgreSQL runtime. This establishes source and startup behavior only. Experience→Domain→GL journal posting, restart/recovery and complete accounting acceptance remain separate pending integration gates. The legacy sample-account sandbox described above is not replaced or qualified by this runtime change.

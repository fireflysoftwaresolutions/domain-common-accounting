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

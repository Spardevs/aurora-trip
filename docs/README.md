# Aurora Trip POS Documentation

## Overview

Aurora Trip POS is an Android point-of-sale application for event ticketing and product sales. It supports multiple payment methods, NFC operations, and receipt printing.

## Documentation Structure

```
docs/
├── README.md                    # This file
├── architecture/                # Page/Activity documentation
│   ├── 00_AppNavigation.md     # Navigation overview
│   ├── 01_MainActivity.md      # Entry point
│   ├── 02_LoginPermissionsActivity.md
│   ├── 03_LoginActivity.md
│   ├── 04_LoginMenuActivity.md
│   ├── 05_LoginPosActivity.md
│   ├── 06_LoginConfirmActivity.md
│   ├── 07_LoginLoadingDownloadActivity.md
│   ├── 08_ProductsListActivity.md
│   ├── 09_QrScannerActivity.md
│   ├── 10_PaymentProcessingActivity.md
│   ├── 11_NFCActivity.md
│   ├── 12_PrintingActivity.md
│   └── 13_RefundActivity.md
├── features/                    # Feature-specific documentation
│   ├── MenuPinFeature.md       # Menu PIN whitelist
│   └── payment-methods-system.md
└── diagrams/                    # Mermaid diagrams
    ├── 00_AppNavigation.mmd
    ├── 01_MainActivity.mmd
    ├── 02_LoginPermissionsActivity.mmd
    ├── 03_LoginActivity.mmd
    ├── 04_LoginMenuActivity.mmd
    ├── 05_LoginPosActivity.mmd
    ├── 06_LoginConfirmActivity.mmd
    ├── 07_LoginLoadingDownloadActivity.mmd
    ├── 08_ProductsListActivity.mmd
    ├── 09_QrScannerActivity.mmd
    ├── 10_PaymentProcessingActivity.mmd
    ├── 11_NFCActivity.mmd
    ├── 12_PrintingActivity.mmd
    └── 13_RefundActivity.mmd
```

## Quick Links

### Getting Started
- [App Navigation Overview](./architecture/00_AppNavigation.md)
- [MainActivity (Entry Point)](./architecture/01_MainActivity.md)

### Login Flow
1. [LoginPermissionsActivity](./architecture/02_LoginPermissionsActivity.md) - Request permissions
2. [LoginActivity](./architecture/03_LoginActivity.md) - Email/QR authentication
3. [LoginMenuActivity](./architecture/04_LoginMenuActivity.md) - Select menu/event
4. [LoginPosActivity](./architecture/05_LoginPosActivity.md) - Select POS terminal
5. [LoginConfirmActivity](./architecture/06_LoginConfirmActivity.md) - Confirm session
6. [LoginLoadingDownloadActivity](./architecture/07_LoginLoadingDownloadActivity.md) - Sync data

### Main Application
- [ProductsListActivity](./architecture/08_ProductsListActivity.md) - Product catalog

### Operations
- [PaymentProcessingActivity](./architecture/10_PaymentProcessingActivity.md) - Payments
- [NFCActivity](./architecture/11_NFCActivity.md) - NFC operations
- [PrintingActivity](./architecture/12_PrintingActivity.md) - Receipt printing
- [RefundActivity](./architecture/13_RefundActivity.md) - Refunds

### Features
- [Authentication](./features/Authentication.md) - Email/Username/QR login
- [Menu PIN Feature](./features/MenuPinFeature.md) - PIN whitelist system
- [Payment Methods](./features/payment-methods-system.md) - Payment system

## Architecture Overview

### Layers

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION                            │
│  Activities, Fragments, ViewModels, Adapters                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         DOMAIN                               │
│  UseCases, Repository Interfaces, Models                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                          DATA                                │
│  Repository Implementations, DataSources, DTOs, Entities    │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│         LOCAL           │     │         REMOTE          │
│  Room Database, DAOs    │     │  Retrofit, API Services │
└─────────────────────────┘     └─────────────────────────┘
```

### Key Technologies

| Technology | Purpose |
|------------|---------|
| Kotlin | Primary language |
| Hilt | Dependency injection |
| Room | Local database |
| Retrofit | REST API client |
| Coroutines | Async operations |
| Flow | Reactive streams |
| ViewBinding | View access |
| ZXing | QR code scanning |

### Build Flavors

| Flavor | Acquirer SDK |
|--------|--------------|
| `pagseguro` | PagSeguro PlugPag |
| `stone` | Stone SDK |

## Diagrams

Mermaid diagrams are available in the `diagrams/` folder. View them with:
- VS Code Mermaid extension
- GitHub (renders automatically)
- [Mermaid Live Editor](https://mermaid.live/)

## Contributing

When adding new features:
1. Create architecture doc in `architecture/`
2. Create Mermaid diagram in `diagrams/`
3. Add feature doc in `features/` if applicable
4. Update this README

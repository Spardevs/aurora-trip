# App Navigation Overview

## Overview

Aurora Trip POS is an Android application for point-of-sale operations. The app follows a structured navigation flow from entry point through authentication to the main product catalog.

## Navigation Flow

```
MainActivity (Entry Point)
    │
    ├── No Permissions ──► LoginPermissionsActivity
    │                              │
    │                              ▼
    ├── Not Logged In ───► LoginActivity ◄──► QrScannerActivity
    │                              │
    │                              ▼
    │                      LoginMenuActivity
    │                              │
    │                              ▼
    │                      LoginPosActivity
    │                              │
    │                              ▼
    │                      LoginConfirmActivity
    │                              │
    │                              ▼
    └── Already Logged ──► LoginLoadingDownloadActivity
                                   │
                                   ▼
                           ProductsListActivity (Main App)
                                   │
                                   ├──► PaymentProcessingActivity
                                   ├──► NFCActivity
                                   ├──► PrintingActivity
                                   └──► RefundActivity
```

## Screen Categories

### Entry Point
| Screen | Purpose |
|--------|---------|
| `MainActivity` | Router - checks permissions and login status |

### Login Flow
| Screen | Purpose |
|--------|---------|
| `LoginPermissionsActivity` | Request CAMERA and LOCATION permissions |
| `LoginActivity` | Email/password or QR code authentication |
| `LoginMenuActivity` | Select menu/event to work with |
| `LoginPosActivity` | Select POS terminal |
| `LoginConfirmActivity` | Confirm session details and operator name |
| `LoginLoadingDownloadActivity` | Sync data (categories, products, thumbnails, pins) |

### Scanner
| Screen | Purpose |
|--------|---------|
| `QrScannerActivity` | Camera-based QR code reader for login |

### Main Application
| Screen | Purpose |
|--------|---------|
| `ProductsListActivity` | Product catalog with categories (main screen) |

### Operations (via Drawer Menu)
| Screen | Purpose |
|--------|---------|
| `PaymentProcessingActivity` | Process payments (credit, debit, PIX, etc.) |
| `NFCActivity` | NFC tag operations (read, write, format) |
| `PrintingActivity` | Receipt printing |
| `RefundActivity` | Process refunds |

## Key Files

```
app/src/main/java/br/com/ticpass/pos/
├── MainActivity.kt                           # Entry point
├── presentation/
│   ├── login/activities/
│   │   ├── LoginActivity.kt
│   │   ├── LoginPermissionsActivity.kt
│   │   ├── LoginMenuActivity.kt
│   │   ├── LoginPosActivity.kt
│   │   ├── LoginConfirmActivity.kt
│   │   └── LoginLoadingDownloadActivity.kt
│   ├── scanners/activities/
│   │   └── QrScannerActivity.kt
│   ├── product/activities/
│   │   └── ProductsListActivity.kt
│   ├── payment/
│   │   └── PaymentProcessingActivity.kt
│   ├── nfc/
│   │   └── NFCActivity.kt
│   ├── printing/
│   │   └── PrintingActivity.kt
│   └── refund/
│       └── RefundActivity.kt
```

## AndroidManifest Activities

All activities are declared in `AndroidManifest.xml` with appropriate configurations:
- `MainActivity` is the launcher activity
- Login activities use `noHistory` to prevent back navigation
- Scanner activity uses portrait orientation lock

## Session Management

Session data is managed via `SessionPrefsManagerUtils`:
- `menuId` - Selected menu/event
- `posId` - Selected POS terminal
- `deviceId` - Device identifier
- `operatorName` - Current operator name

## See Also

- [MainActivity](./01_MainActivity.md)
- [LoginActivity](./03_LoginActivity.md)
- [ProductsListActivity](./08_ProductsListActivity.md)

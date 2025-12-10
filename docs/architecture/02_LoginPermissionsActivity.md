# LoginPermissionsActivity

## Overview

`LoginPermissionsActivity` handles runtime permission requests for CAMERA and LOCATION. These permissions are required for QR code scanning and device location services.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/activities/LoginPermissionsActivity.kt
```

## Responsibilities

1. **Request Permissions** - Request CAMERA and ACCESS_FINE_LOCATION
2. **Handle Denials** - Show rationale dialog if permissions denied
3. **Settings Redirect** - Open app settings if permanently denied
4. **Navigate Forward** - Proceed to LoginActivity when granted

## Required Permissions

| Permission | Purpose |
|------------|---------|
| `CAMERA` | QR code scanning for login |
| `ACCESS_FINE_LOCATION` | Device location for POS operations |

## Permission Flow

```
┌─────────────────────────────┐
│  Check Permissions          │
└──────────────┬──────────────┘
               │
       ┌───────▼───────┐
       │  All Granted? │
       └───────┬───────┘
               │
      ┌────────┴────────┐
      │                 │
      ▼                 ▼
   [YES]              [NO]
      │                 │
      ▼                 ▼
 Navigate to      Request Permissions
 LoginActivity          │
                        ▼
                ┌───────────────┐
                │   Granted?    │
                └───────┬───────┘
                        │
               ┌────────┴────────┐
               │                 │
               ▼                 ▼
            [YES]              [NO]
               │                 │
               ▼                 ▼
          Navigate to     Show Rationale
          LoginActivity    or Settings
```

## Implementation

### Permission Request
```kotlin
private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    val allGranted = permissions.all { it.value }
    if (allGranted) {
        navigateNext()
    } else {
        showPermissionRationale()
    }
}
```

### Rationale Dialog
```kotlin
private fun showPermissionRationale() {
    AlertDialog.Builder(this)
        .setTitle("Permissões Necessárias")
        .setMessage("O app precisa de acesso à câmera e localização...")
        .setPositiveButton("Configurações") { _, _ ->
            openAppSettings()
        }
        .setNegativeButton("Cancelar", null)
        .show()
}
```

### Open Settings
```kotlin
private fun openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", packageName, null)
    startActivity(intent)
}
```

## Dependencies

### Android APIs
| API | Purpose |
|-----|---------|
| `ActivityResultContracts.RequestMultiplePermissions` | Request permissions |
| `AlertDialog` | Show rationale dialog |
| `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` | Open app settings |

### Data Layer
| Component | Purpose |
|-----------|---------|
| `AppDatabase` | Check for existing user |
| `UserDao` | Get user login status |

## Navigation

| Condition | Destination |
|-----------|-------------|
| Permissions granted | `LoginActivity` |

## Layout

```
res/layout/activity_permissions.xml
```

### Key Views
- `btnRequestPermissions` - Button to trigger permission request

## See Also

- [MainActivity](./01_MainActivity.md)
- [LoginActivity](./03_LoginActivity.md)

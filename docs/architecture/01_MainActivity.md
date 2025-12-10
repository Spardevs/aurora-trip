# MainActivity

## Overview

`MainActivity` is the application's entry point and router. It determines the initial navigation path based on permissions and login status.

## Location

```
app/src/main/java/br/com/ticpass/pos/MainActivity.kt
```

## Responsibilities

1. **Permission Check** - Verify required permissions are granted
2. **Login Status Check** - Check if user is already logged in
3. **Device Registration** - Register device with backend (background)
4. **Connectivity Monitoring** - Initialize network status monitoring
5. **Route Navigation** - Direct user to appropriate screen

## Navigation Logic

```kotlin
if (!hasAllPermissions()) {
    // Missing permissions
    startActivity(Intent(this, LoginPermissionsActivity::class.java))
} else if (user?.isLogged == true) {
    // Already logged in
    startActivity(Intent(this, LoginLoadingDownloadActivity::class.java))
} else {
    // Need to login
    SessionPrefsManagerUtils.clearAll()
    startActivity(Intent(this, LoginActivity::class.java))
}
```

## Dependencies

### Injected
| Dependency | Purpose |
|------------|---------|
| `UserRepository` | Check user login status |
| `DeviceService` | Register device with backend |
| `UserDao` | Direct database access for user |

### Utilities
| Utility | Purpose |
|---------|---------|
| `ConnectivityMonitor` | Monitor network connectivity |
| `ConnectionStatusBar` | Display connection status UI |
| `DeviceUtils` | Get device information |
| `SessionPrefsManagerUtils` | Manage session preferences |

## Required Permissions

```kotlin
private val requiredPermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION
)
```

## Device Registration

On startup, the app registers the device with the backend:

```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    try {
        val deviceInfo = DeviceUtils.getDeviceInfo(this@MainActivity)
        deviceService.registerDevice(deviceInfo)
    } catch (e: Exception) {
        Timber.e(e, "Failed to register device")
    }
}
```

## Data Layer

### Remote API
- `DeviceService.registerDevice()` - POST /device/register

### Local Database
- `UserDao.getAnyUserOnce()` - Check for logged user

## Layout

```
res/layout/activity_main.xml
```

## Lifecycle

1. `onCreate()` - Initialize SDK, check permissions
2. Route to appropriate activity
3. `finish()` - Close MainActivity after routing

## See Also

- [LoginPermissionsActivity](./02_LoginPermissionsActivity.md)
- [LoginActivity](./03_LoginActivity.md)
- [LoginLoadingDownloadActivity](./07_LoginLoadingDownloadActivity.md)

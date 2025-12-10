# Menu Pin Feature

## Overview

Menu Pins are a whitelist of authorized PIN codes for a specific menu/event. Each PIN is associated with a user and allows access control for POS operations.

## API Endpoint

```
GET /menu-pin-summary/{menuId}
```

### Headers
```
Content-Type: application/json
Cookie: access=<jwt_token>
Authorization: Basic <credentials>
```

### Response Example
```json
[
    {
        "id": "6921151c98152770a0ce8b50",
        "code": "0578",
        "user": {
            "id": "6921150898152770a0ce8b2d",
            "avatar": 10,
            "username": "riservatoxyz@gmail.com",
            "name": "Miguel Andrade",
            "email": "riservatoxyz@gmail.com"
        },
        "menu": "6921151c98152770a0ce8b42",
        "createdAt": "2025-11-22T01:42:52.630Z",
        "updatedAt": "2025-11-22T01:42:52.630Z"
    }
]
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION                              │
│  LoadingDownloadViewModel                                        │
│    └── RefreshMenuPinsUseCase                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          DOMAIN                                  │
│  MenuPinRepository (interface)                                   │
│  UseCases:                                                       │
│    - RefreshMenuPinsUseCase                                     │
│    - ValidateMenuPinUseCase                                     │
│    - GetMenuPinsUseCase                                         │
│  Models:                                                         │
│    - MenuPin                                                     │
│    - MenuPinUser                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                           DATA                                   │
│  MenuPinRepositoryImpl                                          │
│    ├── MenuPinLocalDataSource                                   │
│    │     └── MenuPinDao (Room)                                  │
│    └── MenuPinRemoteDataSource                                  │
│          └── MenuPinApiService (Retrofit)                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Database Schema

```sql
CREATE TABLE menu_pin (
    id TEXT PRIMARY KEY,
    code TEXT NOT NULL,
    menuId TEXT NOT NULL,
    userId TEXT NOT NULL,
    userName TEXT,
    userEmail TEXT,
    userAvatar INTEGER,
    createdAt TEXT NOT NULL,
    updatedAt TEXT NOT NULL
)
```

---

## Usage Examples

### 1. Refresh Pins from API (during login flow)

```kotlin
// In LoadingDownloadViewModel - automatically called during login
refreshMenuPinsUseCase(menuId)
```

### 2. Validate a PIN Code

```kotlin
@Inject
lateinit var validateMenuPinUseCase: ValidateMenuPinUseCase

suspend fun validateUserPin(menuId: String, pinCode: String): MenuPin? {
    val menuPin = validateMenuPinUseCase(menuId, pinCode)
    
    if (menuPin != null) {
        // PIN is valid - returns both PIN and user info
        Log.d("MenuPin", "Valid PIN: ${menuPin.code}")
        Log.d("MenuPin", "User ID: ${menuPin.user.id}")
        Log.d("MenuPin", "User name: ${menuPin.user.name}")
        Log.d("MenuPin", "User email: ${menuPin.user.email}")
        Log.d("MenuPin", "User avatar: ${menuPin.user.avatar}")
        return menuPin
    } else {
        // PIN is invalid or not in whitelist
        Log.w("MenuPin", "Invalid PIN: $pinCode")
        return null
    }
}

// Example: Use the validated PIN and user
suspend fun onPinEntered(menuId: String, pinCode: String) {
    val result = validateUserPin(menuId, pinCode)
    
    result?.let { menuPin ->
        // Access PIN data
        val pinId = menuPin.id
        val code = menuPin.code
        
        // Access user data
        val userId = menuPin.user.id
        val userName = menuPin.user.name ?: "Unknown"
        val userEmail = menuPin.user.email
        val userAvatar = menuPin.user.avatar
        
        // Proceed with authenticated user
        proceedWithUser(userId, userName)
    } ?: run {
        // Show error - invalid PIN
        showError("PIN inválido")
    }
}
```

### 3. Get All Pins for a Menu (Reactive)

```kotlin
@Inject
lateinit var getMenuPinsUseCase: GetMenuPinsUseCase

fun observeMenuPins(menuId: String) {
    viewModelScope.launch {
        getMenuPinsUseCase(menuId).collect { pins ->
            // Update UI with list of authorized pins
            pins.forEach { pin ->
                Log.d("MenuPin", "PIN: ${pin.code} - User: ${pin.user.name}")
            }
        }
    }
}
```

### 4. Direct Repository Access

```kotlin
@Inject
lateinit var menuPinRepository: MenuPinRepository

// Get pins once (not reactive)
suspend fun getPinsOnce(menuId: String): List<MenuPin> {
    return menuPinRepository.getPinsByMenuIdOnce(menuId)
}

// Count pins for a menu
suspend fun countPins(menuId: String): Int {
    return menuPinRepository.countPins(menuId)
}
```

---

## File Structure

```
app/src/main/java/br/com/ticpass/pos/
├── data/menupin/
│   ├── datasource/
│   │   ├── MenuPinLocalDataSource.kt
│   │   └── MenuPinRemoteDataSource.kt
│   ├── local/
│   │   ├── dao/
│   │   │   └── MenuPinDao.kt
│   │   └── entity/
│   │       └── MenuPinEntity.kt
│   ├── mapper/
│   │   └── MenuPinMapper.kt
│   ├── remote/
│   │   ├── dto/
│   │   │   └── MenuPinDto.kt
│   │   └── service/
│   │       └── MenuPinApiService.kt
│   └── repository/
│       └── MenuPinRepositoryImpl.kt
├── domain/menupin/
│   ├── model/
│   │   └── MenuPin.kt
│   ├── repository/
│   │   └── MenuPinRepository.kt
│   └── usecase/
│       ├── GetMenuPinsUseCase.kt
│       ├── RefreshMenuPinsUseCase.kt
│       └── ValidateMenuPinUseCase.kt
└── core/di/
    └── MenuPinModule.kt
```

---

## Integration in Login Flow

Menu pins are automatically downloaded during the login process in `LoadingDownloadViewModel`:

```kotlin
fun startCompleteProcess(menuId: String, posId: String, ...) {
    viewModelScope.launch {
        // Step 1: Download Categories
        refreshCategoriesUseCase(menuId)
        
        // Step 2: Download Products
        refreshProductsUseCase(menuId)
        
        // Step 3: Download Thumbnails
        productRepository.downloadAndExtractThumbnails(menuId, thumbnailsDir)
        
        // Step 4: Download Menu PINs (whitelist)  ← HERE
        refreshMenuPinsUseCase(menuId)
        
        // Step 5: Set User Logged
        userRepository.setUserLogged(userId, true)
        
        // Step 6-7: POS configuration...
    }
}
```

---

## Domain Models

### MenuPin
```kotlin
data class MenuPin(
    val id: String,
    val code: String,           // The PIN code (e.g., "0578")
    val menuId: String,         // Associated menu ID
    val user: MenuPinUser,      // User who owns this PIN
    val createdAt: String,
    val updatedAt: String
)
```

### MenuPinUser
```kotlin
data class MenuPinUser(
    val id: String,
    val name: String?,          // User's display name
    val email: String?,         // User's email
    val avatar: Int?            // Avatar ID
)
```

# LoginActivity

## Overview

`LoginActivity` handles user authentication via email/password or QR code. It's the primary login screen after permissions are granted.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/activities/LoginActivity.kt
```

## Responsibilities

1. **Email/Password Login** - Traditional credential-based authentication
2. **QR Code Login** - Scan QR code for quick authentication
3. **Display Login States** - Show processing, success, or error fragments
4. **Token Management** - Store access/refresh tokens on success

## Authentication Methods

### Email/Password
```kotlin
private fun performEmailLogin(email: String, password: String) {
    lifecycleScope.launch {
        val result = authRepository.signIn(email, password)
        result.onSuccess { response ->
            saveUserAndNavigate(response)
        }.onFailure { error ->
            showError(error.message)
        }
    }
}
```

### QR Code
```kotlin
private val qrScannerLauncher = registerForActivityResult(QrScannerContract()) { qrText ->
    qrText?.let { qrViewModel.processQrCode(it) }
}

// Launch scanner
qrScannerLauncher.launch(Unit)
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      LoginActivity                           │
│  ┌─────────────────┐  ┌─────────────────────────────────┐  │
│  │ Email/Password  │  │      QR Code Login              │  │
│  │    Form         │  │  ┌─────────────────────────┐    │  │
│  │                 │  │  │    QrLoginViewModel     │    │  │
│  │                 │  │  └───────────┬─────────────┘    │  │
│  └────────┬────────┘  │              │                  │  │
│           │           │              ▼                  │  │
│           │           │  ┌─────────────────────────┐    │  │
│           │           │  │  SignInWithQrUseCase    │    │  │
│           │           │  └───────────┬─────────────┘    │  │
│           │           │              │                  │  │
│           └───────────┴──────────────┼──────────────────┘  │
│                                      │                      │
└──────────────────────────────────────┼──────────────────────┘
                                       ▼
                            ┌─────────────────────┐
                            │  AuthRepositoryImpl │
                            └─────────┬───────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
            ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
            │ AuthService │  │ TokenManager│  │   UserDao   │
            │  (Retrofit) │  │   (Cache)   │  │   (Room)    │
            └─────────────┘  └─────────────┘  └─────────────┘
```

## ViewModel

### QrLoginViewModel

```kotlin
@HiltViewModel
class QrLoginViewModel @Inject constructor(
    private val signInWithQrUseCase: SignInWithQrUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<QrLoginState>(QrLoginState.Idle)
    val state: StateFlow<QrLoginState> = _state

    fun processQrCode(qrText: String) {
        viewModelScope.launch {
            _state.value = QrLoginState.Processing
            try {
                val token = signInWithQrUseCase(qrText)
                _state.value = QrLoginState.Success(token)
            } catch (e: Exception) {
                _state.value = QrLoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### QrLoginState

```kotlin
sealed class QrLoginState {
    object Idle : QrLoginState()
    object Processing : QrLoginState()
    data class Success(val token: String) : QrLoginState()
    data class Error(val message: String) : QrLoginState()
}
```

## UI States (Fragments)

| State | Fragment | Description |
|-------|----------|-------------|
| Processing | `QrCodeProcessingFragment` | Loading spinner |
| Success | `QrCodeSuccessFragment` | Success animation |
| Error | `QrCodeErrorFragment` | Error message with retry |

## Domain Layer

### SignInWithQrUseCase

```kotlin
class SignInWithQrUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(qrText: String): String {
        val parsed = parseQrCode(qrText)
        return authRepository.signInWithQrCode(parsed.token)
    }
}
```

## Data Layer

### AuthService (Retrofit)

```kotlin
interface AuthService {
    @POST("auth/signin")
    suspend fun signIn(@Header("Authorization") auth: String): Response<LoginResponse>

    @POST("auth/signin/pos/short-lived")
    suspend fun signInShortLived(@Body request: QrLoginRequest): Response<LoginResponse>
}
```

### LoginResponse

```kotlin
data class LoginResponse(
    val user: UserDto,
    val jwt: JwtDto
)

data class JwtDto(
    val accessToken: String,
    val refreshToken: String
)
```

## Token Storage

```kotlin
// TokenManager saves tokens to Room database
tokenManager.saveTokens(accessToken, refreshToken)
```

## Navigation

| Condition | Destination |
|-----------|-------------|
| Login success | `LoginMenuActivity` |

## Layout

```
res/layout/activity_login.xml
```

### Key Views
| View ID | Purpose |
|---------|---------|
| `editTextTextEmailAddress` | Email input |
| `editTextTextPassword` | Password input |
| `button_confirm` | Login button |
| `qr_code_login_button` | Launch QR scanner |

## Dependencies

### Injected
- `QrLoginViewModel` - QR login state management
- `AuthRepository` - Authentication operations
- `UserDao` - User persistence

### Contracts
- `QrScannerContract` - Launch QR scanner activity

## See Also

- [QrScannerActivity](./09_QrScannerActivity.md)
- [LoginMenuActivity](./04_LoginMenuActivity.md)
- [AuthRepository](../features/Authentication.md)

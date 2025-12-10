# LoginActivity

## Overview

`LoginActivity` handles user authentication via email/password, username/password, or QR code. It's the primary login screen after permissions are granted.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/activities/LoginActivity.kt
```

## Responsibilities

1. **Email/Password Login** - Traditional email-based authentication
2. **Username/Password Login** - Username-based authentication (alphanumeric)
3. **QR Code Login** - Scan QR code for quick authentication
4. **Input Validation** - Validate email format or username format with feedback
5. **Display Login States** - Show processing, success, or error fragments
6. **Token Management** - Store access/refresh tokens on success

## Authentication Methods

The login form accepts either an **email** or **username** in the same input field. The system automatically detects which type based on the presence of `@`:

- **Contains `@`** → Treated as email, validated with email pattern
- **No `@`** → Treated as username, validated as alphanumeric (min 3 chars)

### Credential Detection

```kotlin
private sealed class LoginCredential {
    data class Email(val email: String, val password: String) : LoginCredential()
    data class Username(val username: String, val password: String) : LoginCredential()
}

private fun isEmailFormat(input: String): Boolean {
    return input.contains("@")
}
```

### Validation Rules

| Type | Validation | Error Message |
|------|------------|---------------|
| Email | `Patterns.EMAIL_ADDRESS` regex | "E-mail inválido. Verifique o formato (ex: usuario@email.com)" |
| Username | `^[a-zA-Z0-9_]{3,}$` regex | "Usuário inválido. Use apenas letras, números e _ (mín. 3 caracteres)" |
| Password (empty) | Non-empty | "Senha obrigatória" |
| Password (length) | Min 12 characters | "Senha deve ter no mínimo 12 caracteres" |
| Empty identifier | - | "E-mail ou usuário obrigatório" |

### Login Flow

```kotlin
private fun performLogin(credential: LoginCredential) {
    lifecycleScope.launch {
        val response = when (credential) {
            is LoginCredential.Email -> 
                authRepository.signInWithEmail(credential.email, credential.password)
            is LoginCredential.Username -> 
                authRepository.signInWithUsername(credential.username, credential.password)
        }
        // Handle response...
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
    @POST("auth/signin/pos")
    suspend fun signIn(
        @Body requestBody: RequestBody
    ): Response<LoginResponse>

    @POST("auth/signin/pos/short-lived")
    suspend fun signInShortLived(
        @Body requestBody: RequestBody,
        @Header("Cookie") cookieHeader: String
    ): Response<LoginResponse>
}
```

### API Endpoints

| Method | Endpoint | Body | Description |
|--------|----------|------|-------------|
| Email | `POST /auth/signin/pos` | `{"email": "<email>", "password": "<password>"}` | Login with email |
| Username | `POST /auth/signin/pos` | `{"username": "<username>", "password": "<password>"}` | Login with username |
| QR Code | `POST /auth/signin/pos/short-lived` | `{}` + Cookie (shortLived=token) | Login with QR token |

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
| `editTextTextEmailAddress` | Email or Username input (same field) |
| `editTextTextPassword` | Password input |
| `button_confirm` | Login button |
| `qr_code_login_button` | Launch QR scanner |
| `login_error_text` | Error message display |
| `choiceContainer` | Initial choice screen (email/QR buttons) |
| `formContainer` | Login form container |

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

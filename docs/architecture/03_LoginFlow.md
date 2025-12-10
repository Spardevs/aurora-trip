# Login Flow

## Overview

The login flow handles user authentication via email/password, username/password, or QR code. It uses a fragment-based architecture with a host activity and dedicated fragments for each login method.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/
├── activities/
│   └── LoginHostActivity.kt       # Host activity (100 lines)
└── fragments/
    ├── LoginChoiceFragment.kt     # Login method selection (132 lines)
    └── CredentialLoginFragment.kt # Email/username form (210 lines)
```

## Architecture

The login flow is split into focused components:

| Component | Lines | Responsibility |
|-----------|-------|----------------|
| `LoginHostActivity` | 100 | Host container, falling animation, device info dialog |
| `LoginChoiceFragment` | 132 | Login method buttons, QR scanner flow |
| `CredentialLoginFragment` | 210 | Email/username form, validation, state observation |

> **Note:** Business logic (validation, API calls) is delegated to ViewModels and UseCases following Clean Architecture.

## Authentication Methods

The login form accepts either an **email** or **username** in the same input field. The system automatically detects which type based on the presence of `@`:

- **Contains `@`** → Treated as email, validated with email pattern
- **No `@`** → Treated as username, validated as alphanumeric (min 3 chars)

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           LoginHostActivity                                  │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      FragmentContainerView                              │ │
│  │  ┌──────────────────────────┐    ┌───────────────────────────────────┐ │ │
│  │  │   LoginChoiceFragment    │───▶│     CredentialLoginFragment       │ │ │
│  │  │  ┌────────────────────┐  │    │  ┌─────────────────────────────┐  │ │ │
│  │  │  │  QrLoginViewModel  │  │    │  │      LoginViewModel         │  │ │ │
│  │  │  └─────────┬──────────┘  │    │  └──────────────┬──────────────┘  │ │ │
│  │  │            │             │    │                 │                 │ │ │
│  │  │            ▼             │    │                 ▼                 │ │ │
│  │  │  ┌────────────────────┐  │    │  ┌─────────────────────────────┐  │ │ │
│  │  │  │ SignInWithQrUseCase│  │    │  │ SignInWithCredentialUseCase │  │ │ │
│  │  │  └────────────────────┘  │    │  └──────────────┬──────────────┘  │ │ │
│  │  └──────────────────────────┘    │                 │                 │ │ │
│  │                                  │  ┌─────────────────────────────┐  │ │ │
│  │                                  │  │    CredentialValidator      │  │ │ │
│  │                                  │  └─────────────────────────────┘  │ │ │
│  │                                  └───────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
                             ┌─────────────────────┐
                             │  AuthRepositoryImpl │
                             └─────────┬───────────┘
                                       │
                 ┌─────────────────────┼─────────────────────┐
                 ▼                     ▼                     ▼
         ┌─────────────┐       ┌─────────────┐       ┌─────────────┐
         │ AuthService │       │ TokenManager│       │   UserDao   │
         │  (Retrofit) │       │   (Cache)   │       │   (Room)    │
         └─────────────┘       └─────────────┘       └─────────────┘
```

## ViewModels

### LoginViewModel (Credential Login)

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithCredentialUseCase: SignInWithCredentialUseCase,
    private val credentialValidator: CredentialValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(identifier: String, password: String) {
        if (_uiState.value is LoginUiState.Loading) return

        when (val result = credentialValidator.validate(identifier, password)) {
            is ValidationResult.Invalid -> {
                _uiState.value = LoginUiState.ValidationFailed(result.error)
            }
            is ValidationResult.Valid -> {
                performLogin(result.credential)
            }
        }
    }

    private fun performLogin(credential: LoginCredential) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            signInWithCredentialUseCase(credential).fold(
                onSuccess = { _uiState.value = LoginUiState.Success },
                onFailure = { _uiState.value = LoginUiState.Error(it.message ?: "Unknown") }
            )
        }
    }
}
```

### LoginUiState

```kotlin
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class ValidationFailed(val error: ValidationError) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
```

### QrLoginViewModel

```kotlin
@HiltViewModel
class QrLoginViewModel @Inject constructor(
    private val signInWithQrUseCase: SignInWithQrUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<QrLoginState>(QrLoginState.Idle)
    val state: StateFlow<QrLoginState> = _state

    fun signInWithQr(qrText: String) {
        viewModelScope.launch {
            _state.value = QrLoginState.Processing
            signInWithQrUseCase(qrText).fold(
                onSuccess = { _state.value = QrLoginState.Success(it.first) },
                onFailure = { _state.value = QrLoginState.Error(it.message ?: "Unknown") }
            )
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

### Models

```kotlin
// LoginCredential.kt
sealed class LoginCredential {
    data class Email(val email: String, val password: String) : LoginCredential()
    data class Username(val username: String, val password: String) : LoginCredential()
}

// ValidationResult.kt
sealed class ValidationResult {
    data class Valid(val credential: LoginCredential) : ValidationResult()
    data class Invalid(val error: ValidationError) : ValidationResult()
}

enum class ValidationError {
    EMPTY_IDENTIFIER,
    INVALID_EMAIL_FORMAT,
    INVALID_USERNAME_FORMAT,
    EMPTY_PASSWORD,
    PASSWORD_TOO_SHORT
}
```

### CredentialValidator

```kotlin
class CredentialValidator @Inject constructor() {
    fun validate(identifier: String, password: String): ValidationResult {
        // Validates identifier (email or username) and password
        // Returns Valid(credential) or Invalid(error)
    }
}
```

### Validation Rules

| Type | Validation | Error |
|------|------------|-------|
| Email | `Patterns.EMAIL_ADDRESS` | `INVALID_EMAIL_FORMAT` |
| Username | `^[a-zA-Z0-9_]{3,}$` | `INVALID_USERNAME_FORMAT` |
| Password | Non-empty | `EMPTY_PASSWORD` |
| Password | Min 12 chars | `PASSWORD_TOO_SHORT` |
| Identifier | Non-empty | `EMPTY_IDENTIFIER` |

### SignInWithCredentialUseCase

```kotlin
class SignInWithCredentialUseCase @Inject constructor(
    private val authRepository: AuthRepositoryImpl
) {
    suspend operator fun invoke(credential: LoginCredential): Result<LoginResponse> {
        return when (credential) {
            is LoginCredential.Email -> 
                authRepository.signInWithEmail(credential.email, credential.password)
            is LoginCredential.Username -> 
                authRepository.signInWithUsername(credential.username, credential.password)
        }
    }
}
```

### SignInWithQrUseCase

```kotlin
class SignInWithQrUseCase @Inject constructor(
    private val authRepository: AuthRepositoryImpl
) {
    suspend operator fun invoke(shortLivedToken: String): Result<Pair<LoginResponse, Pair<String?, String?>>> {
        // Calls authRepository.signInWithQrCode(shortLivedToken)
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

## Layouts

| Layout | Component |
|--------|-----------|
| `activity_login_host.xml` | Host activity with fragment container |
| `fragment_login_choice.xml` | Login method buttons |
| `fragment_credential_login.xml` | Email/username form |

### Key Views

**LoginChoiceFragment:**
| View ID | Purpose |
|---------|---------|
| `email_login_button` | Navigate to credential form |
| `qr_code_login_button` | Launch QR scanner |

**CredentialLoginFragment:**
| View ID | Purpose |
|---------|---------|
| `editTextTextEmailAddress` | Email or Username input |
| `editTextTextPassword` | Password input |
| `button_confirm` | Login button |
| `button_back` | Return to choice screen |
| `login_error_text` | Error message display |
| `loadingOverlay` | Loading indicator |

## Dependencies

### ViewModels (Injected via Hilt)
- `LoginViewModel` - Credential login state management (used by `CredentialLoginFragment`)
- `QrLoginViewModel` - QR login state management (used by `LoginChoiceFragment`)

### Contracts
- `QrScannerContract` - Launch QR scanner activity

### Callbacks
- `LoginHostCallback` - Interface for fragment-to-activity communication (animations)

## File Structure

```
domain/login/
├── model/
│   ├── LoginCredential.kt      # Email/Username sealed class
│   └── ValidationResult.kt     # Valid/Invalid result + ValidationError enum
├── usecase/
│   ├── SignInWithCredentialUseCase.kt
│   └── SignInWithQrUseCase.kt
└── validator/
    └── CredentialValidator.kt  # Input validation logic

presentation/login/
├── activities/
│   └── LoginHostActivity.kt    # Host activity (100 lines)
├── fragments/
│   ├── LoginChoiceFragment.kt  # Login method selection (132 lines)
│   └── CredentialLoginFragment.kt # Email/username form (210 lines)
├── states/
│   └── LoginUiState.kt         # Credential login states
└── viewmodels/
    └── LoginViewModel.kt       # Credential login ViewModel
```

## See Also

- [QrScannerActivity](./09_QrScannerActivity.md)
- [LoginMenuActivity](./04_LoginMenuActivity.md)
- [AuthRepository](../features/Authentication.md)

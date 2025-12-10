# Authentication Feature

## Overview

Aurora Trip POS supports three authentication methods:
1. **Email/Password** - Traditional email-based login
2. **Username/Password** - Alphanumeric username-based login
3. **QR Code** - Quick login via scanned QR token

## API Endpoints

| Method | Endpoint | Body | Description |
|--------|----------|------|-------------|
| Email | `POST /auth/signin/pos` | `{"email": "<email>", "password": "<password>"}` | Login with email |
| Username | `POST /auth/signin/pos` | `{"username": "<username>", "password": "<password>"}` | Login with username |
| QR Code | `POST /auth/signin/pos/short-lived` | `{}` + `Cookie: shortLived=<token>` | Login with QR token |

### Request Format

Email and username login use the **same endpoint** (`/auth/signin/pos`) but differ in the request body:
- **Email**: `{"email": "user@example.com", "password": "securePassword123!"}`
- **Username**: `{"username": "john_doe", "password": "securePassword123!"}`

### Response Format

```json
{
    "user": {
        "id": "user_id",
        "email": "user@example.com",
        "name": "User Name"
    },
    "jwt": {
        "access": "access_token_here",
        "refresh": "refresh_token_here"
    }
}
```

Tokens are also returned in `Set-Cookie` headers:
- `access=<token>; ...`
- `refresh=<token>; ...`

---

## Input Validation

### Email vs Username Detection

The system automatically detects the credential type based on the `@` character:

```kotlin
private fun isEmailFormat(input: String): Boolean {
    return input.contains("@")
}
```

### Validation Rules

| Type | Rule | Regex | Error Message |
|------|------|-------|---------------|
| **Email** | Must match email pattern | `Patterns.EMAIL_ADDRESS` | "E-mail inválido. Verifique o formato (ex: usuario@email.com)" |
| **Username** | Alphanumeric + underscore, min 3 chars | `^[a-zA-Z0-9_]{3,}$` | "Usuário inválido. Use apenas letras, números e _ (mín. 3 caracteres)" |
| **Password (empty)** | Non-empty | - | "Senha obrigatória" |
| **Password (length)** | Min 12 characters | - | "Senha deve ter no mínimo 12 caracteres" |
| **Empty identifier** | - | - | "E-mail ou usuário obrigatório" |

### Valid Examples

| Input | Type | Valid |
|-------|------|-------|
| `user@example.com` | Email | ✅ |
| `john.doe@company.co.uk` | Email | ✅ |
| `invalid@` | Email | ❌ |
| `@invalid.com` | Email | ❌ |
| `john_doe` | Username | ✅ |
| `user123` | Username | ✅ |
| `ab` | Username | ❌ (too short) |
| `user@name` | Email | ❌ (invalid email) |
| `user-name` | Username | ❌ (hyphen not allowed) |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        LoginActivity                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  validateInputs() → LoginCredential.Email/Username        │  │
│  │  performLogin(credential)                                  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AuthRepositoryImpl                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  signInWithEmail(email, password)                        │   │
│  │  signInWithUsername(username, password)                  │   │
│  │  signInWithQrCode(qrToken)                               │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        AuthService                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  POST /auth/signin/pos (email or username in body)       │   │
│  │  POST /auth/signin/pos/short-lived (QR code)             │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       TokenManager                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  saveTokens(accessToken, refreshToken)                   │   │
│  │  getAccessToken()                                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Code Examples

### Email Login

```kotlin
// User enters: "user@example.com"
// System detects: contains "@" → Email

val credential = LoginCredential.Email("user@example.com", "password123")
val response = authRepository.signInWithEmail(credential.email, credential.password)
```

### Username Login

```kotlin
// User enters: "john_doe"
// System detects: no "@" → Username

val credential = LoginCredential.Username("john_doe", "password123")
val response = authRepository.signInWithUsername(credential.username, credential.password)
```

### QR Code Login

```kotlin
// QR code scanned with token
qrViewModel.signInWithQr(qrToken)

// Internally calls:
val response = authRepository.signInWithQrCode(qrToken)
```

---

## Error Handling

### HTTP Error Codes

| Code | Email Message | Username Message |
|------|---------------|------------------|
| 400 | "Requisição inválida" | "Requisição inválida" |
| 401 | "E-mail ou senha incorretos" | "Usuário ou senha incorretos" |
| 404 | "E-mail não encontrado" | "Usuário não encontrado" |
| 5xx | "Erro no servidor" | "Erro no servidor" |

### Validation Errors (Client-side)

Validation errors are shown in the `login_error_text` TextView with the field highlighted in red.

---

## File Structure

```
app/src/main/java/br/com/ticpass/pos/
├── data/auth/
│   ├── remote/
│   │   ├── dto/
│   │   │   └── LoginResponse.kt
│   │   └── service/
│   │       └── AuthService.kt
│   └── repository/
│       └── AuthRepositoryImpl.kt
├── presentation/login/activities/
│   └── LoginActivity.kt
└── core/network/
    └── TokenManager.kt
```

---

## Token Storage

Tokens are stored via `TokenManager` and persisted in Room database:

```kotlin
// After successful login
tokenManager.saveTokens(accessToken, refreshToken)

// For authenticated requests
val token = tokenManager.getAccessToken()
```

---

## See Also

- [LoginActivity](../architecture/03_LoginActivity.md)
- [QrScannerActivity](../architecture/09_QrScannerActivity.md)

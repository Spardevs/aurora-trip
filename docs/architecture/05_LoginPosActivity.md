# LoginPosActivity

## Overview

`LoginPosActivity` displays a list of available POS terminals for the selected menu. The user selects which terminal they will operate.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/activities/LoginPosActivity.kt
```

## Responsibilities

1. **Load POS List** - Fetch available POS terminals for the menu
2. **Refresh from API** - Sync POS list from backend
3. **Display Grid** - Show terminals in a 3-column grid
4. **Handle Selection** - Save selected POS and navigate

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     LoginPosActivity                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │               LoginPosViewModel                      │   │
│  │  ┌──────────────────┐  ┌────────────────────────┐   │   │
│  │  │ GetPosByMenuUC   │  │ RefreshPosListUseCase  │   │   │
│  │  └────────┬─────────┘  └───────────┬────────────┘   │   │
│  │           │                        │                 │   │
│  │           └────────────┬───────────┘                 │   │
│  │                        ▼                             │   │
│  │               ┌─────────────────┐                    │   │
│  │               │  PosRepository  │                    │   │
│  │               └─────────────────┘                    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## ViewModel

### LoginPosViewModel

```kotlin
@HiltViewModel
class LoginPosViewModel @Inject constructor(
    private val getPosByMenuUseCase: GetPosByMenuUseCase,
    private val refreshPosListUseCase: RefreshPosListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginPosUiState>(LoginPosUiState.Loading)
    val uiState: StateFlow<LoginPosUiState> = _uiState

    fun loadPos(menuId: String) {
        viewModelScope.launch {
            try {
                refreshPosListUseCase(menuId)
                val posList = getPosByMenuUseCase(menuId)
                _uiState.value = LoginPosUiState.Success(posList)
            } catch (e: Exception) {
                _uiState.value = LoginPosUiState.Error(e.message)
            }
        }
    }
}
```

### LoginPosUiState

```kotlin
sealed class LoginPosUiState {
    object Loading : LoginPosUiState()
    data class Success(val posList: List<Pos>) : LoginPosUiState()
    object Empty : LoginPosUiState()
    data class Error(val message: String?) : LoginPosUiState()
}
```

## Domain Model

### Pos

```kotlin
data class Pos(
    val id: String,
    val prefix: String,      // e.g., "POS"
    val sequence: Int,       // e.g., 1, 2, 3
    val commission: Double?  // Optional commission rate
)
```

## UI Components

### RecyclerView Adapter

```kotlin
class LoginPosAdapter(
    private val onPosClick: (Pos) -> Unit
) : ListAdapter<Pos, PosViewHolder>(PosDiffCallback()) {
    // Grid display with prefix + sequence
}
```

### Grid Layout
- 3 columns
- Display: `"${pos.prefix} ${pos.sequence}"` (e.g., "POS 1")

## POS Selection

```kotlin
fun onPosSelected(posId: String, posName: String) {
    // Save POS details to session
    SessionPrefsManagerUtils.savePosId(posId)
    SessionPrefsManagerUtils.savePosName(posName)
    
    // Navigate to confirmation
    val intent = Intent(this, LoginConfirmActivity::class.java)
    intent.putExtra("POS_ID", posId)
    intent.putExtra("POS_NAME", posName)
    startActivity(intent)
}
```

## Data Layer

### PosRepository

```kotlin
interface PosRepository {
    suspend fun getPosByMenu(menuId: String): List<Pos>
    suspend fun refreshPosList(menuId: String)
    suspend fun selectPos(posId: String): Result<Unit>
    suspend fun openPosSession(posId: String, deviceId: String, cashierName: String): Result<Unit>
}
```

## Navigation

| Action | Destination |
|--------|-------------|
| POS selected | `LoginConfirmActivity` |

## Layout

```
res/layout/activity_login_pos.xml
```

### Key Views
| View ID | Purpose |
|---------|---------|
| `pos_recycler_view` | Grid of POS terminals |

## Fragments

| Fragment | Purpose |
|----------|---------|
| `LoginLoadingFragment` | Show while loading POS list |

## Session Data Saved

| Key | Value |
|-----|-------|
| `posId` | Selected POS ID |
| `posName` | POS display name |
| `commission` | Commission rate (if applicable) |

## See Also

- [LoginMenuActivity](./04_LoginMenuActivity.md)
- [LoginConfirmActivity](./06_LoginConfirmActivity.md)

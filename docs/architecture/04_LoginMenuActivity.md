# LoginMenuActivity

## Overview

`LoginMenuActivity` displays a list of available menus/events for the user to select. Each menu represents an event or venue configuration.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/activities/LoginMenuActivity.kt
```

## Responsibilities

1. **Load Menus** - Fetch available menus from API
2. **Download Logos** - Download and cache menu logos
3. **Display Grid** - Show menus in a 2-column grid
4. **Handle Selection** - Save selected menu and navigate

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    LoginMenuActivity                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              LoginMenuViewModel                      │   │
│  │  ┌─────────────────┐  ┌─────────────────────────┐   │   │
│  │  │ GetMenuItemsUC  │  │ DownloadMenuLogoUseCase │   │   │
│  │  └────────┬────────┘  └───────────┬─────────────┘   │   │
│  │           │                       │                  │   │
│  │           └───────────┬───────────┘                  │   │
│  │                       ▼                              │   │
│  │              ┌─────────────────┐                     │   │
│  │              │  MenuRepository │                     │   │
│  │              └─────────────────┘                     │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## ViewModel

### LoginMenuViewModel

```kotlin
@HiltViewModel
class LoginMenuViewModel @Inject constructor(
    private val getMenuItemsUseCase: GetMenuItemsUseCase,
    private val downloadMenuLogoUseCase: DownloadMenuLogoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginMenuUiState>(LoginMenuUiState.Loading)
    val uiState: StateFlow<LoginMenuUiState> = _uiState

    fun loadMenus() {
        viewModelScope.launch {
            try {
                val menus = getMenuItemsUseCase()
                _uiState.value = LoginMenuUiState.Success(menus)
                downloadLogos(menus)
            } catch (e: Exception) {
                _uiState.value = LoginMenuUiState.Error(e.message)
            }
        }
    }
}
```

### LoginMenuUiState

```kotlin
sealed class LoginMenuUiState {
    object Loading : LoginMenuUiState()
    data class Success(val menus: List<Menu>) : LoginMenuUiState()
    data class Empty(val message: String) : LoginMenuUiState()
    data class Error(val message: String?) : LoginMenuUiState()
}
```

## Domain Model

### Menu

```kotlin
data class Menu(
    val id: String,
    val label: String,
    val logo: String?,
    val startDate: String,
    val endDate: String
)
```

## UI Components

### RecyclerView Adapter

```kotlin
class LoginMenuAdapter(
    private val onMenuClick: (Menu) -> Unit
) : ListAdapter<Menu, MenuViewHolder>(MenuDiffCallback()) {
    // Grid display with logo and label
}
```

### Grid Layout
- 2 columns
- Menu logo image
- Menu label text

## Menu Selection

```kotlin
private fun onMenuClicked(menu: Menu) {
    // Save menu details to session
    SessionPrefsManagerUtils.saveSelectedMenuId(menu.id)
    SessionPrefsManagerUtils.saveMenuName(menu.label)
    SessionPrefsManagerUtils.saveMenuStartDate(menu.startDate)
    SessionPrefsManagerUtils.saveMenuEndDate(menu.endDate)
    
    // Navigate to POS selection
    val posFragment = LoginPosFragment.newInstance(menu.id)
    supportFragmentManager.beginTransaction()
        .replace(R.id.content_frame, posFragment)
        .addToBackStack("pos")
        .commit()
}
```

## Data Layer

### MenuApiService

```kotlin
interface MenuApiService {
    @GET("menu/list")
    suspend fun getMenus(): List<MenuDto>
}
```

### Logo Download

```kotlin
// Logos are downloaded and cached locally
downloadMenuLogoUseCase(menu.id, menu.logo)
```

## Navigation

| Action | Destination |
|--------|-------------|
| Menu selected | `LoginPosFragment` (embedded) or `LoginPosActivity` |

## Layout

```
res/layout/activity_login_menu.xml
```

### Key Views
| View ID | Purpose |
|---------|---------|
| `menusRecyclerView` | Grid of menus |
| `content_frame` | Fragment container |

## Fragments

| Fragment | Purpose |
|----------|---------|
| `LoginLoadingFragment` | Show while loading menus |
| `LoginPosFragment` | Embedded POS selection |

## Session Data Saved

| Key | Value |
|-----|-------|
| `menuId` | Selected menu ID |
| `menuName` | Menu label |
| `menuStartDate` | Event start date |
| `menuEndDate` | Event end date |

## See Also

- [LoginActivity](./03_LoginActivity.md)
- [LoginPosActivity](./05_LoginPosActivity.md)

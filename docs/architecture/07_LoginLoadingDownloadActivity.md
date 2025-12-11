# LoginLoadingDownloadActivity

## Overview

`LoginLoadingDownloadActivity` displays a loading screen while synchronizing all necessary data from the backend. This includes categories, products, thumbnails, menu pins, and POS session setup.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/activities/LoginLoadingDownloadActivity.kt
```

## Responsibilities

1. **Download Categories** - Sync product categories
2. **Download Products** - Sync product catalog
3. **Download Thumbnails** - Download product images
4. **Download Menu Pins** - Sync authorized PIN whitelist
5. **Set User Logged** - Mark user as logged in
6. **Configure POS** - Select and open POS session

## Process Steps

| Step | Description | Status Message |
|------|-------------|----------------|
| 1 | Download categories | "Baixando categorias..." |
| 2 | Download products | "Baixando produtos..." |
| 3 | Download thumbnails | "Baixando thumbnails..." |
| 4 | Download menu pins | "Baixando PINs autorizados..." |
| 5 | Set user logged | "Logando usuário..." |
| 6 | Configure POS | "Configurando POS..." |
| 7 | Open POS session | "Abrindo POS..." |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                  LoginLoadingDownloadActivity                    │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                LoadingDownloadViewModel                    │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                    UseCases                          │  │  │
│  │  │  • RefreshCategoriesUseCase                         │  │  │
│  │  │  • RefreshProductsUseCase                           │  │  │
│  │  │  • RefreshMenuPinsUseCase                           │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │                  Repositories                        │  │  │
│  │  │  • ProductRepository (thumbnails)                   │  │  │
│  │  │  • UserRepository (set logged)                      │  │  │
│  │  │  • PosRepository (select, open session)             │  │  │
│  │  │  • MenuPinRepository (via UseCase)                  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## ViewModel

### LoadingDownloadViewModel

```kotlin
@HiltViewModel
class LoadingDownloadViewModel @Inject constructor(
    private val refreshCategoriesUseCase: RefreshCategoriesUseCase,
    private val refreshProductsUseCase: RefreshProductsUseCase,
    private val refreshMenuPinsUseCase: RefreshMenuPinsUseCase,
    private val productRepository: ProductRepository,
    private val posRepository: PosRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadingDownloadUiState>(LoadingDownloadUiState.Idle)
    val uiState: StateFlow<LoadingDownloadUiState> = _uiState

    fun startCompleteProcess(
        menuId: String, 
        posId: String, 
        deviceId: String, 
        cashierName: String, 
        userId: String
    ) {
        viewModelScope.launch {
            try {
                // Step 1: Categories
                _uiState.value = LoadingDownloadUiState.Loading("Baixando categorias...")
                refreshCategoriesUseCase(menuId)

                // Step 2: Products
                _uiState.value = LoadingDownloadUiState.Loading("Baixando produtos...")
                refreshProductsUseCase(menuId)

                // Step 3: Thumbnails
                _uiState.value = LoadingDownloadUiState.Loading("Baixando thumbnails...")
                val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
                productRepository.downloadAndExtractThumbnails(menuId, thumbnailsDir)

                // Step 4: Menu Pins
                _uiState.value = LoadingDownloadUiState.Loading("Baixando PINs autorizados...")
                refreshMenuPinsUseCase(menuId)

                // Step 5: User Logged
                _uiState.value = LoadingDownloadUiState.Loading("Logando usuário...")
                userRepository.setUserLogged(userId, true)

                // Step 6: Select POS
                _uiState.value = LoadingDownloadUiState.Loading("Configurando POS...")
                posRepository.selectPos(posId).onFailure { throw it }

                // Step 7: Open Session
                _uiState.value = LoadingDownloadUiState.Loading("Abrindo POS...")
                posRepository.openPosSession(posId, deviceId, cashierName).onFailure { throw it }

                // Success
                _uiState.value = LoadingDownloadUiState.Success("Processo completo!")
            } catch (e: Exception) {
                _uiState.value = LoadingDownloadUiState.Error("Erro: ${e.message}")
            }
        }
    }
}
```

### LoadingDownloadUiState

```kotlin
sealed class LoadingDownloadUiState {
    object Idle : LoadingDownloadUiState()
    data class Loading(val message: String) : LoadingDownloadUiState()
    data class Success(val message: String) : LoadingDownloadUiState()
    data class Error(val message: String) : LoadingDownloadUiState()
}
```

## Activity Implementation

```kotlin
@AndroidEntryPoint
class LoginLoadingDownloadActivity : AppCompatActivity() {

    private val viewModel: LoadingDownloadViewModel by viewModels()

    @Inject
    lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_download)
        
        val statusTextView: TextView = findViewById(R.id.tvDownloadProgress)

        // Observe state
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoadingDownloadUiState.Loading -> {
                        statusTextView.text = state.message
                    }
                    is LoadingDownloadUiState.Success -> {
                        navigateProductScreen()
                    }
                    is LoadingDownloadUiState.Error -> {
                        statusTextView.text = "Erro: ${state.message}"
                    }
                    else -> {}
                }
            }
        }

        // Start process
        lifecycleScope.launch {
            val loggedUser = userDao.getAnyUserOnce()
            
            // Skip if already logged
            if (loggedUser?.isLogged == true) {
                navigateProductScreen()
                return@launch
            }

            // Get session data
            val menuId = SessionPrefsManagerUtils.getSelectedMenuId()
            val posId = SessionPrefsManagerUtils.getPosId()
            val deviceId = SessionPrefsManagerUtils.getDeviceId()
            val cashierName = SessionPrefsManagerUtils.getCashierName()
            val userId = loggedUser?.id

            if (allDataPresent(menuId, posId, deviceId, cashierName, userId)) {
                viewModel.startCompleteProcess(menuId!!, posId!!, deviceId!!, cashierName!!, userId!!)
            } else {
                showMissingDataError()
            }
        }
    }

    private fun navigateProductScreen() {
        startActivity(Intent(this, ProductsListActivity::class.java))
        finish()
    }
}
```

## Session Data Required

| Key | Source |
|-----|--------|
| `menuId` | `SessionPrefsManagerUtils.getSelectedMenuId()` |
| `posId` | `SessionPrefsManagerUtils.getPosId()` |
| `deviceId` | `SessionPrefsManagerUtils.getDeviceId()` |
| `cashierName` | `SessionPrefsManagerUtils.getCashierName()` |
| `userId` | `UserDao.getAnyUserOnce()?.id` |

## Navigation

| Condition | Destination |
|-----------|-------------|
| Already logged | `ProductsListActivity` (skip process) |
| Process success | `ProductsListActivity` |
| Process error | Stay on screen, show error |

## Layout

```
res/layout/activity_login_download.xml
```

### Key Views
| View ID | Purpose |
|---------|---------|
| `tvDownloadProgress` | Status message display |

## Error Handling

- Each step can fail independently
- Error message shows which step failed
- User stays on screen to see error

## See Also

- [LoginConfirmActivity](./06_LoginConfirmActivity.md)
- [ProductsListActivity](./08_ProductsListActivity.md)
- [Menu Pin Feature](../features/MenuPinFeature.md)

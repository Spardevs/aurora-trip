# ProductsListActivity

## Overview

`ProductsListActivity` is the main application screen after login. It displays the product catalog organized by categories using tabs and a swipeable ViewPager.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/product/activities/ProductsListActivity.kt
```

## Responsibilities

1. **Display Categories** - Show category tabs
2. **Display Products** - Show products in a grid per category
3. **Navigation Drawer** - Provide access to other app features
4. **Product Selection** - Handle product tap for cart/payment

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     ProductsListActivity                         │
│                   (extends BaseDrawerActivity)                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                      TabLayout                             │  │
│  │  [All] [Category 1] [Category 2] [Category 3] ...         │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                      ViewPager2                            │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │            CategoryProductsFragment                  │  │  │
│  │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │  │  │
│  │  │  │ Product │ │ Product │ │ Product │ │ Product │   │  │  │
│  │  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘   │  │  │
│  │  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │  │  │
│  │  │  │ Product │ │ Product │ │ Product │ │ Product │   │  │  │
│  │  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘   │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## ViewModels

### CategoryViewModel

```kotlin
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState

    fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                // Add "All" category at the beginning
                val allCategories = listOf(Category("all", "Todos")) + categories
                _uiState.value = CategoryUiState.Success(allCategories)
            }
        }
    }
}
```

### ProductViewModel

```kotlin
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductModel>>(emptyList())
    val products: StateFlow<List<ProductModel>> = _products

    fun loadProducts(categoryId: String?) {
        viewModelScope.launch {
            getProductsUseCase().collect { allProducts ->
                _products.value = if (categoryId == null || categoryId == "all") {
                    allProducts
                } else {
                    allProducts.filter { it.categoryId == categoryId }
                }
            }
        }
    }
}
```

## Domain Models

### Category

```kotlin
data class Category(
    val id: String,
    val name: String
)
```

### ProductModel

```kotlin
data class ProductModel(
    val id: String,
    val name: String,
    val price: Double,
    val thumbnailPath: String?,
    val categoryId: String,
    val stock: Int?
)
```

## UI Components

### TabLayout + ViewPager2

```kotlin
private fun setupViewPagerAndTabs() {
    val categories = categoryViewModel.categories.value
    
    viewPager.adapter = CategoryPagerAdapter(this, categories)
    
    TabLayoutMediator(tabLayout, viewPager) { tab, position ->
        tab.text = categories[position].name
    }.attach()
}
```

### CategoryPagerAdapter

```kotlin
class CategoryPagerAdapter(
    activity: FragmentActivity,
    private val categories: List<Category>
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = categories.size

    override fun createFragment(position: Int): Fragment {
        return CategoryProductsFragment.newInstance(categories[position].id)
    }
}
```

### CategoryProductsFragment

```kotlin
class CategoryProductsFragment : Fragment() {
    
    private val productViewModel: ProductViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val categoryId = arguments?.getString("categoryId")
        productViewModel.loadProducts(categoryId)
        
        lifecycleScope.launch {
            productViewModel.products.collect { products ->
                adapter.submitList(products)
            }
        }
    }
}
```

## Base Class

### BaseDrawerActivity

`ProductsListActivity` extends `BaseDrawerActivity` which provides:
- Navigation drawer
- Toolbar
- Common drawer menu items

```kotlin
class ProductsListActivity : BaseDrawerActivity() {
    
    override fun getLayoutResourceId(): Int = R.layout.activity_products_list
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewPagerAndTabs()
    }
}
```

## Navigation Drawer Items

| Item | Destination |
|------|-------------|
| Payments | `PaymentProcessingActivity` |
| NFC | `NFCActivity` |
| Printing | `PrintingActivity` |
| Refunds | `RefundActivity` |
| Logout | Clear session, return to `LoginActivity` |

## Layout

```
res/layout/activity_products_list.xml
```

### Key Views
| View ID | Type | Purpose |
|---------|------|---------|
| `tabLayout` | TabLayout | Category tabs |
| `viewPager` | ViewPager2 | Swipeable category pages |
| `content_frame` | FrameLayout | Fragment container |

## Data Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  CategoryDao    │────►│ CategoryRepo    │────►│ GetCategoriesUC │
│  (Room)         │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ CategoryViewModel│
                                                └─────────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  ProductDao     │────►│ ProductRepo     │────►│ GetProductsUC   │
│  (Room)         │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ ProductViewModel │
                                                └─────────────────┘
```

## See Also

- [LoginLoadingDownloadActivity](./07_LoginLoadingDownloadActivity.md)
- [PaymentProcessingActivity](./10_PaymentProcessingActivity.md)
- [NFCActivity](./11_NFCActivity.md)

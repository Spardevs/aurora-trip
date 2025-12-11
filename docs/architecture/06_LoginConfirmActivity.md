# LoginConfirmActivity

## Overview

`LoginConfirmActivity` displays a summary of the selected menu and POS, and prompts the user to enter their atendente name before finalizing the session.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/login/activities/LoginConfirmActivity.kt
```

## Responsibilities

1. **Display Summary** - Show selected menu, dates, and POS
2. **Collect Atendente Name** - Get the cashier/atendente name
3. **Confirm Session** - Save atendente name and proceed

## Screen Content

| Field | Source |
|-------|--------|
| Menu Name | `SessionPrefsManagerUtils.getMenuName()` |
| Event Dates | `SessionPrefsManagerUtils.getMenuStartDate()` / `getMenuEndDate()` |
| POS Name | `SessionPrefsManagerUtils.getPosName()` |
| Atendente Name | User input (EditText) |

## Implementation

### Load Session Data

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login_confirm)
    
    // Display session data
    findViewById<TextView>(R.id.menuValue).text = SessionPrefsManagerUtils.getMenuName()
    findViewById<TextView>(R.id.posValue).text = SessionPrefsManagerUtils.getPosName()
    
    // Format and display dates
    val startDate = SessionPrefsManagerUtils.getMenuStartDate()
    val endDate = SessionPrefsManagerUtils.getMenuEndDate()
    findViewById<TextView>(R.id.dateValue).text = formatDateRange(startDate, endDate)
    
    // Confirm button
    findViewById<ImageButton>(R.id.imageButton).setOnClickListener {
        loginFinish()
    }
}
```

### Confirm and Navigate

```kotlin
private fun loginFinish() {
    val name = findViewById<EditText>(R.id.nameText).text.toString()
    
    // Validate atendente name
    if (name.isBlank()) {
        showError("Por favor, insira o nome do atendente")
        return
    }
    
    // Save atendente name
    SessionPrefsManagerUtils.saveCashierName(name)
    
    // Navigate to loading/download screen
    val intent = Intent(this, LoginLoadingDownloadActivity::class.java)
    startActivity(intent)
    finish()
}
```

## Session Data

### Read
| Key | Purpose |
|-----|---------|
| `menuName` | Display menu label |
| `menuStartDate` | Display event start |
| `menuEndDate` | Display event end |
| `posName` | Display POS name |

### Write
| Key | Value |
|-----|-------|
| `cashierName` | Entered atendente name |

## Navigation

| Action | Destination |
|--------|-------------|
| Confirm button | `LoginLoadingDownloadActivity` |

## Layout

```
res/layout/activity_login_confirm.xml
```

### Key Views
| View ID | Type | Purpose |
|---------|------|---------|
| `menuValue` | TextView | Selected menu name |
| `dateValue` | TextView | Event date range |
| `posValue` | TextView | Selected POS name |
| `nameText` | EditText | Atendente name input |
| `imageButton` | ImageButton | Confirm button |

## Validation

- Atendente name cannot be empty
- Shows error message if validation fails

## Date Formatting

```kotlin
private fun formatDateRange(start: String?, end: String?): String {
    // Format: "22/11/2025 - 23/11/2025"
    val startFormatted = formatDate(start)
    val endFormatted = formatDate(end)
    return "$startFormatted - $endFormatted"
}
```

## See Also

- [LoginPosActivity](./05_LoginPosActivity.md)
- [LoginLoadingDownloadActivity](./07_LoginLoadingDownloadActivity.md)

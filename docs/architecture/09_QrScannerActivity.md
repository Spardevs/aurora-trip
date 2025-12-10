# QrScannerActivity

## Overview

`QrScannerActivity` provides a camera-based QR code scanner for login authentication. It uses the ZXing library for barcode detection.

## Location

```
app/src/main/java/br/com/ticpass/pos/presentation/scanners/activities/QrScannerActivity.kt
```

## Responsibilities

1. **Camera Preview** - Display camera feed
2. **QR Detection** - Continuously scan for QR codes
3. **Permission Handling** - Request camera permission if needed
4. **Return Result** - Send scanned QR text back to caller

## Implementation

```kotlin
class QrScannerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QR_TEXT = "extra_qr_text"
    }

    private lateinit var barcodeView: BarcodeView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startScanner() else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result == null) return
            barcodeView.pause()
            
            val intent = Intent().apply { 
                putExtra(EXTRA_QR_TEXT, result.text) 
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.zxing_qr_scanner)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startScanner()
        }
    }

    private fun startScanner() {
        barcodeView.decodeContinuous(barcodeCallback)
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
```

## Activity Contract

### QrScannerContract

```kotlin
class QrScannerContract : ActivityResultContract<Unit, String?>() {
    
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, QrScannerActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        return if (resultCode == Activity.RESULT_OK) {
            intent?.getStringExtra(QrScannerActivity.EXTRA_QR_TEXT)
        } else {
            null
        }
    }
}
```

### Usage in LoginActivity

```kotlin
private val qrScannerLauncher = registerForActivityResult(QrScannerContract()) { qrText ->
    qrText?.let { 
        qrViewModel.processQrCode(it) 
    }
}

// Launch scanner
fun onQrButtonClick() {
    qrScannerLauncher.launch(Unit)
}
```

## ZXing Library

### BarcodeView

The `BarcodeView` from ZXing provides:
- Camera preview
- Continuous barcode detection
- Multiple format support (QR, EAN, etc.)

### Dependency

```gradle
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
```

## Lifecycle Management

| Lifecycle | Action |
|-----------|--------|
| `onResume()` | Resume camera preview |
| `onPause()` | Pause camera preview |
| `onDestroy()` | Release camera resources |

## Result Handling

| Result | Meaning |
|--------|---------|
| `RESULT_OK` + `EXTRA_QR_TEXT` | QR code successfully scanned |
| `RESULT_CANCELED` | User cancelled or permission denied |

## Layout

```
res/layout/activity_qr_scanner.xml
```

### Key Views
| View ID | Type | Purpose |
|---------|------|---------|
| `zxing_qr_scanner` | BarcodeView | Camera preview and scanner |

## Permission Flow

```
┌─────────────────────────┐
│  Check CAMERA permission │
└───────────┬─────────────┘
            │
    ┌───────▼───────┐
    │   Granted?    │
    └───────┬───────┘
            │
   ┌────────┴────────┐
   │                 │
   ▼                 ▼
[YES]              [NO]
   │                 │
   ▼                 ▼
Start           Request
Scanner         Permission
                    │
            ┌───────▼───────┐
            │   Granted?    │
            └───────┬───────┘
                    │
           ┌────────┴────────┐
           │                 │
           ▼                 ▼
        [YES]              [NO]
           │                 │
           ▼                 ▼
        Start            Cancel &
        Scanner          Finish
```

## See Also

- [LoginActivity](./03_LoginActivity.md)
- [QrLoginViewModel](./03_LoginActivity.md#viewmodel)

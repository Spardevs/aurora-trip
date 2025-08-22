# Point of Sale System

A comprehensive Android Point of Sale (POS) system built with modern architecture patterns, supporting multiple payment acquirers, NFC operations, printing, and queue-based processing.

## Architecture Overview

The application is built using:
- **Multi-flavor architecture** supporting different payment acquirers (PagSeguro, Stone)
- **Generic queue management system** for processing payments, NFC operations, printing, and refunds
- **Modular SDK architecture** with flavor-specific implementations
- **Reactive programming** with Kotlin Flows and Coroutines
- **Type-safe interfaces** ensuring compile-time safety across flavors


## Core Features

### Payment Processing
- **Multi-acquirer support**: PagSeguro and Stone payment processors
- **Multiple payment methods**: Card transactions, PIX, cash payments
- **Receipt printing**: Automatic customer and merchant receipt generation
- **Transaction management**: Queue-based processing with error handling

### NFC Operations
- **MIFARE Classic support**: Complete tag reading and writing capabilities
- **Comprehensive tag mapping**: Structured data extraction with sector/block details
- **Key management**: Automatic key fallback (Key A/B) for authentication
- **Memory analysis**: Total and used memory calculations per sector

### Queue Management System
- **Generic processing**: Type-safe queue system for any operation type
- **Reactive state management**: Real-time processing updates via Kotlin Flows
- **Persistence strategies**: Configurable immediate or in-memory persistence
- **Interactive processing**: Built-in user input handling during operations
- **Error resilience**: Comprehensive retry/skip/abort error handling

### Printing System
- **Receipt printing**: Customer and merchant receipt generation
- **Document printing**: Support for various document types
- **Queue-based processing**: Reliable print job management

### Refund Processing
- **Transaction reversals**: Complete refund processing system
- **Error handling**: Proper error management for failed refunds 

## Technical Architecture

### SDK Architecture
- **Flavor-based implementation**: Build-time selection of payment acquirers
- **Type-safe providers**: Generic interfaces with flavor-specific implementations
- **Singleton management**: Shared SDK instances across all providers
- **Central access point**: `AcquirerSdk` object for unified provider access

### Queue System
- **Processor-agnostic design**: Reusable for any processing type
- **Hybrid persistence**: Fast in-memory operations with optional storage
- **Interactive workflows**: Built-in support for user input during processing
- **Comprehensive error handling**: Retry, skip, and abort options

### Current Implementations
- **Payment Queue**: Card transactions, PIX, cash with receipt printing
- **NFC Queue**: MIFARE Classic tag operations with comprehensive mapping
- **Print Queue**: Receipt and document printing management
- **Refund Queue**: Transaction reversal processing  

## Documentation

- **[Queue Management System](app/src/main/java/pos/queue/README.md)** - Complete queue system documentation
- **[SDK Architecture](app/src/main/java/pos/sdk/README.md)** - Multi-flavor SDK implementation guide
- **[Payment Processing](app/src/main/java/pos/queue/docs/payments/README.md)** - Payment queue documentation
- **[Print System](app/src/main/java/pos/queue/docs/printing/README.md)** - Printing queue documentation


## Development

### Build Flavors
- **PagSeguro**: Uses PlugPag SDK for payment processing
- **Stone**: Uses Stone SDK with UserModel for payment processing

### Key Technologies
- **Kotlin**: Primary development language with coroutines
- **Android Architecture Components**: ViewModel, LiveData, Room
- **Dependency Injection**: Hilt for dependency management
- **Reactive Programming**: Kotlin Flows for state management
- **Type Safety**: Generic interfaces with compile-time safety

## Key Permissions

- **NFC**: `android.permission.NFC` for MIFARE Classic tag operations
- **Internet**: `android.permission.INTERNET` for payment processing and API communication
- **Network State**: `android.permission.ACCESS_NETWORK_STATE` for connectivity checks
- **Storage**: External storage permissions for receipt and document storage
- **Bluetooth**: For printer connectivity and communication




## Architecture Benefits

1. **Extensibility**: Easy to add new payment acquirers or processing types
2. **Type Safety**: Compile-time safety prevents runtime errors
3. **Maintainability**: Clear separation of concerns with modular design
4. **Testability**: Each component can be independently tested
5. **Performance**: Hybrid in-memory/persistent queue processing
6. **Reliability**: Comprehensive error handling and recovery mechanisms

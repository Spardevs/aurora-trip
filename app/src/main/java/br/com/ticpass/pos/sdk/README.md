# Acquirer SDK Architecture

This document describes the architecture of the Acquirer SDK system, which is designed to support multiple acquirer flavors using a source set-based provider pattern with flavor-specific type safety and shared SDK instances.

## Architecture Overview

The Acquirer SDK uses a build flavor-based architecture that allows for different payment processors (PagSeguro, Stone, etc.) to be used without changing the client code. This is achieved through:

1. **Common interfaces** in the main source set
2. **Provider objects** that serve as access points for implementations
3. **Flavor-specific implementations** in separate source sets
4. **Build system selection** of the appropriate implementation at build time
5. **Singleton SDK instances** shared across providers within each flavor
6. **Flavor-specific type safety** ensuring proper typing and compile-time safety

## Architecture Diagram

```mermaid
graph LR
    %% Main section - Interfaces
    subgraph "Interfaces"
        AcquirerProvider[AcquirerProvider]
        BasePaymentProvider[BasePaymentProvider]
        BasePrintingProvider[BasePrintingProvider]
        BaseNFCProvider[BaseNFCProvider]
        
        AcquirerProvider --> BasePaymentProvider
        AcquirerProvider --> BasePrintingProvider
        AcquirerProvider --> BaseNFCProvider
    end
    
    %% Main source set implementations
    subgraph "Main Source Set"
        AcquirerSdk[AcquirerSdk]
        PaymentProviderMain[PaymentProvider]
        PrintingProviderMain[PrintingProvider]
        NFCProviderMain[NFCProvider]
        
        AcquirerSdk -- payment --> PaymentProviderMain
        AcquirerSdk -- printing --> PrintingProviderMain
        AcquirerSdk -- nfc --> NFCProviderMain
    end
    
    %% Connect interfaces to main implementations
    BasePaymentProvider --> PaymentProviderMain
    BasePrintingProvider --> PrintingProviderMain
    BaseNFCProvider --> NFCProviderMain
    
    %% Build system and flavor-specific implementations
    subgraph "Build Flavors"
        BuildFlavor[Build Flavor Selection]
        
        subgraph "PagSeguro"
            PlugPag[PlugPag SDK]
            SdkInstancePag[SdkInstance]
            PaymentProviderPag[PaymentProvider]
            PrintingProviderPag[PrintingProvider]
            NFCProviderPag[NFCProvider]
            
            SdkInstancePag -- "Singleton" --> PlugPag
            PaymentProviderPag -- "uses" --> SdkInstancePag
            PrintingProviderPag -- "uses" --> SdkInstancePag
            NFCProviderPag -- "uses" --> SdkInstancePag
        end
        
        subgraph "Stone"
            UserModel[UserModel SDK]
            SdkInstanceStn[SdkInstance]
            PaymentProviderStn[PaymentProvider]
            PrintingProviderStn[PrintingProvider]
            NFCProviderStn[NFCProvider]
            
            SdkInstanceStn -- "Singleton" --> UserModel
            PaymentProviderStn -- "uses" --> SdkInstanceStn
            PrintingProviderStn -- "uses" --> SdkInstanceStn
            NFCProviderStn -- "uses" --> SdkInstanceStn
        end
        
        subgraph "Acquirer_n"
            SDK_n[Custom SDK]
            SdkInstanceN[SdkInstance]
            PaymentProviderN[PaymentProvider]
            PrintingProviderN[PrintingProvider]
            NFCProviderN[NFCProvider]
            
            SdkInstanceN -- "Singleton" --> SDK_n
            PaymentProviderN -- "uses" --> SdkInstanceN
            PrintingProviderN -- "uses" --> SdkInstanceN
            NFCProviderN -- "uses" --> SdkInstanceN
        end
        
        BuildFlavor --> PagSeguro
        BuildFlavor --> Stone
        BuildFlavor --> Acquirer_n
    end
    
    %% Override relationships
    PaymentProviderMain -.Override.-> PaymentProviderPag
    PrintingProviderMain -.Override.-> PrintingProviderPag
    NFCProviderMain -.Override.-> NFCProviderPag
    
    PaymentProviderMain -.Override.-> PaymentProviderStn
    PrintingProviderMain -.Override.-> PrintingProviderStn
    NFCProviderMain -.Override.-> NFCProviderStn
    
    PaymentProviderMain -.Override.-> PaymentProviderN
    PrintingProviderMain -.Override.-> PrintingProviderN
    NFCProviderMain -.Override.-> NFCProviderN
    
    %% App usage
    subgraph "App Usage"
        AcquirerPaymentProcessor[AcquirerPaymentProcessor]
    end
    
    AcquirerPaymentProcessor --> AcquirerSdk
    
    %% Styling
    classDef interface fill:#f9f,stroke:#333,stroke-width:1px;
    classDef main fill:#bbf,stroke:#333,stroke-width:1px;
    classDef usage fill:#ddd,stroke:#333,stroke-width:1px;
    classDef sdk fill:#ffd,stroke:#333,stroke-width:1px;
    
    class AcquirerProvider,BasePaymentProvider,BasePrintingProvider,BaseNFCProvider interface;
    class AcquirerSdk,PaymentProviderMain,PrintingProviderMain,NFCProviderMain main;
    class AcquirerPaymentProcessor usage;
    class PlugPag,UserModel,SDK_n sdk;
    
    %% Style the subgraphs
    style Interfaces fill:#fff,stroke:#333,stroke-width:1px;
    style 'Main Source Set' fill:#eef,stroke:#333,stroke-width:1px;
    style 'Build Flavors' fill:#efe,stroke:#333,stroke-width:1px;
    style 'App Usage' fill:#fee,stroke:#333,stroke-width:1px;
    style PagSeguro fill:#bfb,stroke:#333,stroke-width:1px;
    style Stone fill:#fbb,stroke:#333,stroke-width:1px;
    style Acquirer_n fill:#bbf,stroke:#333,stroke-width:1px;
```

## How It Works

1. **Interface Layer**:
   - `AcquirerProvider` - Base interface for all provider types
   - Specific interfaces extend this base: `BasePaymentProvider<T>`, `BasePrintingProvider<T>`, `BaseNFCProvider<T>`
   - Type parameter `<T>` allows flavor-specific SDK type to be enforced

2. **Access Layer**:
   - `AcquirerSdk` - Central access point for all providers
   - Exposes properties: `payment`, `printing`, `nfc` to access provider instances
   - Each property returns a flavor-specific typed instance

3. **SDK Instance Management**:
   - `SdkInstance` - Singleton object that manages the flavor-specific SDK instance
   - Ensures only one SDK instance is created and shared across all providers
   - Each flavor provides its own implementation (e.g., PagSeguro → PlugPag, Stone → UserModel)

4. **Provider Objects**:
   - `PaymentProvider`, `PrintingProvider`, `NFCProvider` - Singleton objects that provide type-safe access
   - Each provider maintains its own initialization state
   - All providers delegate to the shared `SdkInstance` for the actual SDK instance

5. **Flavor Implementation**:
   - Each flavor (PagSeguro, Stone, etc.) provides its own implementations
   - These override the providers from the main source set
   - All use the same class names, allowing seamless replacement

6. **Type Safety**:
   - Providers expose flavor-specific types enabling proper type inference
   - This ensures compile-time safety for flavor-specific features

7. **Build System Selection**:
   - Android Gradle build system selects the appropriate source set based on build flavor
   - No runtime conditionals needed in code

## Usage

```kotlin
// Initialize all providers with application context
AcquirerSdk.initialize(applicationContext)

// Access a provider - flavor-specific types inferred automatically
val paymentProvider = AcquirerSdk.payment

// Use the provider (implementation automatically selected based on build flavor)
if (paymentProvider.isInitialized()) {
    // Get the flavor-specific SDK instance
    val sdkInstance = paymentProvider.getInstance()
    
    // Use the flavor-specific SDK features with proper typing
    // Example for PagSeguro:
    // sdkInstance.doPlugPagSpecificOperation() 
    // 
    // Example for Stone:
    // sdkInstance.doStoneSpecificOperation()
}
```

## Extensibility

The architecture is designed to be easily extensible:

1. **Adding new provider types** - Create a new interface extending `AcquirerProvider<T>` and add a corresponding accessor in `AcquirerSdk`

2. **Adding new flavors** - Create a new source set for the flavor and implement:
   - `SdkInstance` - Singleton for managing the flavor's SDK instance
   - Provider implementations that use the shared `SdkInstance`

3. **Enhancing type safety** - Add flavor-specific extension methods or interfaces to the provider objects to expose additional functionality while maintaining type safety

## Benefits of this Architecture

1. **True Singleton SDK** - Only one SDK instance is created and shared across all providers (payment, printing, NFC)

2. **Type Safety** - Proper typing ensures compile-time safety for flavor-specific features

3. **Separation of Concerns** - Each provider is responsible for a specific domain (payment, printing, NFC)

4. **Build-time Selection** - No runtime conditionals or reflection needed

5. **Clean API** - Clients interact with a consistent API regardless of flavor

6. **Testability** - Each component can be easily mocked and tested independently

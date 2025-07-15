# Acquirer SDK Architecture

This document describes the architecture of the Acquirer SDK system, which is designed to support multiple acquirer flavors using a source set-based provider pattern.

## Architecture Overview

The Acquirer SDK uses a build flavor-based architecture that allows for different payment processors (PagSeguro, Stone, etc.) to be used without changing the client code. This is achieved through:

1. **Common interfaces** in the main source set
2. **Provider objects** that serve as access points for implementations
3. **Flavor-specific implementations** in separate source sets
4. **Build system selection** of the appropriate implementation at build time

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
            PaymentProviderPag[PaymentProvider]
            PrintingProviderPag[PrintingProvider]
            NFCProviderPag[NFCProvider]
        end
        
        subgraph "Stone"
            PaymentProviderStn[PaymentProvider]
            PrintingProviderStn[PrintingProvider]
            NFCProviderStn[NFCProvider]
        end
        
        subgraph "Acquirer_n"
            PaymentProviderCie[PaymentProvider]
            PrintingProviderCie[PrintingProvider]
            NFCProviderCie[NFCProvider]
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
    
    PaymentProviderMain -.Override.-> PaymentProviderCie
    PrintingProviderMain -.Override.-> PrintingProviderCie
    NFCProviderMain -.Override.-> NFCProviderCie
    
    %% App usage
    subgraph "App Usage"
        AcquirerPaymentProcessor[AcquirerPaymentProcessor]
    end
    
    AcquirerPaymentProcessor --> AcquirerSdk
    
    %% Styling
    classDef interface fill:#f9f,stroke:#333,stroke-width:1px;
    classDef main fill:#bbf,stroke:#333,stroke-width:1px;
    classDef usage fill:#ddd,stroke:#333,stroke-width:1px;
    
    class AcquirerProvider,BasePaymentProvider,BasePrintingProvider,BaseNFCProvider interface;
    class AcquirerSdk,PaymentProviderMain,PrintingProviderMain,NFCProviderMain main;
    class AcquirerPaymentProcessor usage;
    
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
   - Specific interfaces extend this base: `BasePaymentProvider`, `BasePrintingProvider`, `BaseNFCProvider`

2. **Access Layer**:
   - `AcquirerSdk` - Central access point for all providers
   - Exposes properties: `payment`, `printing`, `nfc` to access provider instances

3. **Provider Objects**:
   - `PaymentProvider`, `PrintingProvider`, `NFCProvider` - Singleton objects that return instances
   - Main source set contains default implementations that throw `NotImplementedError`

4. **Flavor Implementation**:
   - Each flavor (PagSeguro, Stone, etc.) provides its own implementations
   - These override the providers from the main source set
   - All use the same class names, allowing seamless replacement

5. **Build System Selection**:
   - Android Gradle build system selects the appropriate source set based on build flavor
   - No runtime conditionals needed in code

## Usage

```kotlin
// Initialize all providers with application context
AcquirerSdk.initialize(applicationContext)

// Access a provider
val paymentProvider = AcquirerSdk.payment

// Use the provider (implementation automatically selected based on build flavor)
if (paymentProvider.isInitialized()) {
    // Use the provider
}
```

## Extensibility

The architecture is designed to be easily extensible:

1. **Adding new provider types** - Create a new interface extending `AcquirerProvider` and add a corresponding accessor in `AcquirerSdk`

2. **Adding new flavors** - Create a new source set for the flavor and implement the provider interfaces

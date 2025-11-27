# productapp

## Descrição

Aplicativo Android em Kotlin (módulo `app`) seguindo princípios de Clean Architecture: separação em camadas de apresentação, domínio e dados. O app consome uma API externa, persiste dados localmente com Room e expõe telas usando Activities/Fragments + ViewModel.

## Estrutura do projeto

```
productapp/
├─ app/
│  ├─ build.gradle
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ AndroidManifest.xml
│  │  ├─ java/ (ou kotlin/)
│  │  │  └─ com/example/productapp/
│  │  │     ├─ App.kt
│  │  │     ├─ core/
│  │  │     │  ├─ di/
│  │  │     │  │  └─ AppModule.kt
│  │  │     │  ├─ network/
│  │  │     │  │  └─ NetworkClient.kt
│  │  │     │  └─ util/
│  │  │     │    └─ ResultWrapper.kt
│  │  │     ├─ presentation/
│  │  │     │  └─ product/
│  │  │     │     ├─ ProductActivity.kt
│  │  │     │     ├─ ProductFragment.kt
│  │  │     │     ├─ ProductViewModel.kt
│  │  │     │     ├─ ProductAdapter.kt
│  │  │     │     └─ ProductUiState.kt
│  │  │     ├─ domain/
│  │  │     │  └─ product/
│  │  │     │     ├─ model/
│  │  │     │     │  └─ Product.kt
│  │  │     │     ├─ repository/
│  │  │     │     │  └─ ProductRepository.kt
│  │  │     │     └─ usecase/
│  │  │     │        ├─ GetProductsUseCase.kt
│  │  │     │        └─ GetProductByIdUseCase.kt
│  │  │     └─ data/
│  │  │        └─ product/
│  │  │           ├─ local/
│  │  │           │  ├─ db/
│  │  │           │  │  └─ AppDatabase.kt
│  │  │           │  ├─ dao/
│  │  │           │  │  └─ ProductDao.kt
│  │  │           │  └─ entity/
│  │  │           │    └─ ProductEntity.kt
│  │  │           ├─ remote/
│  │  │           │  ├─ service/
│  │  │           │  │  └─ ProductApiService.kt
│  │  │           │  └─ dto/
│  │  │           │    └─ ProductDto.kt
│  │  │           ├─ mapper/
│  │  │           │  └─ ProductMapper.kt
│  │  │           ├─ datasource/
│  │  │           │  ├─ ProductLocalDataSource.kt
│  │  │           │  └─ ProductRemoteDataSource.kt
│  │  │           └─ repository/
│  │  │              └─ ProductRepositoryImpl.kt
│  │  ├─ res/
│  │  │  ├─ layout/
│  │  │  │  ├─ activity_product.xml
│  │  │  │  ├─ product_fragment.xml
│  │  │  │  └─ item_product.xml
│  │  │  ├─ menu/
│  │  │  │  └─ product_menu.xml
│  │  │  ├─ drawable/
│  │  │  │  └─ ic_product.xml
│  │  │  └─ values/
│  │  │     ├─ strings.xml
│  │  │     ├─ colors.xml
│  │  │     └─ styles.xml
│  └─ proguard-rules.pro
├─ build.gradle (project)
├─ settings.gradle
└─ gradle/
```

## Explicação das pastas e arquivos

### Raiz do projeto

* `build.gradle` (nível do projeto): módulos, repositórios e plugins aplicados globalmente.

* `settings.gradle`: registro dos módulos do Gradle.

* `gradle/`: scripts e wrapper do Gradle.

### app/

Módulo principal do app.

* `build.gradle` (módulo `app`): dependências do app (Retrofit, Room, Coroutines, Hilt/Koin, etc.), configurações de compilação, minSdk/targetSdk.

* `proguard-rules.pro`: regras de obfuscação para release.

* `src/main/AndroidManifest.xml`: declarações de Activities, permissões e configurações do app.

### com/example/productapp/

Ponto de entrada e configuração do app.

* `App.kt`: classe Application — inicialização de bibliotecas, DI, logging, WorkManager, etc.

### core/

Código reutilizável em todo o app.

* `di/AppModule.kt`: módulos de injeção de dependência (provides para NetworkClient, Database, DAOs, Repositories, UseCases).

* `network/NetworkClient.kt`: configuração Retrofit/OkHttp (baseURL, interceptors, timeout).

* `util/ResultWrapper.kt`: classe utilitária para encapsular sucesso/erro (padrão Resource/Result/Either).

### presentation/product/

Camada de UI relacionada ao recurso "product".

* `ProductActivity.kt`: Activity (podendo hospedar NavHostFragment ou apenas o Fragment).

* `ProductFragment.kt`: Fragment responsável pela UI de listagem/detalhes.

* `ProductViewModel.kt`: ViewModel que expõe estados (StateFlow/LiveData) e executa UseCases.

* `ProductAdapter.kt`: Adapter do RecyclerView para exibir itens `Product`.

* `ProductUiState.kt`: modelo de estado da UI (loading, success, error, empty).

Boas práticas: usar `StateFlow` + `viewModelScope` com coroutines, separar eventos/side-effects.

### domain/product/

Camada de domínio — regras de negócio e contratos (independente de frameworks).

* `model/Product.kt`: modelo de domínio (POJO/DTO do domínio).

* `repository/ProductRepository.kt`: interface contratual para acessar produtos (getProducts(), getById()).

* `usecase/GetProductsUseCase.kt`, `GetProductByIdUseCase.kt`: casos de uso que orquestram chamadas do repositório.

Motivo: facilita testes unitários e separação entre lógica e implementação.

### data/product/

Implementações de persistência, rede e mapeamento.

* local/

    * `db/AppDatabase.kt`: definição do RoomDatabase e versionamento.

    * `dao/ProductDao.kt`: queries SQLite/Room (inserir, atualizar, buscar por id, listar). Preferir retornar `Flow<List<ProductEntity>>`.

    * `entity/ProductEntity.kt`: entidade Room (anotações @Entity, @PrimaryKey, índices, TypeConverters se necessário).

* remote/

    * `service/ProductApiService.kt`: interface Retrofit com endpoints (GET /products, GET /products/{id}, etc.).

    * `dto/ProductDto.kt`: classes de resposta JSON (mapeamento das respostas externas).

* `mapper/ProductMapper.kt`: conversões entre `ProductDto <-> ProductEntity <-> Product` (extensões ou funções utilitárias).

* datasource/

    * `ProductLocalDataSource.kt`: abstrai operações de banco (usa DAOs).

    * `ProductRemoteDataSource.kt`: abstrai chamadas Retrofit.

* repository/

    * `ProductRepositoryImpl.kt`: implementação da `ProductRepository` usando os data sources e mapper. Deve implementar estratégia de cache e atualização (ex.: NetworkBoundResource).

Sugestão: garantir que `ProductRepositoryImpl` retorne `Flow<Resource<List<Product>>>` e trate erros/exceções.

### res/

Recursos Android: layouts, drawables, strings, cores, estilos e menus.

* `layout/`: arquivos de layout XML (Activity/Fragment/Item).

* `menu/`: menus de toolbar ou context menu.

* `drawable/`: ícones vetoriais e shapes.

* `values/`: strings, cores, dimensões, temas.

### test/

Testes unitários. Criar também `androidTest/` para testes instrumentados.

## Recomendações e melhorias para a refatoração (v2)

1. Arquitetura e modularização

* Manter a separação Presentação/Domínio/Dados. Considere modularizar por feature (ex.: `:feature-product`) e módulos compartilhados (`:core`, `:data`, `:domain`) para builds mais rápidos e independência.

* Tornar `domain` livre de dependências Android.

1. Injeção de dependência

* Use Hilt (ou Koin) para simplificar injeção. Centralize providers em `core/di`.

* Garantir escopos corretos (Singleton para Retrofit/DB, ViewModelScope para ViewModel).

1. Conectividade e rede

* Use Retrofit + OkHttp com interceptors (logging, autenticação, retry).

* Implementar um padrão Network Bound Resource: primeiro exibir cache do Room (Flow), então tentar atualizar via rede aplicando transações.

* Tratar erros com um `ResultWrapper`/`Resource` com estados claros (Success, Loading, Error).

1. Concurrency e reatividade

* Coroutines + Flow em toda a stack (DAOs retornando Flow, UseCases retornando Flow, ViewModel expondo StateFlow).

* Evitar LiveData se o projeto for 100% Kotlin/Coroutines.

1. Persistência (Room)

* Adicionar `@Index`, constraints e migrations versionadas no `AppDatabase`.

* Usar `TypeConverters` para tipos complexos.

* Retornar `Flow` do DAO para atualizações reativas.

1. Sincronização e política de cache

* Implementar estratégias: TTL, cache-first, or force-refresh.

* Para sync em background, usar WorkManager (ex.: sincronizar diariamente ou quando o app abre).

1. UI e navegação

* Considere Navigation Component (single-Activity + NavHost).

* Isolar View logic no ViewModel; UI apenas observa `UiState`.

* Uso de Paging 3 para listas grandes.

1. Testes e qualidade

* Cobertura unitária para UseCases e RepositoryImpl (mock de data sources).

* Testes instrumentados para DAOs e flows do Room.

* Ferramentas: detekt, ktlint, Sonar, CI (GitHub Actions/GitLab CI).

1. Observabilidade e manutenção

* Logging estruturado, métricas de performance, e crash reporting.

* Scripts de DB migration e backup durante deploys.

1. Outras melhorias práticas

* Usar sealed classes para estados e eventos.

* Centralizar constantes e strings.

* Proteger chamadas de rede com tempo limite e retry/backoff.

* Documentar contrato de API (ex.: OpenAPI/Swagger se possível).

## Exemplo mínimo de fluxo de dados (recomendado)

1. UI (Fragment) observa `ProductViewModel.uiState: StateFlow<ProductUiState>`.

2. `ProductViewModel` chama `GetProductsUseCase`.

3. `GetProductsUseCase` coleta `ProductRepository.getProducts()` (retornando Flow<Resource<List<Product>>>).

4. `ProductRepositoryImpl`:

* lê `ProductLocalDataSource.getProducts()` (Flow from Room),

* dispara requisição `ProductRemoteDataSource.fetchProducts()` via Retrofit,

* mapeia DTO -> Entity -> salva no DB,

* Room emite novo Flow com dados atualizados.

1. UI atualiza automaticamente com os dados mais recentes.

## Como usar / comandos úteis

* Build: execute `./gradlew assembleDebug`

* Rodar testes unitários: `./gradlew test`

* Rodar instrumented tests: `./gradlew connectedAndroidTest`

* Limpar DB local (apenas em dev): implementar um comando de debug no App ou usar `adb shell` para deletar app data.

## Próximos passos sugeridos

* Gerar um template de módulo `feature-product` com interfaces e classes vazias.

* Implementar exemplo de `NetworkBoundResource` (padrão) em Kotlin.

* Criar `AppModule.kt` com providers para Retrofit, Room e Repositórios (Hilt).

* Gerar exemplos de testes unitários para UseCase e RepositoryImpl.
# Architecture Documentation - Samurai Jack

## 🏛️ High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                     │
│         (Jetpack Compose UI Components & Screens)           │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                      ViewModel Layer                         │
│        (State Management & Business Logic Orchestration)     │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                     Domain Layer                            │
│              (Interfaces & Business Rules)                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                      Data Layer                             │
│              (Repository & Data Sources)                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
         ┌─────────────┴──────────────┐
         │                            │
    ┌────▼─────┐          ┌──────────▼────┐
    │  Local   │          │    Remote     │
    │DataSource│          │  DataSource   │
    └──────────┘          │ (API/Network) │
                          └───────────────┘
```

## 📦 Module Structure

### Presentation Layer
**Location**: `ui/`

#### Components
- **Composables**: Reusable UI building blocks
- **Screens**: Full-screen UI compositions
- **ViewModels**: State holders and business logic coordinators

#### Key Files
```
ui/
├── theme/
│   ├── Theme.kt          # Material 3 theme definition
│   ├── Typography.kt     # Typography system
│   └── DesignTokens.kt   # Spacing, radius, colors, etc.
├── components/
│   ├── CommonComponents.kt
│   ├── GameComponents.kt
│   └── MenuComponents.kt
└── screens/
    ├── GameScreen.kt
    ├── MenuScreen.kt
    └── ScoreScreen.kt
```

### Domain Layer
**Location**: `domain/`

Defines contracts and business rules:
- **Repository Interfaces**: Define data contracts
- **Domain Models**: Core business objects
- **Use Cases**: Specific business operations

```
domain/
├── repository/
│   ├── GameRepository.kt
│   └── AudioRepository.kt
└── model/
    ├── Game.kt
    ├── GameState.kt
    └── Player.kt
```

### Data Layer
**Location**: `data/`

Implements data operations and caching:
- **Repositories**: Concrete repository implementations
- **Data Sources**: Local and remote data access
- **Entities**: Database entities and API response models

```
data/
├── repository/
│   ├── GameRepositoryImpl.kt
│   └── AudioRepositoryImpl.kt
├── datasource/
│   ├── GameLocalDataSource.kt
│   ├── GameRemoteDataSource.kt
│   └── AudioDataSource.kt
└── entity/
    ├── GameEntity.kt
    └── PlayerEntity.kt
```

### Engine Layer
**Location**: `engine/`

Core game engine and graphics:

```
engine/
├── GameEngine.kt      # Main game loop
├── GLRenderer.kt      # OpenGL rendering
├── Shader.kt          # GLSL shader programs
└── Math3D.kt          # 3D math utilities
```

### Audio Layer
**Location**: `audio/`

Sound and music management:

```
audio/
└── SoundManager.kt    # Audio playback control
```

## 🔄 Data Flow

### Feature: Load and Display Game

```
User Action
    ↓
GameScreen Composable
    ↓
GameViewModel.loadGame()
    ↓
GameRepository.getGame()
    ↓
┌─────────────────────────┐
│ Try Remote Data Source  │
└────────────┬────────────┘
             │
          Success / Failure
             │
    ┌────────▼────────┐
    │    Success      │    Failure
    │                 │       │
    ↓                 ↓       ↓
 Return Data      Try Local  Handle
    │              Source     Error
    │                 │       │
    └─────────┬───────┴───────┘
              │
        Update UI State
              │
    ┌─────────▼──────────┐
    │ StateFlow emits    │
    │ new UI State       │
    └────────┬───────────┘
             │
    Recompose Composable
             │
    Display Updated UI
```

## 🎯 MVVM Pattern Implementation

### ViewModel
Manages UI state and handles user interactions:

```kotlin
class GameViewModel(
    private val gameRepository: GameRepository,
    private val audioManager: AudioManager
) : ViewModel() {
    
    // State
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Idle)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    
    // Events
    fun onStartGame(difficulty: GameDifficulty) {
        viewModelScope.launch {
            _uiState.value = GameUiState.Loading
            try {
                val game = gameRepository.initializeGame(difficulty)
                audioManager.playStartSound()
                _uiState.value = GameUiState.GameStarted(game)
            } catch (e: Exception) {
                _uiState.value = GameUiState.Error(e.message.orEmpty())
            }
        }
    }
}
```

### Repository
Abstracts data access:

```kotlin
interface GameRepository {
    suspend fun initializeGame(difficulty: GameDifficulty): Game
    suspend fun saveGameProgress(progress: GameProgress)
    suspend fun loadGameProgress(): GameProgress?
}

class GameRepositoryImpl(
    private val localDataSource: GameLocalDataSource,
    private val remoteDataSource: GameRemoteDataSource
) : GameRepository {
    
    override suspend fun initializeGame(difficulty: GameDifficulty): Game {
        return remoteDataSource.getGameConfig(difficulty)
            .toCachedLocalGame()
    }
}
```

### UI (Composables)
Observes state and reacts to changes:

```kotlin
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is GameUiState.Idle -> StartScreen { difficulty ->
            viewModel.onStartGame(difficulty)
        }
        is GameUiState.Loading -> LoadingScreen()
        is GameUiState.GameStarted -> GameplayScreen(
            game = (uiState as GameUiState.GameStarted).game
        )
        is GameUiState.Error -> ErrorScreen(
            message = (uiState as GameUiState.Error).message
        )
    }
}
```

## 🔌 Dependency Injection (Hilt)

### Module Definition
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideGameRepository(
        local: GameLocalDataSource,
        remote: GameRemoteDataSource
    ): GameRepository = GameRepositoryImpl(local, remote)
}

@Module
@InstallIn(ActivityComponent::class)
object ViewModelModule {
    
    @Provides
    fun provideGameViewModel(
        repository: GameRepository
    ): GameViewModel = GameViewModel(repository)
}
```

### Usage in Composables
```kotlin
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel()
) {
    // ViewModel automatically injected
}
```

## 🔀 State Management with StateFlow

### Best Practices
1. **Immutable State**: Never mutate state directly
2. **Single Source of Truth**: One StateFlow per feature
3. **Clear Events**: Explicit user interactions
4. **Side Effects**: Managed in ViewModel with viewModelScope

```kotlin
// ✅ Good: Immutable state updates
_gameState.update { currentState ->
    currentState.copy(score = currentState.score + points)
}

// ❌ Bad: Direct mutation
_gameState.value.score += points
```

## 🧪 Testing Architecture

### Unit Tests
Test ViewModels and business logic in isolation:

```kotlin
@Test
fun testGameInitialization() {
    val mockRepository = mockk<GameRepository>()
    coEvery { mockRepository.initializeGame(any()) } returns testGame
    
    val viewModel = GameViewModel(mockRepository)
    viewModel.onStartGame(GameDifficulty.NORMAL)
    
    assertEquals(GameUiState.GameStarted(testGame), viewModel.uiState.value)
}
```

### Integration Tests
Test data layer and repository interactions:

```kotlin
@Test
suspend fun testGameRepositoryLoadsSavedGame() {
    val repository = GameRepositoryImpl(localSource, remoteSource)
    val saved = repository.loadGameProgress()
    
    assertNotNull(saved)
    assertEquals("test-game", saved?.gameId)
}
```

### UI Tests
Test composables with test utilities:

```kotlin
@Test
fun testGameScreenDisplaysLoading() {
    composeRule.setContent {
        GameScreen(viewModel = mockViewModel)
    }
    
    composeRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
}
```

## 📊 Entity-Relationship Diagram

```
┌──────────┐         ┌──────────┐
│  Player  │────┐    │   Game   │
│          │    │    │          │
└──────────┘    │    └────┬─────┘
                │         │
            ┌───▼─────────▼───┐
            │   GameSession   │
            │  (Junction)     │
            └─────────────────┘
                    │
            ┌───────▼────────┐
            │   GameProgress │
            │   (Saves)      │
            └────────────────┘
```

## 🔐 Security Considerations

1. **Data Validation**: Validate all inputs
2. **Error Handling**: Never expose sensitive data in errors
3. **Secure Storage**: Use EncryptedSharedPreferences for sensitive data
4. **Network Security**: Use HTTPS and certificate pinning
5. **Code Obfuscation**: Enabled for release builds

## 📈 Scalability Patterns

### Adding New Features
1. Define Domain interfaces
2. Implement Data layer
3. Create ViewModel
4. Build Composable UI
5. Add tests at each layer

### Adding New Data Sources
1. Implement DataSource interface
2. Update Repository implementation
3. Handle cache/fallback logic
4. Add appropriate error handling

---

**Last Updated**: 2024-06-24

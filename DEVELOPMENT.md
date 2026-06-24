# Development Guidelines - Samurai Jack

## 🎯 Core Principles

1. **Clean Code**: Write code that is easy to read, understand, and maintain
2. **Quality First**: Prioritize code quality over speed
3. **Testing**: Write tests alongside features
4. **Documentation**: Document complex logic and APIs
5. **Consistency**: Follow established patterns and conventions

## 📝 Code Style & Standards

### Kotlin Code Format

```kotlin
// Good: Clear naming and structure
class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow<GameState>(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    fun startGame() {
        viewModelScope.launch {
            _gameState.value = GameState.Loading
            try {
                val result = gameRepository.initializeGame()
                _gameState.value = GameState.Success(result)
            } catch (e: Exception) {
                _gameState.value = GameState.Error(e.message.orEmpty())
            }
        }
    }
}
```

### Naming Conventions

| Element | Pattern | Example |
|---------|---------|---------|
| Classes | PascalCase | `GameEngine`, `ModelRenderer` |
| Functions | camelCase | `startGame()`, `renderFrame()` |
| Variables | camelCase | `playerScore`, `isLoading` |
| Constants | UPPER_SNAKE_CASE | `MAX_PLAYERS`, `DEFAULT_TIMEOUT` |
| Private members | Leading underscore | `_internalState` |
| Composables | PascalCase | `GameScreen()`, `ScoreBoard()` |

### File Organization

```
com.thigazhini_labs.samuraijack/
├── data/
│   ├── repository/    # Repository implementations
│   ├── datasource/    # Local and remote data sources
│   └── model/         # Data models
├── domain/
│   ├── repository/    # Repository interfaces
│   └── model/         # Domain models
├── ui/
│   ├── theme/         # Material Design theming
│   ├── components/    # Reusable UI components
│   ├── screens/       # Screen-level composables
│   └── viewmodel/     # ViewModels
├── engine/            # Core game engine
├── audio/             # Audio system
└── util/              # Utility functions
```

## 🏗️ Architecture Patterns

### MVVM Pattern

```kotlin
// ViewModel Layer
class GameViewModel(
    private val repository: GameRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadGame(gameId: String) {
        viewModelScope.launch(dispatcher) {
            try {
                val game = repository.getGame(gameId)
                _uiState.value = UiState.Success(game)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message.orEmpty())
            }
        }
    }
}

// Repository Pattern
interface GameRepository {
    suspend fun getGame(id: String): Game
    suspend fun saveGame(game: Game)
}

class GameRepositoryImpl(
    private val localDataSource: GameLocalDataSource,
    private val remoteDataSource: GameRemoteDataSource
) : GameRepository {
    
    override suspend fun getGame(id: String): Game =
        try {
            remoteDataSource.fetchGame(id)
        } catch (e: Exception) {
            localDataSource.getGame(id)
        }
}

// UI Layer (Compose)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is UiState.Loading -> LoadingState()
        is UiState.Success -> GameContent(game = (uiState as UiState.Success).game)
        is UiState.Error -> ErrorState(message = (uiState as UiState.Error).message)
    }
}
```

## ✅ Composition & Testing

### Unit Testing

```kotlin
@Test
fun testGameInitialization() {
    // Arrange
    val mockRepository = mockk<GameRepository>()
    coEvery { mockRepository.getGame("test-id") } returns testGame
    val viewModel = GameViewModel(mockRepository)
    
    // Act
    viewModel.loadGame("test-id")
    
    // Assert
    assertEquals(UiState.Success(testGame), viewModel.uiState.value)
}
```

### Compose UI Testing

```kotlin
@Test
fun testGameScreenDisplaysContent() {
    composeRule.setContent {
        GameScreen(viewModel = mockViewModel)
    }
    
    composeRule.onNodeWithTag("game_content").assertIsDisplayed()
}
```

## 🎨 Compose Patterns

### State Management

```kotlin
@Composable
fun GameScreen() {
    var gameState by remember { mutableStateOf(GameState.Idle) }
    
    LaunchedEffect(Unit) {
        gameState = GameState.Loading
        // Load game data
        gameState = GameState.Ready
    }
    
    when (gameState) {
        GameState.Loading -> LoadingScreen()
        GameState.Ready -> GameContent()
        GameState.Error -> ErrorScreen()
    }
}
```

### Custom Composables

```kotlin
@Composable
fun SamuraiGameCard(
    game: Game,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    SamuraiCard(modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = game.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

## 📋 Error Handling

### Best Practices

```kotlin
// ✅ Good: Explicit error handling
try {
    val result = apiCall()
    processResult(result)
} catch (e: IOException) {
    Timber.e(e, "Network error")
    handleNetworkError()
} catch (e: SerializationException) {
    Timber.e(e, "Data parsing error")
    handleParsingError()
}

// ❌ Bad: Swallowing exceptions
try {
    val result = apiCall()
    processResult(result)
} catch (e: Exception) {
    // Silent failure
}
```

## 🔍 Code Review Checklist

Before submitting a PR, ensure:

- [ ] Code follows naming conventions
- [ ] No magic numbers (use constants)
- [ ] Functions are single responsibility
- [ ] Error handling is comprehensive
- [ ] Unit tests are included
- [ ] Documentation is updated
- [ ] No debug logging left in
- [ ] No hardcoded strings (use resources)
- [ ] Performance considerations addressed
- [ ] Accessibility requirements met

## 🚀 Performance Guidelines

### General Tips

1. **Lazy Loading**: Load data only when needed
2. **Caching**: Cache expensive computations
3. **Composable Recomposition**: Minimize unnecessary recompositions
4. **Memory**: Avoid memory leaks with proper lifecycle management
5. **Threading**: Use coroutines for async operations

### Anti-Patterns

```kotlin
// ❌ Bad: Heavy computation in Compose function
@Composable
fun ExpensiveComposable() {
    val result = expensiveComputation() // Called every recomposition!
    Text(result)
}

// ✅ Good: Lazy computation
@Composable
fun EfficientComposable() {
    val result = remember { expensiveComputation() }
    Text(result)
}
```

## 📚 Documentation Standards

### KDoc Comments

```kotlin
/**
 * Starts the game with the specified difficulty level.
 *
 * @param difficulty The game difficulty (EASY, NORMAL, HARD)
 * @param playerName The name of the player
 * @return A coroutine job representing the game session
 * @throws IllegalArgumentException if difficulty is invalid
 *
 * Example usage:
 * ```kotlin
 * val job = gameEngine.startGame(Difficulty.NORMAL, "Jack")
 * ```
 */
suspend fun startGame(difficulty: Difficulty, playerName: String): Job
```

### Comment Guidelines

```kotlin
// ✅ Good: Explains WHY
// We need to debounce search input to avoid excessive API calls
val searchResults = searchQuery.debounce(300).flatMapLatest { query ->
    repository.search(query)
}

// ❌ Bad: Explains WHAT (obvious from code)
// Set loading to true
isLoading = true
```

## 🔗 Additional Resources

- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Design Guidelines](https://developer.android.com/design)
- [Compose Documentation](https://developer.android.com/jetpack/compose/documentation)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Clean Code by Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)

## 📞 Support

For questions or clarifications, please:
1. Check existing documentation
2. Search GitHub issues
3. Ask in team discussions
4. Create a new issue if needed

---

**Last Updated**: 2024-06-24

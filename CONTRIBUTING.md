# Contributing to Samurai Jack

Thank you for your interest in contributing to the Samurai Jack project! This guide will help you get started.

## 🤝 Code of Conduct

Be respectful, inclusive, and professional. We are committed to providing a welcoming environment for all contributors.

## 🚀 Getting Started

### Prerequisites
- Android Studio Jellyfish or later
- Kotlin 2.0+
- Java 17+
- Git

### Setup Development Environment

```bash
# Clone the repository
git clone https://github.com/vengatmacuser/samurai-jack.git
cd samurai-jack

# Run setup script
./setup.sh

# Sync Gradle
./gradlew sync

# Verify setup
./gradlew build
```

## 📋 Before You Start

1. Check existing [Issues](https://github.com/vengatmacuser/samurai-jack/issues)
2. Review [Pull Requests](https://github.com/vengatmacuser/samurai-jack/pulls)
3. Read [DEVELOPMENT.md](DEVELOPMENT.md) for coding standards
4. Review [ARCHITECTURE.md](ARCHITECTURE.md) for project structure

## 🔄 Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name

# Branch naming conventions:
# feature/description - New feature
# bugfix/description - Bug fix
# docs/description - Documentation
# refactor/description - Code refactoring
# test/description - Tests
```

### 2. Implement Your Changes

Follow the [Development Guidelines](DEVELOPMENT.md):
- Write clean, well-documented code
- Follow Kotlin naming conventions
- Add unit tests
- Use design tokens for UI consistency

### 3. Run Quality Checks

```bash
# Format code
./gradlew ktlintFormat

# Run static analysis
./gradlew detekt

# Run tests
./gradlew testDebugUnitTest

# Or run all checks at once
./gradlew ktlintCheck detekt testDebugUnitTest
```

### 4. Commit Your Changes

```bash
# Stage your changes
git add .

# Commit with descriptive message
git commit -m "feat: Add feature description

- Detail about change 1
- Detail about change 2

Closes #123"

# Pre-commit hooks will run automatically
```

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style changes
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Test changes
- `chore`: Build/config changes

#### Example
```
feat(game): Add difficulty levels

- Added EASY, NORMAL, HARD levels
- Implement difficulty-based scoring
- Add difficulty selection UI

Closes #45
```

### 5. Push and Create Pull Request

```bash
# Push branch
git push origin feature/your-feature-name

# Create PR on GitHub
# Fill out PR template
# Request reviewers
# Link related issues
```

## 📝 Pull Request Guidelines

### PR Title
Clear, concise, following commit message format:
- ✅ `feat: Add player leaderboard`
- ✅ `fix: Crash on game exit`
- ❌ `Update stuff`
- ❌ `asdf`

### PR Description
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Documentation update
- [ ] Code refactoring
- [ ] Performance improvement

## Testing
Describe testing performed:
- Unit tests: ✅ All tests pass
- Manual testing: Tested on Android 14

## Screenshots (if applicable)
Include before/after screenshots for UI changes

## Checklist
- [ ] Code follows style guidelines
- [ ] No warnings from detekt/ktlint
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes
- [ ] Tested on min SDK level
```

## ✅ Quality Standards

### Code Review Criteria

Your PR will be reviewed for:

1. **Correctness**
   - Logic is sound
   - No memory leaks
   - Proper error handling

2. **Style**
   - Follows Kotlin conventions
   - No detekt/ktlint warnings
   - Consistent formatting

3. **Testing**
   - Unit tests included
   - Good coverage
   - Tests are meaningful

4. **Documentation**
   - Code is commented where needed
   - Public APIs documented
   - README updated if needed

5. **Performance**
   - No unnecessary allocations
   - Efficient algorithms
   - No blocking operations

6. **Security**
   - No hardcoded credentials
   - Proper input validation
   - Secure network communication

## 🧪 Testing Guidelines

### Unit Tests
```kotlin
@Test
fun testScenarioSuccess() {
    // Arrange
    
    // Act
    
    // Assert
}

@Test
fun testScenarioError() {
    // Test error conditions
}
```

### Coverage
- Aim for 80%+ coverage on new code
- Critical paths should have >90% coverage
- Run: `./gradlew testDebugUnitTestCoverage`

## 📚 Documentation

Update documentation when:
- Adding new features
- Changing API contracts
- Modifying architecture
- Adding significant functionality

Files to update:
- `README.md` - Feature overview
- `DEVELOPMENT.md` - Dev guidelines
- `ARCHITECTURE.md` - Architecture changes
- Code comments - Complex logic
- KDoc comments - Public APIs

## 🐛 Reporting Issues

### Before Creating an Issue
- Search existing issues
- Check closed issues
- Verify it's not a duplicate

### Issue Template

**Title**: Clear, concise description

**Description**:
```
### Environment
- Android version:
- Device:
- App version:

### Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

### Expected Behavior
What should happen

### Actual Behavior
What actually happens

### Screenshots
If applicable

### Logs
Include relevant logs
```

## 🔍 Code Review Process

1. **Automated Checks** (GitHub Actions)
   - Detekt static analysis
   - KtLint formatting
   - Unit tests
   - Build verification

2. **Manual Review**
   - Code quality
   - Architecture compliance
   - Test coverage
   - Documentation

3. **Approval**
   - At least one approval required
   - All checks must pass
   - Conflicts must be resolved

4. **Merge**
   - Squash and merge (keep history clean)
   - Rebase and merge (preserve commits)

## 📞 Getting Help

- **Discord**: Join our community server
- **GitHub Discussions**: Ask questions
- **Issues**: Report problems
- **Email**: developers@samuraijack.dev

## 📖 Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Android Documentation](https://developer.android.com/)
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)

## 🎓 Development Tips

### Useful Commands
```bash
# Build and install
./gradlew installDebug

# Run app on device
adb shell am start -n com.thigazhini_labs.samuraijack/.MainActivity

# View logs
adb logcat -s "GameEngine"

# Debug specific test
./gradlew testDebugUnitTest --tests "com.thigazhini_labs.samuraijack.*GameViewModelTest"

# Generate coverage report
./gradlew testDebugUnitTestCoverage
```

### Performance Profiling
- Use Android Profiler in Android Studio
- Profile CPU, memory, and network
- Use Jetpack Macrobenchmark for UI tests

## ✨ Recognition

Contributors will be:
- Added to CONTRIBUTORS.md
- Mentioned in release notes
- Recognized in project documentation

## 📜 License

By contributing, you agree that your contributions will be licensed under the project's license.

---

**Thank you for contributing to Samurai Jack! 🗡️**

Last Updated: 2024-06-24

# 🏀 Basketball Plugin - Developer Documentation

## 📋 Project Overview

Het Basketball Plugin is een geavanceerde Minecraft plugin voor Minecraft 1.12.2 die realistische basketball gameplay implementeert met WorldGuard integratie, multiplayer support en uitgebreide physics.

## 🏗️ Architecture

### Core Components

#### 1. **BasketballPlugin** (Main Class)
- **Location**: `src/main/java/nl/djorr/basketball/BasketballPlugin.java`
- **Purpose**: Hoofdklasse die alle managers initialiseert en lifecycle beheert
- **Key Features**:
  - Plugin lifecycle management (onEnable/onDisable)
  - Manager initialization
  - Event listener registration
  - Command registration

#### 2. **Managers** (`managers/` package)
- **BasketballManager**: Basketball entities en physics
- **ConfigManager**: Configuration handling
- **DataManager**: Data persistence (scores, regions)
- **HologramManager**: DecentHolograms integratie
- **ScoreManager**: Score tracking en win conditions

#### 3. **Objects** (`objects/` package)
- **Basketball**: Basketball entity wrapper
- **BasketballRegion**: Region/court management

#### 4. **Listeners** (`listeners/` package)
- **BasketballListener**: Basketball gameplay events
- **RegionListener**: WorldGuard region events

#### 5. **Utils** (`utils/` package)
- **BasketballAnimation**: Animation system
- **BasketballTextureUtil**: Basketball texture handling
- **ItemBuilder**: Item creation utilities
- **ItemUtil**: Item validation utilities

## 🔧 Current Implementation Status

### ✅ **Completed Features**

#### Core System
- ✅ **Plugin lifecycle** - Proper enable/disable handling
- ✅ **Configuration system** - YAML config met alle settings
- ✅ **WorldGuard integration** - Region detection en management
- ✅ **Basketball entities** - ArmorStand-based basketball system
- ✅ **Physics system** - Realistische bounce physics
- ✅ **Scoring system** - Hopper-based scoring detection
- ✅ **Multiplayer support** - Multiple players per region
- ✅ **Auto-pickup/drop** - Automatic basketball management

#### Visual & Audio
- ✅ **Particle effects** - Basketball trail en score explosions
- ✅ **Sound effects** - Basketball bounce en score sounds
- ✅ **Title notifications** - Score announcements
- ✅ **Basketball textures** - Custom basketball skull textures
- ✅ **Animations** - Basketball spawn en score animations

#### Data Management
- ✅ **Score persistence** - Player scores saved between sessions
- ✅ **Region data** - Basketball regions saved/loaded
- ✅ **Player tracking** - Players tracked within regions
- ✅ **Win/loss tracking** - Player statistics

#### Commands & Permissions
- ✅ **Admin commands** - Region en leaderboard management
- ✅ **Permission system** - basketball.admin en basketball.play
- ✅ **Help system** - Command help en usage

### 🚧 **Partially Implemented**

#### Leaderboard System
- 🚧 **DecentHolograms integration** - Basic integration exists
- 🚧 **Real-time updates** - Needs optimization
- 🚧 **Statistics display** - Basic implementation

#### Performance Optimization
- 🚧 **Physics optimization** - Basic optimization implemented
- 🚧 **Memory management** - Basketball cleanup on disable
- 🚧 **Debug logging** - Configurable debug system

### ❌ **Missing Features**

#### Advanced Gameplay
- ❌ **Tournament system** - Multi-region tournaments
- ❌ **Team mode** - Team-based basketball
- ❌ **Power-ups** - Special basketball abilities
- ❌ **Custom courts** - Different court types

#### UI/UX Improvements
- ❌ **GUI menus** - Inventory-based configuration
- ❌ **Scoreboard integration** - Bukkit scoreboard display
- ❌ **Boss bar** - Game progress display
- ❌ **Action bar** - Real-time game info

#### Advanced Features
- ❌ **Replay system** - Game replay functionality
- ❌ **Statistics API** - External statistics access
- ❌ **Webhook integration** - Discord/webhook notifications
- ❌ **Economy integration** - Vault support

## 🐛 Known Issues & Bugs

### Critical Issues
1. **Memory Leaks**
   - **Issue**: Basketball entities blijven soms bestaan na region cleanup
   - **Location**: `BasketballManager.cleanupBasketballsOnStartup()`
   - **Status**: Partially fixed, needs monitoring

2. **Concurrent Modification**
   - **Issue**: ConcurrentModificationException in physics loop
   - **Location**: `BasketballListener.startPhysicsTask()`
   - **Status**: Needs thread-safe implementation

3. **Region Detection**
   - **Issue**: Players sometimes not detected in regions
   - **Location**: `RegionListener.onPlayerMove()`
   - **Status**: Intermittent issue, needs investigation

### Performance Issues
1. **Physics Lag**
   - **Issue**: Physics calculations cause lag with many basketballs
   - **Location**: `BasketballManager.handlePhysics()`
   - **Solution**: Implement physics tick limiting

2. **Particle Overload**
   - **Issue**: Too many particles cause client lag
   - **Location**: `BasketballListener.createScoreExplosion()`
   - **Solution**: Reduce particle count and add distance checks

3. **Memory Usage**
   - **Issue**: Basketball objects not properly garbage collected
   - **Location**: Various managers
   - **Solution**: Implement proper cleanup cycles

### Minor Issues
1. **Texture Loading**
   - **Issue**: Basketball textures sometimes don't load properly
   - **Location**: `BasketballTextureUtil.applyBasketballTexture()`
   - **Status**: Intermittent, needs better error handling

2. **Command Feedback**
   - **Issue**: Some commands lack proper feedback
   - **Location**: `BasketballCommand.handleRegionCreate()`
   - **Status**: Needs better error messages

3. **Configuration**
   - **Issue**: Some config values not properly validated
   - **Location**: `ConfigManager.loadConfig()`
   - **Status**: Needs input validation

## 🔮 Planned Improvements

### Short Term (v1.1.0)
- [ ] **Fix memory leaks** - Proper entity cleanup
- [ ] **Optimize physics** - Reduce tick rate for performance
- [ ] **Improve error handling** - Better exception handling
- [ ] **Add configuration validation** - Validate config values
- [ ] **Fix concurrent modification** - Thread-safe collections

### Medium Term (v1.2.0)
- [ ] **Tournament system** - Multi-region competitions
- [ ] **Team mode** - 2v2 basketball games
- [ ] **GUI menus** - Inventory-based configuration
- [ ] **Statistics API** - External access to player stats
- [ ] **Webhook integration** - Discord notifications

### Long Term (v2.0.0)
- [ ] **Replay system** - Record and replay games
- [ ] **Custom courts** - Different basketball court types
- [ ] **Power-ups** - Special basketball abilities
- [ ] **Economy integration** - Vault support for betting
- [ ] **Mobile app** - Companion app for statistics

## 🛠️ Development Setup

### Prerequisites
- **Java 8** (required for Minecraft 1.12.2)
- **Maven 3.6+** (dependency management)
- **IDE** (IntelliJ IDEA recommended)
- **Git** (version control)

### Dependencies
```xml
<!-- Core Dependencies -->
<dependency>
    <groupId>com.destroystokyo.paper</groupId>
    <artifactId>paper-spigot-1.12.2</artifactId>
    <version>1.12.2-R0.1-SNAPSHOT</version>
    <scope>system</scope>
</dependency>

<!-- External Libraries -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.38</version>
</dependency>

<dependency>
    <groupId>io.github.bananapuncher714</groupId>
    <artifactId>nbteditor</artifactId>
    <version>7.19.8</version>
</dependency>

<!-- WorldGuard Integration -->
<dependency>
    <groupId>com.sk89q.worldguard</groupId>
    <artifactId>worldguard</artifactId>
    <version>7.0.9</version>
    <scope>system</scope>
</dependency>

<!-- Hologram Support -->
<dependency>
    <groupId>eu.decentsoftware.holograms</groupId>
    <artifactId>DecentHolograms</artifactId>
    <version>2.9.2</version>
    <scope>system</scope>
</dependency>
```

### Build Process
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package JAR
mvn package

# Install to local repository
mvn install
```

## 📊 Code Quality

### Code Structure
- **Package organization**: Logical separation of concerns
- **Naming conventions**: Consistent Java naming
- **Documentation**: Javadoc comments on public methods
- **Error handling**: Try-catch blocks where appropriate

### Areas for Improvement
1. **Thread Safety**: Implement proper synchronization
2. **Memory Management**: Better object lifecycle management
3. **Error Handling**: More comprehensive exception handling
4. **Testing**: Add unit tests for core functionality
5. **Documentation**: More inline code documentation

### Performance Considerations
1. **Physics Optimization**: Reduce unnecessary calculations
2. **Memory Usage**: Implement object pooling
3. **Network Optimization**: Reduce packet overhead
4. **Database Queries**: Optimize data persistence

## 🔍 Debugging

### Debug Mode
Enable debug mode in `config.yml`:
```yaml
debug:
  enabled: true
  log_region_checks: true
```

### Common Debug Scenarios
1. **Basketball not spawning**: Check region detection
2. **Physics not working**: Check entity creation
3. **Scoring not detected**: Check hopper placement
4. **Memory issues**: Monitor entity count

### Log Analysis
```java
// Enable debug logging
plugin.getLogger().info("Debug: " + debugInfo);

// Check entity count
plugin.getLogger().info("Basketball count: " + basketballs.size());

// Monitor performance
long startTime = System.currentTimeMillis();
// ... code ...
long endTime = System.currentTimeMillis();
plugin.getLogger().info("Operation took: " + (endTime - startTime) + "ms");
```

## 🤝 Contributing Guidelines

### Code Style
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Naming**: camelCase for variables, PascalCase for classes
- **Comments**: Javadoc for public methods

### Commit Messages
```
feat: add tournament system
fix: resolve memory leak in basketball cleanup
docs: update README with new features
refactor: optimize physics calculations
test: add unit tests for scoring system
```

### Pull Request Process
1. **Create feature branch** from `main`
2. **Implement changes** with proper testing
3. **Update documentation** if needed
4. **Submit PR** with detailed description
5. **Address review comments** promptly

## 📚 Additional Resources

### Documentation
- [Minecraft 1.12.2 API Documentation](https://hub.spigotmc.org/javadocs/spigot/)
- [WorldGuard Documentation](https://worldguard.enginehub.org/)
- [DecentHolograms Documentation](https://github.com/DecentSoftware-eu/DecentHolograms)

### Testing
- **Unit Tests**: JUnit 4 for core functionality
- **Integration Tests**: Test with actual Minecraft server
- **Performance Tests**: Monitor memory and CPU usage

### Deployment
- **Development**: Local server for testing
- **Staging**: Test server for validation
- **Production**: Live server deployment

---

**�� Happy coding! 🏀** 
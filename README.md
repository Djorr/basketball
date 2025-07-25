# 🏀 Basketball Plugin

Een geavanceerde Minecraft basketball plugin voor Minecraft 1.12.2 met realistische physics, multiplayer support en WorldGuard integratie.

## 📋 Features

### 🎮 Gameplay
- **Realistische basketball physics** met bounce mechanics
- **Multiplayer basketball** - meerdere spelers kunnen tegelijk spelen
- **Automatische score detectie** via hoppers
- **Basketball spawnen** als blokken op de grond
- **Auto-pickup** wanneer spelers dichtbij komen
- **Auto-drop** wanneer spelers te lang stilstaan of lopen

### 🏟️ Region System
- **WorldGuard integratie** voor basketball courts
- **Automatische hoop detectie** (hoppers)
- **Backboard detectie** (stained glass)
- **Player tracking** binnen regions
- **Score tracking** per speler

### 🎯 Scoring System
- **Hopper-based scoring** - basketball moet door hopper vallen
- **Points per basket** configuratie
- **Game win condition** (10 punten)
- **Winner announcements** met titles
- **Score persistence** tussen sessies

### 🎨 Visual Effects
- **Particle effects** tijdens basketball beweging
- **Score explosions** bij scoring
- **Basketball animations** bij spawnen
- **Realistische bounce particles**
- **Title notifications** bij scoring

### 📊 Leaderboards
- **DecentHolograms integratie** voor leaderboards
- **Player statistics** tracking
- **Win/loss tracking** per speler
- **Real-time updates**

## 🚀 Installation

### Vereisten
- **Minecraft Server**: 1.12.2
- **PaperSpigot**: 1.12.2-R0.1-SNAPSHOT
- **WorldGuard**: 7.0.9
- **WorldEdit**: 6.1.9
- **DecentHolograms**: 2.9.2
- **NBTEditor**: 7.19.8

### Installatie Stappen
1. **Download dependencies** naar `libs/` folder:
   - PaperSpigot-1.12.2-R0.1-SNAPSHOT-latest.jar
   - WorldGuard-1.12.2.jar
   - WorldEdit.jar
   - DecentHolograms-2.9.2.jar

2. **Compileer het project**:
   ```bash
   mvn clean package
   ```

3. **Plaats de JAR** in je plugins folder

4. **Start je server** - de plugin zal automatisch laden

## ⚙️ Configuration

### Basketball Settings
```yaml
basketball:
  item:
    material: SKULL_ITEM
    data: 3
    name: "&6Basketball"
    lore:
      - "&7Throw this basketball into the hoop!"
      - "&7Use Q to throw the ball"
  
  physics:
    max_bounces: 5
    bounce_height: 0.8
    bounce_decay: 0.7
    throw_velocity: 2.0
    throw_arc: 0.8
    pickup_range: 4.0
```

### Scoring Settings
```yaml
scoring:
  points_per_basket: 2
  title_duration: 60
  title_fade_in: 10
  title_fade_out: 20
```

### Hoop Settings
```yaml
hoop:
  detection_radius: 1.5
  score_height: 2.0
  backboard_material: STAINED_GLASS
  backboard_data: 11
```

## 🎮 Commands

### Admin Commands
- `/basketball help` - Toon help menu
- `/basketball region create <name>` - Maak nieuwe basketball region
- `/basketball region delete <name>` - Verwijder basketball region
- `/basketball region list [page]` - Toon alle regions
- `/basketball leaderboard create <region>` - Maak leaderboard voor region
- `/basketball leaderboard delete <region>` - Verwijder leaderboard
- `/basketball leaderboard list [page]` - Toon alle leaderboards

### Permissions
- `basketball.admin` - Toegang tot alle admin commands (default: op)
- `basketball.play` - Toegang tot basketball gameplay (default: true)

## 🏗️ Basketball Court Setup

### 1. WorldGuard Region
Maak eerst een WorldGuard region voor je basketball court:
```
/region define basketball_court
/region addmember basketball_court <player>
```

### 2. Basketball Court Bouwen
- **Hoops**: Plaats hoppers op gewenste hoogte
- **Backboards**: Plaats stained glass blokken achter de hoppers
- **Court**: Bouw je basketball court binnen de region

### 3. Plugin Region Registreren
```
/basketball region create basketball_court
```

### 4. Leaderboard Toevoegen
```
/basketball leaderboard create basketball_court
```

## 🎯 How to Play

### Basketball Ophalen
1. **Stap in de basketball region**
2. **Links-klik** op een basketball blok om het op te pakken
3. **Basketball verschijnt** in je inventory

### Basketball Gooien
1. **Druk Q** om basketball te gooien
2. **Basketball vliegt** met realistische physics
3. **Mik op de hopper** om te scoren

### Scoring
- **Basketball moet door hopper vallen** om te scoren
- **2 punten per basket** (configureerbaar)
- **10 punten om te winnen**
- **Winner krijgt title** en game reset

### Multiplayer
- **Meerdere spelers** kunnen tegelijk spelen
- **Iedereen kan basketball oppakken**
- **Scores worden apart bijgehouden**
- **Auto-pickup** werkt voor alle spelers

## 🔧 Troubleshooting

### Basketball Spawnt Niet
- **Check of je in region staat**
- **Check of er al een basketball is**
- **Check debug logs** voor errors

### Basketball Detectie Werkt Niet
- **Check WorldGuard region** bestaat
- **Check hoppers** zijn geplaatst
- **Check backboards** zijn stained glass
- **Reload plugin** met `/basketball reload`

### Performance Issues
- **Reduce physics tick rate** in config
- **Disable debug logging** in productie
- **Limit aantal basketballs** per region

## 📝 Changelog

### v1.0.0
- ✅ Realistische basketball physics
- ✅ Multiplayer support
- ✅ WorldGuard integratie
- ✅ Hopper-based scoring
- ✅ Auto-pickup/drop system
- ✅ Particle effects
- ✅ Leaderboards
- ✅ Score persistence

## 🤝 Contributing

1. **Fork het project**
2. **Maak feature branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit changes** (`git commit -m 'Add AmazingFeature'`)
4. **Push naar branch** (`git push origin feature/AmazingFeature`)
5. **Open Pull Request**

## 📄 License

Dit project is gelicenseerd onder de MIT License - zie [LICENSE](LICENSE) file voor details.

## 👨‍💻 Author

**Djorr** - *Initial work* - [Basketball Plugin](https://github.com/djorr/basketball)

## 🙏 Acknowledgments

- **WorldGuard team** voor region system
- **DecentHolograms** voor hologram support
- **NBTEditor** voor item NBT handling
- **Minecraft community** voor feedback en testing

---

**🏀 Veel plezier met basketballen! 🏀** 
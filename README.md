<div align="center">

# ⚔️ CombatAPI

**A powerful and lightweight Combat Log API for Minecraft plugins**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8+-green.svg)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](#)

*Track player combat states with ease and precision*

[Features](#-features) • [Quick Start](#-quick-start) • [API Reference](#-api-reference) • [Configuration](#-configuration)

</div>

---

## ✨ Features

- 🎯 **Combat Tracking** - Monitor players in combat with their attackers
- ⏱️ **Time Management** - Get precise combat time remaining
- 🚪 **Combat Logging** - Handle player disconnections during combat
- 👥 **Bulk Operations** - Retrieve all combat players and pairs
- 🎨 **Action Bar Support** - Real-time combat status display
- ⚡ **High Performance** - Asynchronous updates with thread safety
- 🔧 **Configurable** - Customizable duration and messages
- 📦 **Easy Integration** - Simple shading into other plugins

## 🚀 Quick Start

### Installation

1. Download the latest release from [Releases](../../releases)
2. Place the JAR file in your `plugins` folder
3. Restart your server
4. Configure the plugin in `config.yml`

### Basic Usage

```java
import me.hussainbeast.combatapi.api.CombatAPI;
import me.hussainbeast.combatapi.api.CombatAPIProvider;

// Get the API instance
CombatAPI api = CombatAPIProvider.getAPI();

// Put a player in combat
api.enterCombat(victim, attacker);

// Check if player is in combat
if (api.isInCombat(player)) {
    long timeLeft = api.getCombatTimeRemaining(player);
    player.sendMessage("Combat ends in " + timeLeft + " seconds!");
}
```

## 📚 API Reference

### Core Methods

<details>
<summary><strong>🎯 Combat State Management</strong></summary>

#### `enterCombat(Player victim, Player attacker)`
Puts a player into combat with a specific attacker.

```java
api.enterCombat(victim, attacker);
```

**Features:**
- Cancels existing combat timers
- Creates new combat timer with configured duration
- Sends combat enter message
- Stores attacker information and start time

#### `leaveCombat(Player player)`
Removes a player from combat immediately.

```java
api.leaveCombat(player);
```

**Features:**
- Cancels combat timer
- Sends combat leave message
- Clears action bar display
- Removes combat data

#### `clearCombat(Player player)`
Alias for `leaveCombat()` - provides alternative method name.

#### `clearAllCombat()`
Removes all players from combat and clears all data.

```java
api.clearAllCombat(); // Useful for server restart
```

</details>

<details>
<summary><strong>🔍 Combat Status Checking</strong></summary>

#### `isInCombat(Player player)`
Checks if a player is currently in combat.

```java
if (api.isInCombat(player)) {
    player.sendMessage("You are in combat!");
}
```

#### `isInCombat(UUID playerUUID)`
UUID-based version for checking combat status.

```java
boolean inCombat = api.isInCombat(playerUUID);
```

</details>

<details>
<summary><strong>👥 Player Relationships</strong></summary>

#### `getLastAttacker(Player player)` / `getAttacker(Player victim)`
Returns who last attacked the specified player.

```java
Player attacker = api.getAttacker(victim);
if (attacker != null) {
    victim.sendMessage("You were attacked by " + attacker.getName());
}
```

#### `getVictim(Player attacker)`
Returns who the attacker is currently fighting.

```java
Player victim = api.getVictim(attacker);
if (victim != null) {
    attacker.sendMessage("You are fighting " + victim.getName());
}
```

**Note:** All methods have UUID-based variants for when you only have player UUIDs.

</details>

<details>
<summary><strong>⏱️ Time Management</strong></summary>

#### `getCombatTimeRemaining(Player player)`
Returns remaining combat time in seconds.

```java
long timeLeft = api.getCombatTimeRemaining(player);
player.sendMessage("Combat ends in " + timeLeft + " seconds");
```

#### `getCombatDuration()` / `setCombatDuration(int seconds)`
Get or set the global combat duration.

```java
int duration = api.getCombatDuration(); // Get current duration
api.setCombatDuration(15); // Set to 15 seconds
```

**Note:** Setting duration updates config file and affects all future combat entries.

</details>

<details>
<summary><strong>🎨 Action Bar Management</strong></summary>

#### `isActionBarEnabled()` / `setActionBarEnabled(boolean enabled)`
Manage action bar display for combat timers.

```java
if (api.isActionBarEnabled()) {
    // Action bars are showing combat timers
}

api.setActionBarEnabled(false); // Disable action bars
```

**Features:**
- Shows real-time combat countdown
- Asynchronous updates for performance
- Automatic cleanup when disabled

</details>

<details>
<summary><strong>📊 Bulk Operations</strong></summary>

#### `getAllPlayersInCombat()`
Get all players currently in combat.

```java
Set<Player> combatPlayers = api.getAllPlayersInCombat();
Bukkit.broadcastMessage(combatPlayers.size() + " players in combat!");
```

#### `getAllCombatPairs()`
Get all victim → attacker relationships.

```java
Map<Player, Player> pairs = api.getAllCombatPairs();
for (Map.Entry<Player, Player> entry : pairs.entrySet()) {
    Player victim = entry.getKey();
    Player attacker = entry.getValue();
    // Process combat pair
}
```

</details>

<details>
<summary><strong>🚪 Combat Logging</strong></summary>

#### `handleCombatLog(Player player)`
Handles player disconnection during combat.

```java
// Automatically called when player leaves during combat
api.handleCombatLog(player);
```

**Features:**
- Fires `PlayerKilledEvent` with combat log flag
- Removes player from combat
- Customizable punishment system

</details>

---

## 🎭 Events

### PlayerKilledEvent

Fired when a player is killed in combat or logs out during combat.

```java
@EventHandler
public void onPlayerKilled(PlayerKilledEvent event) {
    Player victim = event.getVictim();
    Player killer = event.getKiller();
    
    if (event.isCombatLog()) {
        // Handle combat logging
        Bukkit.broadcastMessage(victim.getName() + " combat logged!");
    } else if (killer != null) {
        // Handle normal PvP kill
        killer.sendMessage("You killed " + victim.getName() + "!");
    }
}
```

**Available Methods:**
- `getVictim()` - The killed player
- `getKiller()` / `getAttacker()` - The killer (may be null)
- `hasKiller()` / `hasAttacker()` - Check if killer exists
- `isCombatLog()` - Check if death was from combat logging
- `getKillReason()` - Get the reason for the kill

---

## ⚙️ Configuration

```yaml
# Combat duration in seconds
combat-duration: 10

# Action bar settings
action-bar:
  enabled: true
  update-frequency: 20  # Ticks (20 = 1 second)
  batch-size: 50       # Players processed per batch

# Combat messages
messages:
  enter-combat: "&cYou are now in combat!"
  leave-combat: "&aYou are no longer in combat."
  combat-log: "&c{victim} combat logged and was killed by {attacker}!"
  action-bar: "&cCombat: &f{time}s"
```

### Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `combat-duration` | `10` | How long combat lasts (seconds) |
| `action-bar.enabled` | `true` | Show combat timer in action bar |
| `action-bar.update-frequency` | `20` | Update interval in ticks |
| `action-bar.batch-size` | `50` | Players processed per update |

---

## 🔧 Commands & Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/combatapi reload` | `combatapi.reload` | Reload configuration |
| `/combatapi clear [player]` | `combatapi.clear` | Clear combat for player/all |
| `/combatapi status [player]` | `combatapi.status` | Check combat status |
---

## 🚀 Installation

### For Server Owners

1. **Download** the latest release from [Releases](../../releases)
2. **Place** the JAR file in your `plugins/` folder
3. **Restart** your server
4. **Configure** the plugin in `plugins/CombatAPI/config.yml`

### For Developers

#### Maven Dependency

```xml
<dependency>
    <groupId>me.hussainbeast</groupId>
    <artifactId>combatapi</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle Dependency

```gradle
dependencies {
    compileOnly 'me.hussainbeast:combatapi:1.0.0'
}
```

#### Shading (Optional)

To include CombatAPI directly in your plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>me.hussainbeast.combatapi</pattern>
                        <shadedPattern>your.package.combatapi</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 🔨 Building

```bash
# Clone the repository
git clone https://github.com/yourusername/CombatAPI.git
cd CombatAPI

# Build with Maven
mvn clean package

# The compiled JAR will be in target/
```

**Requirements:**
- Java 17+
- Maven 3.6+
- Spigot/Paper 1.8+

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. **Fork** the repository
2. **Create** your feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 💡 Support

- 📖 **Documentation**: Check this README and inline code comments
- 🐛 **Bug Reports**: [Open an issue](../../issues/new?template=bug_report.md)
- 💡 **Feature Requests**: [Open an issue](../../issues/new?template=feature_request.md)
- 💬 **Discord**: [Join our community](https://discord.gg/your-server)

---

<div align="center">

**Made with ❤️ for the Minecraft community**

⭐ **Star this repo if you found it helpful!** ⭐

</div>

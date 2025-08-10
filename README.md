# CombatAPI

A comprehensive Combat Log API for Minecraft plugins that provides easy access to combat information including attackers and victims.

## Features

- Track players in combat with their attackers
- Get combat time remaining for players
- Handle combat logging events
- Retrieve all players currently in combat
- Get combat pairs (victim -> attacker mapping)
- Configurable combat duration and action bar messages
- Easy to shade into other plugins

## API Usage

### Getting the API Instance

```java
import me.hussainbeast.combatapi.api.CombatAPI;
import me.hussainbeast.combatapi.api.CombatAPIProvider;

// Check if CombatAPI is available
if (CombatAPIProvider.isAvailable()) {
    CombatAPI api = CombatAPIProvider.getAPI();
}
```

### Main API Methods

```java
// Check if player is in combat
boolean inCombat = api.isInCombat(player);

// Get the attacker of a victim
Player attacker = api.getAttacker(victim);

// Get the victim of an attacker
Player victim = api.getVictim(attacker);

// Get all players currently in combat
Set<Player> playersInCombat = api.getAllPlayersInCombat();

// Get all combat pairs (victim -> attacker)
Map<Player, Player> combatPairs = api.getAllCombatPairs();

// Get remaining combat time
long timeRemaining = api.getCombatTimeRemaining(player);

// Manually enter/leave combat
api.enterCombat(victim, attacker);
api.leaveCombat(player);
```

### Listening to Combat Events

```java
import me.hussainbeast.combatapi.api.PlayerKilledEvent;

@EventHandler
public void onPlayerKilled(PlayerKilledEvent event) {
    Player victim = event.getVictim();
    Player attacker = event.getAttacker();
    
    if (event.isCombatLog()) {
        // Player was killed by combat logging
    } else {
        // Player was killed in normal PvP
    }
}
```

## Shading into Your Plugin

To use CombatAPI in your plugin, add this to your `pom.xml`:

### 1. Add Repository

```xml
<repositories>
    <repository>
        <id>local-repo</id>
        <url>file://${project.basedir}/libs</url>
    </repository>
</repositories>
```

### 2. Add Dependency

```xml
<dependencies>
    <dependency>
        <groupId>me.hussainbeast</groupId>
        <artifactId>combatapi</artifactId>
        <version>1.0.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

### 3. Configure Shade Plugin

```xml
<build>
    <plugins>
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
                        <createDependencyReducedPom>false</createDependencyReducedPom>
                        <artifactSet>
                            <includes>
                                <include>me.hussainbeast:combatapi</include>
                            </includes>
                        </artifactSet>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 4. Add Dependency in plugin.yml

```yaml
depend: [CombatAPI]
# or
soft-depend: [CombatAPI]
```

## Configuration

The plugin comes with a configurable `config.yml`:

```yaml
combat-duration: 10  # Combat duration in seconds

action-bar:
  enabled: true
  format: "&c&lCOMBAT &8» &f{time}s"
  update-frequency: 20  # Update frequency in ticks
  batch-size: 50  # Players processed per batch

messages:
  combat-log-killer: "&a{victim} &ccombat logged! You got the kill!"
  combat-log-broadcast: "&c{victim} &4combat logged and was killed!"
```

## Building

1. Clone the repository
2. Run `mvn clean package`
3. The compiled JAR will be in the `target/` directory

## Requirements

- Java 8+
- Spigot/Paper 1.8.8+
- Maven for building

## License

This project is open source and available under the MIT License.

# Heartbound Permissions Minecraft Plugin

Heartbound Permissions is a Minecraft plugin for Paper servers that adds permanent and timed extra hearts through operator-only commands, with support for day, second, and tick durations and persistent player heart data.

## Install

1. Use a Paper 1.21.11 server.
2. Install Java 21.
3. Build the project with Maven:
   ```
   mvn package
   ```
4. Copy the generated `target/plugin.jar` file into the server's `plugins` folder.
5. Start or restart the server.
6. The plugin creates its configuration data automatically.

## Use

Only players who are server operators can use the commands. Commands cannot be run from the console.

Add permanent hearts to yourself:

```
/perm heart <amount>
```

Example:

```
/perm heart 3
```

Add one timed heart:

```
/s <seconds> heart
/d <days> heart
/t <ticks> heart
```

Examples:

```
/s 60 heart
/d 2 heart
/t 1200 heart
```

Add a custom number of timed hearts:

```
/s <seconds> heart <amount>
/d <days> heart <amount>
/t <ticks> heart <amount>
```

Example:

```
/s 300 heart 2
```

Each extra heart adds two maximum health points. Permanent hearts remain after restarts. Timed hearts expire automatically, are checked once per second, and are stored so their expiration time persists across restarts. The plugin limits maximum health to 1024 health points.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/35c4c6be-964a-4a51-8050-78722f37ffbb?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=35c4c6be-964a-4a51-8050-78722f37ffbb

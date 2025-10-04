# Super Better Sleep InfinixMc - Forge Edition

A Minecraft Forge mod that improves sleep mechanics by allowing a configurable percentage of players to skip the night with customizable messages and weather control.

## Features

- **Configurable Sleep Threshold**: Set the percentage of players required to sleep (0.0 to 1.0)
- **Custom Messages**: Fully customizable sleeping and morning messages
- **Storm Control**: Option to clear storms when skipping night
- **TOML Configuration**: Easy-to-use configuration file
- **Multiplayer Friendly**: Designed for servers with multiple players
- **Consistent with Fabric Version**: Same functionality as the Fabric edition

## Installation

1. Make sure you have Minecraft Forge installed for version 1.21.1
2. Download the mod jar file
3. Place it in your `mods` folder
4. Start your server/client

## Configuration

The mod creates a configuration file at `config/sleepingmod-config.toml` with the following options:

```toml
# Name of the server
server_name = "Server"

# Color and format for the server name
server_name_color = "§6§l"
message_color = "§a"
format = "§o"

# Percentage of players required to sleep (0.5 = 50%)
sleep_threshold = 0.5

# Whether to clear storms when players sleep
clear_storms = true

# Message when storm persists
storm_persist_message = "It's still storming! Unable to skip the night."

# Custom messages for sleeping
sleeping_messages = [
  "Oh no! Everyone is entering the land of dreams... 😴",
  "Is this a pajama party? 🛌",
  "Watch out! Even the monsters are falling asleep. 💤"
]

# Custom messages for morning
morning_messages = [
  "Good morning, sleepyheads! The sun is here ☀️.",
  "Time to get up! Where's the coffee? ☕",
  "The dawn is here! Let's get to work 💪."
]
```

## Requirements

- Minecraft 1.21.1
- Minecraft Forge 52.0.17 or higher
- Java 17 or higher

## Building

To build the mod from source:

```bash
./gradlew build
```

The built jar will be in the `build/libs` directory.

## License

This project is licensed under the MIT License.

## Author

ReaperFxx

## Compatibility

This mod is server-side only and works on both dedicated servers and single-player worlds. It's fully compatible with the Fabric version and shares the same configuration format.# forge-infinixmc-sleepingmod

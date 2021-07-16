# WHIMC-Observations
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/whimc/Observation-Displayer?label=download&logo=github)](https://github.com/whimc/Observation-Displayer/releases/latest)

Create holographic observations in worlds

## Building
Compile a jar from the command line by doing a "Build" via Maven:
```
$ mvn clean package
```
It should show up in the target directory. Make sure to update your version number.

## Dependencies
* [HolographicDisplays](https://github.com/filoghost/HolographicDisplays)
* [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)

## Commands

### `/observations`
Manager the observations that have been created on the server.

| Command | Permission | Description |
|---|---|---|
|`/observations`|`n/a`|List all `/observations` subcommands|
|`/observations info`|`whimc-observations.observations.info`|Show information about an observation|
|`/observations list`|`whimc-observations.observations.list`|List all observations (or filer by player and/or world)|
|`/observations near`|`whimc-observations.observations.near`|List all observations in a radius|
|`/observations purge`|`whimc-observations.observations.purge`|Purge all temporary observations|
|`/observations reactivate`|`whimc-observations.observations.reactivate`|Reactivate observations within a given time frame|
|`/observations remove`|`whimc-observations.observations.remove`|Remove an observation|
|`/observations removeall`|`whimc-observations.observations.removeall`|Remove all observations from a certain player and/or in a certain world|
|`/observations setexpiration`|`whimc-observations.observations.setexpiration`|Change the expiration date of an observation|
|`/observations teleport`|`whimc-observations.observations.teleport`|Teleport to an observation|


### `/observe`
Root command for making observations.

| Command | Permission | Description |
|---|---|---|
|`/observe`|`whimc-observations.observe`|Open a GUI to form a templated observation|
|`/observe <observation>`|`whimc-observations.observe.freehand`|Write your own observation without a template|

If the player has the permission `whimc-observations.observe.customresponse`, they will be given the option to submit a custom response for their prompt fill-ins.

### Specific

## Config

### General
| Key | Type | Description |
|---|---|---|
|`debug`|`boolean`|Enable/disable debug messages (shows queries and other information in console)|
|`expiration-days`|`integer`|The amount of time (in days) it will take for an observation to expire|

#### Example
```yaml
debug: false
expiration-days: 7
```

### MySQL
| Key | Type | Description |
|---|---|---|
|`mysql.host`|`string`|The host of the database|
|`mysql.port`|`integer`|The port of the database|
|`mysql.database`|`string`|The name of the database to use|
|`mysql.username`|`string`|Username for credentials|
|`mysql.password`|`string`|Password for credentials|

#### Example
```yaml
mysql:
  host: localhost
  port: 3306
  database: minecraft
  username: user
  password: pass
```

### Template GUI
| Key | Type | Description |
|---|---|---|
|`template-gui.filler-item`|`Minecraft material`|The item to use for filler spaces in the GUI|
|`template-gui.inventory-name`|`string`|The name of the inventory used for the GUI|
|`template-gui.rows`|`integer`|The number of rows that will be in the GUI (Range [1-6])|
|`template-gui.cancel.item`|`Minecraft material`|The item to use for the cancel button|
|`template-gui.cancel.position`|`integer`|The position of the cancel button|
|`template-gui.cancel.name`|`string`|The text to display for the cancel button|
|`template-gui.cancel.lore`|`string list`|The lore of the cancel button|
|`template-gui.uncategorized.item`|`Minecraft material`|The item to use for the uncategorized observation button|
|`template-gui.uncategorized.position`|`integer`|The position of the uncategorized button|
|`template-gui.uncategorized.name`|`string`|The text to display for the uncategorized button|
|`template-gui.uncategorized.lore`|`string list`|The lore of the uncategorized button|

#### Example
```yaml
template-gui:
  filler-item: white_stained_glass_pane
  inventory-name: "&lChoose an observation type!"
  rows: 4
  cancel:
    item: barrier
    position: 27
    name: "&cCancel"
    lore:
      - "&7Cancel your observation"
  uncategorized:
    item: oak_sign
    position: 35
    name: '&7Uncategorized Observation'
    lore:
      - '&fMake a custom observation without a template.'
      - ''
      - '&8&oThis can also be done with &7&o/observe <observation>'
```

### Templates
There are 5 observation categories supported by the plugin: `ANALOGY`, `COMPARATIVE`, `DESCRIPTIVE`, `FACTUAL`, and `INFERENCE`

| Key | Type | Description |
|---|---|---|
|`templates.<category>.gui.item`|`Minecraft material`|The item to use for this category in the GUI|
|`templates.<category>.gui.position`|`integer`|The position of the category's item|
|`templates.<category>.gui.name`|`string`|The name of the category's item|
|`templates.<category>.gui.lore`|`string list`|The lore of the category's item|
|`templates.<category>.prompts`|`prompt list`|A list of prompts|

Each prompt has the prompt itself and a list of world-specific fill-ins for the prompt.
Within the `prompt`, a fill-in is notated via `{}`. A prompt can have as many fill-ins as you'd like.
You'll use the index of each `{}` to configure the fill-ins for that prompt in a given world.

#### Example prompt
```yaml
- prompt: The {} is {}.
  worlds:
    world1: # These options will only be shown for players in world1
      0: # These will be the fill-ins for the first {}
      - moon
      - water level
      1: # These will be the fill-ins for the second {}
      - large
      - missing
      - low
      - high
    world2: # These options will only be shown for players in world2
      # ...
```

#### Example template
```yaml
templates:
  DESCRIPTIVE:
    gui:
      item: orange_concrete
      position: 13
      name: '&6Descriptive Observation'
      lore:
        - '&fRelated to color, temperature, quantity, and other physical attributes.'
        - ''
        - '&7Example:'
        - '  &f&o There are lots of trees.'
    prompts:
      - prompt: There are lots of &7{}&r.
        worlds:
          world:
            0:
              - coral
              - trees
      - prompt: There are no &7{}&r.
        worlds:
          world:
            0:
              - animals
              - plants
      - prompt: It is really &7{}&r.
        worlds:
          world:
            0:
              - hot
              - cold
      - prompt: The &7{}&r is &7{}&r.
        worlds:
          world:
            0:
              - moon
              - water level
            1:
              - large
              - missing
              - low
              - high
```

# NameColor
[![Download](https://img.shields.io/github/downloads/blockparole/LeeesNC/latest/total.svg?label=download%20latest&style=popout-square)](https://github.com/blockparole/LeeesNC/releases/latest)
[![RepoSize](https://img.shields.io/github/languages/code-size/blockparole/LeeesNC.svg?label=repo%20size&style=popout-square)](https://github.com/blockparole/LeeesNC)
[![HitCount](http://hits.dwyl.com/blockparole/LeeesNC.svg)](https://github.com/blockparole/LeeesNC)

Provides a name color self service command.

![screenshot](screenshot.png)

Usage:
 - `/nc <modifiers>`
 - `/nc italic dark red STRIKETHROUGH magic underline BOLD`
 - `/nc reset`

|Colors         |Format          |
|---------------|----------------|
|`AQUA`         |`BOLD`          |
|`BLACK`        |`ITALIC`        |
|`BLUE`         |`MAGIC`         |
|`DARK_AQUA`    |`STRIKETHROUGH` |
|`DARK_BLUE`    |`UNDERLINE`     |
|`DARK_GRAY`    |                |
|`DARK_GREEN`   |                |
|`DARK_PURPLE`  |                |
|`DARK_RED`     |                |
|`GOLD`         |                |
|`GRAY`         |                |
|`GREEN`        |                |
|`LIGHT_PURPLE` |                |
|`RED`          |                |
|`WHITE`        |                |
|`YELLOW`       |                |

Player modifiers are referenced via UUID:
```
permission-required-command: false
permission-command: namecolor.command
save-modifiers: true
load-modifiers: true
modifier-bold-allow: true
modifier-italic-allow: true
modifier-magic-allow: false
modifier-strikethrough-allow: false
modifier-underline-allow: false
2a844c27-d1d2-32bd-88e9-c9fba95cf7a7: §m
```

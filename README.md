# Endless Fortune - Fabric Mod

A luck-based skill system mod for Minecraft 1.21.11 (Fabric).

## Features

### Skill System
Players receive a **random skill** on first join. Skills are divided into three categories:

#### Combat Skills
- **Berserker** - Channel rage for devastating damage
- **Guardian** - Defensive warrior who absorbs damage
- **Sharpshooter** - Master of ranged combat
- **Necromancer** - Command undead and drain life

#### Utility Skills
- **Alchemist** - Master of potions and brewing
- **Explorer** - Born to wander and discover
- **Enchanter** - Harness arcane enchantment power
- **Trickster** - Cunning rogue who bends the rules

#### Gathering Skills
- **Miner** - The earth yields its treasures
- **Farmer** - Nature blesses your harvests
- **Lumberjack** - Fell trees with efficiency
- **Fisherman** - The waters reward patience

### Luck System
Earn Luck by completing advancements:
- **Standard Advancements**: +0.5% Luck
- **Goal Advancements**: +2.0% Luck
- **Challenge Advancements**: +5.0% Luck

### Luck-Locked Abilities
Each skill has **5 abilities** that unlock at specific Luck thresholds (5%, 15%, 30%, 50%, 75%).

### Luck-Locked Loot Tables
When opening chests (Dungeons, Bastions, etc.):
- **Low Luck (< 10%)**: Normal loot
- **High Luck**: Items may be upgraded (Iron → Diamond, etc.)
- Plays **amethyst chime** sound when luck influences loot

### Potion of Regret
Reroll your skill! Crafted with Ghast Tears, Dragon's Breath, and Nether Stars.

### Skill Tomes
Craft category-specific skill tomes (Combat, Utility, Gathering) for very expensive recipes.
**Warning**: Using a Skill Tome **resets your Luck to 0%**!

## Commands
- `/luck` - View your current luck percentage
- `/luck info` - Detailed breakdown with abilities
- `/luck set <player> <amount>` - Set luck (OP only)
- `/luck add <player> <amount>` - Add/remove luck (OP only)
- `/skill` - View your current skill and abilities
- `/skill list` - View all available skills
- `/skill set <player> <skillId>` - Set skill (OP only)

## Crafting Recipes

### Luck Crystal
```
E A E
A D A
E A E
```
E = Emerald, A = Amethyst Shard, D = Diamond

### Potion of Regret
```
G D G
N B N
G D G
```
G = Ghast Tear, D = Dragon's Breath, N = Nether Star, B = Glass Bottle

### Combat Skill Tome
```
L N L
S B S
L N L
```
L = Luck Crystal, N = Netherite Ingot, S = Netherite Sword, B = Book

### Utility Skill Tome
```
L N L
E B E
L N L
```
L = Luck Crystal, N = Netherite Ingot, E = Eye of Ender, B = Book

### Gathering Skill Tome
```
L N L
P B P
L N L
```
L = Luck Crystal, N = Netherite Ingot, P = Netherite Pickaxe, B = Book

## Building
```bash
./gradlew build
```
The mod JAR will be in `build/libs/`.

## Requirements
- Minecraft 1.21.11
- Java 21
- Fabric Loader >= 0.18.0
- Fabric API

# Sable DynoCats

**Sable DynoCats** is a compatibility addon between **Sable Beyond** and **Copycats+** that adds realistic dynamic mass calculation for Copycat blocks.

Copycat blocks can imitate almost any material, but without proper integration they cannot provide correct physical properties to Sable. A wooden copycat and an iron copycat should not behave the same way.

This mod bridges that gap by calculating Copycat mass based on their actual copied materials.

---

## Features

### Copycats+ integration

- Support for Copycats+ blocks in Sable physics
- Automatic mass calculation based on copied materials
- Support for multi-state Copycats:
  - Copycat Byte
  - Copycat Board
  - Copycat Slab
  - Copycat Cogwheel
  - Copycat Half Layer
  - and more

### Accurate multi-material calculation

Multi-part Copycats are handled individually.

For example:

- A fully iron Copycat Byte is heavier than a partially filled one
- Empty sections contribute only the Copycat block mass
- Different materials inside one Copycat are calculated separately

This prevents situations where a single iron section makes an entire multi-part Copycat behave as if it was completely made of iron.

---

## Configuration

The mod provides configurable options:

| Option | Description |
| --- | --- |
| `enableMassMultipliers` | Enables custom mass multipliers from JSON definitions |
| `includeCopycatMass` | Adds the base mass of the Copycat block itself |

---

## Data-driven mass definitions

Mass properties are stored using JSON files.

Example:

```json
{
  "mass_multiplier": 0.5
}
```

# Downloads
## [Curseforge](https://www.curseforge.com/minecraft/mc-mods/sable-beyond-dynamic-copycats-addon)
## [Modrinth](https://modrinth.com/mod/sable-beyond-dynamic-copycats-addon)
## [Releases](https://github.com/kuki2008/Sable-Beyond-Dynamic-Copycat-Blocks-Mass/releases)

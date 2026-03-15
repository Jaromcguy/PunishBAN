# Punish

A lightweight, all-in-one punishment plugin for Paper 1.21.1+ servers. No database required — everything is stored in simple YAML files.

> 🌍 **Multi-language support built-in!** Switch between English and German at any time with `/punish lang en` or `/punish lang de` — no restart required.

---

## Features

🔨 **Ban System**
- `/ban <player> <preset> [duration] [reason]` — works online & offline
- Built-in presets: RA, HR, WR, SP, TX, hacking, illegal mods
- Custom durations (`1d`, `12h`, `30m`) or use preset defaults
- Permanent keywords: certain words in the reason force a permanent ban

🔇 **Mute System**
- `/mute <player> <preset> [duration] [reason]`
- Same preset system as bans

🛡️ **Smart Chat Filter**
- Detects filtered words with **leetspeak normalization** (e.g. `n1gg3r` → detected)
- Case insensitive
- Sends **clickable notifications** to staff with `[BAN]` and `[MUTE]` buttons
- Spam protection: cooldown prevents notification flooding

⚡ **Auto-Ban**
- 3 filter violations within 24 hours → automatic 24h ban
- Racism (RA) category → **instant permanent ban** on first offense

🔍 **Player Info & Logs**
- `/playerinfo <player>` — UUID, first/last login, ban/mute status, alt accounts (same IP)
- `/banlogs <player>` — full ban & mute history (last 10 entries)

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ban <player> <preset> [duration] [reason]` | Ban a player | `punish.ban` |
| `/unban <player>` | Unban a player | `punish.ban` |
| `/mute <player> <preset> [duration] [reason]` | Mute a player | `punish.mute` |
| `/unmute <player>` | Unmute a player | `punish.mute` |
| `/playerinfo <player>` | View player info | `punish.info` |
| `/banlogs <player>` | View punishment history | `punish.logs` |
| `/punish reload` | Reload config.yml | `punish.admin` |
| `/punish lang <de\|en>` | Switch plugin language | `punish.admin` |

---

## Permissions

| Permission | Description |
|-----------|-------------|
| `punish.ban` | Ban & unban players |
| `punish.mute` | Mute & unmute players |
| `punish.info` | View player info |
| `punish.logs` | View ban/mute logs |
| `punish.notify` | Receive chat filter alerts |
| `punish.admin` | Use /punish reload & /punish lang |

---

## Ban & Mute Presets

| Preset | Category | Default Duration |
|--------|----------|-----------------|
| `RA` | Racism | Permanent |
| `HR` | Hate Speech | 4 days |
| `WR` | Advertising | 1 day |
| `SP` | Spam | 1 day |
| `TX` | Toxic Behavior | 2 days |
| `schwerhacking` | Hacking (severe) | Permanent |
| `mittel_hacking` | Hacking (medium) | 7 days |
| `leicht_hacking` | Hacking (light) | 3 days |
| `illegal_leicht` | Illegal Mods (light) | 36 hours |
| `illegal_normal` | Illegal Mods (normal) | 7 days |

All presets, filter words, and durations are fully configurable in `config.yml`.

---

## Requirements

- Paper 1.21.1+
- Java 21+
- LuckPerms recommended for permission management

---

## Building

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/`.

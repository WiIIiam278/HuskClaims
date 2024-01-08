<!--suppress ALL -->
<p align="center">
    <img src="images/banner.png" alt="HuskClaims" />
    <a href="https://github.com/WiIIiam278/HuskClaims/actions/workflows/ci.yml">
        <img src="https://img.shields.io/github/actions/workflow/status/WiIIiam278/HuskClaims/ci.yml?branch=master&logo=github"/>
    </a> 
    <a href="https://repo.william278.net/#/releases/net/william278/huskclaims/">
        <img src="https://repo.william278.net/api/badge/latest/releases/net/william278/huskclaims/huskclaims-common?color=00fb9a&name=Maven&prefix=v" />
    </a> 
    <a href="https://discord.gg/tVYhJfyDWG">
        <img src="https://img.shields.io/discord/818135932103557162.svg?label=&logo=discord&logoColor=fff&color=7389D8&labelColor=6A7EC2" />
    </a> 
    <br/>
    <b>
        <a href="https://william278.net/docs/huskclaims/setup">Setup</a>
    </b> — 
    <b>
        <a href="https://william278.net/docs/huskclaims/">Docs</a>
    </b> — 
    <b>
        <a href="http://github.com/WiIIiam278/HuskClaims/issues">Issues</a>
    </b>
</p>
<br/>

**HuskClaims** is a clean, cross-server compatible grief prevention plugin that you already know how to use. HuskClaims will bring claiming on your server into the 2020s with intuitive chat menus, cross-server synchronisation support, modern display block visualisation, user group and LuckPerms role trust management, and much more &mdash; achieved through a modular, performant design.

## Features
**⭐ Works cross-server** &mdash; Works seamlessly cross-server. Manage/accrue claim blocks and list claims globally.

**⭐ Super intuitive** &mdash; Golden shovel claiming plugin, with a nice chat interface. Simple, and everyone knows how to use it!

**⭐ Modular & customizable** &mdash; Customise trust levels to suit your server needs. Display claims on BlueMap, Pl3xMap, and Dynmap.

**⭐ Great admin features** &mdash; Make admin claims and manage players. Trust LuckPerms permission groups for easy staff access management. 

**⭐ Modern conveniences** &mdash; Beautiful clickable menus and glowing display entity visualisation. Make groups to manage trust in bulk.

**⭐ Easy to import & configure** &mdash; Import existing player claims and profiles from GriefPrevention. Has a robust, [extensible API](https://william278.net/docs/huskclaims/api). 

**Ready?** [Let the claims begin!](https://william278.net/docs/huskclaims/setup)

## Setup
Requires Java 16+ and a Minecraft 1.16.5+ Spigot-based server. A MySQL database and (optionally) Redis are also needed if you wish to run the plugin across multiple servers on a proxy network.

1. Place the plugin jar file in the `/plugins/` directory of each Spigot server you want to install it on.
2. Start, then stop every server to let HuskClaims generate the config file.
3. Navigate to the HuskClaims config file on each server (`~/plugins/HuskClaims/config.yml`)
4. Configure the plugin to your liking. If you are running HuskClaims across multiple servers, set `cross_server.enabled` to `true` and fill in your MySQL credentials, remembering to change the database type to `MYSQL` as well.
5. Start every server again and begin claiming!

## Development
To build HuskClaims, simply run the following in the root of the repository:

```bash
./gradlew clean build
```

### License
HuskClaims is licensed under the Apache 2.0 license.

- [License](https://github.com/WiIIiam278/HuskClaims/blob/master/LICENSE)

Contributions to the project are welcome&mdash;feel free to open a pull request with new features, improvements and/or fixes!

### Support
_Coming soon!_

## Translations
Translations of the plugin locales are welcome to help make the plugin more accessible. Please submit a pull request with your translations as a `.yml` file. ([More info&hellip;](https://william278.net/docs/huskclaims/translations))

- [Locales Directory](https://github.com/WiIIiam278/HuskClaims/tree/master/common/src/main/resources/locales)
- [English Locales](https://github.com/WiIIiam278/HuskClaims/tree/master/common/src/main/resources/locales/en-gb.yml)

## Links
- [Docs](https://william278.net/docs/huskclaims) &mdash; Read the plugin documentation!
- [Issues](https://github.com/WiIIiam278/HuskClaims/issues) &mdash; File a bug report or feature request
- [Discord](https://discord.gg/tVYhJfyDWG) &mdash; Get help, ask questions (Proof of purchase required)

---
&copy; [William278](https://william278.net/), 2024. Licensed under the Apache-2.0 License.
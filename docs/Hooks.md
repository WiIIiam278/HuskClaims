HuskClaims offers several built-in hooks providing support for other plugins. These hooks can be enabled or disabled in the `hooks` section of the plugin [[config]].

| Name                               | Description                       | Link                                              |
|------------------------------------|-----------------------------------|---------------------------------------------------|
| [Vault](#vault)                    | Economy support for claim blocks  | https://www.spigotmc.org/resources/vault.34315/   |
| [LuckPerms](#luckperms)            | Trust tags for LuckPerms groups   | https://luckperms.net/                            |
| [HuskHomes](#huskhomes)            | Restricting homes to claims       | https://william278.net/project/huskhomes/         |
| [Plan](#plan)                      | Display claim analytics in Plan   | https://www.playeranalytics.net/                  |
| [PlaceholderAPI](#placeholderapi)  | Provides HuskClaims placeholders  | https://placeholderapi.com/                       |
| [Dynmap](#dynmap-pl3xmap-bluemap)  | Add claim markers to your Dynmap  | https://www.spigotmc.org/resources/dynmap.274/    |
| [Pl3xMap](#dynmap-pl3xmap-bluemap) | Add claim markers to your Pl3xMap | https://modrinth.com/plugin/pl3xmap/              |
| [BlueMap](#dynmap-pl3xmap-bluemap) | Add claim markers to your BlueMap | https://www.spigotmc.org/resources/bluemap.83557/ |

## Vault
If Vault (and a compatible economy plugin) is installed, the `/buyclaimblocks` command will be enabled allowing users to [purchase claim blocks for money](claim-blocks#buying-claim-blocks).

## LuckPerms
If LuckPerms is installed, HuskClaims will register [Trust Tags](trust#trust-tags) for every defined LuckPerms group in the format `#role/(group_name)` &mdash; e.g. `#role/admin`. This allows you to easily grant trust to all members of a LuckPerms group, particularly useful in admin claims.

## HuskHomes
If HuskHomes is installed, the `/huskclaims teleport` command will be enabled allowing admins to quickly teleport to claims from the [claim list](claims#listing-claims).

Additionally, HuskClaims will restrict creating or relocating homes to be within claims unless the user has a minimum [[trust]] level in the claim (default is Access Trust+).

## Plan
If Plan is installed, HuskClaims will display HuskClaims analytics (such as Claim Blocks and Claims created) in the Plan web interface on user and server pages.

## PlaceholderAPI
If PlaceholderAPI is installed, HuskClaims will register a PlaceholderAPI expansion allowing you to use HuskClaims placeholders in other plugins that support PlaceholderAPI. The following placeholders are available:

| Placeholder                              | Description                                                                | Example                       |
|------------------------------------------|----------------------------------------------------------------------------|-------------------------------|
| `%huskclaims_claim_blocks%`              | The number of [[claim blocks]] the player has                              | `100`                         |
| `%huskclaims_current_is_claimed%`        | Whether the player is currently standing in a claim                        | `true` or `false`             |
| `%huskclaims_current_claim_owner%`       | The owner of the claim the player is standing in                           | `Steve` or `an administrator` |
| `%huskclaims_current_claim_is_trusted%`  | Whether the player has a [[trust level]] in the claim they are standing in | `true` or `false`             |
| `%huskclaims_current_claim_trust_level%` | The trust level of the player in the claim they are standing in            | `Access` or `Container`       |
| `%huskclaims_can_build%`                 | Whether the player can build in the claim they are standing in             | `true` or `false`             |
| `%huskclaims_can_open_containers%`       | Whether the player can open containers in the claim they are standing in   | `true` or `false`             |
| `%huskclaims_can_interact%`              | Whether the player can interact in the claim they are standing in          | `true` or `false`             |

## Dynmap, Pl3xMap, BlueMap
If one of the supported mapping plugins is installed, HuskClaims will add region markers for claims on your server map:

* [Dynmap](https://www.spigotmc.org/resources/dynmap.274/) (v3.2+)
* [Pl3xMap](https://modrinth.com/plugin/pl3xmap/) (v1.20.4+)
* [BlueMap](https://www.spigotmc.org/resources/bluemap.83557/) (v3.20+)

You can configure the marker label name and which types of claims to show in the config file.
HuskClaims lets you give users, groups of users, and tags a "Trust Level" in [[Claims]]. Trust Levels grant users access to Operation Type flags and Privileges, which grant users respective claim access rights and management privileges. Trust levels are fully configurable and can be customized or changed entirely by editing the `trust_levels.yml` config file.

## Trust Levels
Trust Levels are defined in the `trust_levels.yml` config file and the default set of trust levels, based on the classic GriefPrevention plugin setup, are detailed below. Each trust level has an associated command (build trust, for instance). Users, groups, or tags can only be in one trust level at a time and trust levels are explicitly defined; they do not inherit granted operation types or privileges from lower levels.

<table>
    <thead>
        <tr>
            <th>Trust Level</th>
            <th>Command</th>
            <th>Description</th>
            <th>Allowed Operations</th>
            <th>Allowed Privileges</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Manage</td>
            <td><code>/permissiontrust</code></td>
            <td>Grants full claim management access, letting players trust/untrust other users, toggle operation groups, and create child claims (and all the below).</td>
            <td>
                <details>
                <summary>View list</summary>
                <ul>
                    <li>BLOCK_BREAK</li>
                    <li>BLOCK_PLACE</li>
                    <li>BLOCK_INTERACT</li>
                    <li>REDSTONE_INTERACT</li>
                    <li>ENTITY_INTERACT</li>
                    <li>CONTAINER_OPEN</li>
                    <li>FARM_BLOCK_BREAK</li>
                    <li>FARM_BLOCK_PLACE</li>
                    <li>FARM_BLOCK_INTERACT</li>
                    <li>PLACE_HANGING_ENTITY</li>
                    <li>BREAK_HANGING_ENTITY</li>
                    <li>PLAYER_DAMAGE_PLAYER</li>
                    <li>PLAYER_DAMAGE_PERSISTENT_ENTITY</li>
                    <li>PLAYER_DAMAGE_MONSTER</li>
                    <li>PLAYER_DAMAGE_ENTITY</li>
                    <li>FILL_BUCKET</li>
                    <li>EMPTY_BUCKET</li>
                    <li>USE_SPAWN_EGG</li>
                    <li>ENDER_PEARL_TELEPORT</li>
                </ul>
                </details>
            </td>
            <td>
                <details>
                <summary>View list</summary>
                <ul>
                    <li>MANAGE_TRUSTEES</li>
                    <li>MANAGE_CHILD_CLAIMS</li>
                    <li>MANAGE_OPERATION_GROUPS</li>
                    <li>MANAGE_BANS</li>
                    <li>MAKE_PRIVATE</li>
                </ul>
                </details>
            </td>
        </tr>
        <tr>
            <td>Build</td>
            <td><code>/trust</code></td>
            <td>Grants access to let users break and place all blocks within the claim (and all the below).</td>
            <td>
                <details>
                <summary>View list</summary>
                <ul>
                    <li>BLOCK_BREAK</li>
                    <li>BLOCK_PLACE</li>
                    <li>BLOCK_INTERACT</li>
                    <li>REDSTONE_INTERACT</li>
                    <li>ENTITY_INTERACT</li>
                    <li>CONTAINER_OPEN</li>
                    <li>FARM_BLOCK_BREAK</li>
                    <li>FARM_BLOCK_PLACE</li>
                    <li>FARM_BLOCK_INTERACT</li>
                    <li>PLACE_HANGING_ENTITY</li>
                    <li>BREAK_HANGING_ENTITY</li>
                    <li>PLAYER_DAMAGE_PLAYER</li>
                    <li>PLAYER_DAMAGE_PERSISTENT_ENTITY</li>
                    <li>PLAYER_DAMAGE_MONSTER</li>
                    <li>PLAYER_DAMAGE_ENTITY</li>
                    <li>FILL_BUCKET</li>
                    <li>EMPTY_BUCKET</li>
                    <li>USE_SPAWN_EGG</li>
                    <li>ENDER_PEARL_TELEPORT</li>
                </ul>
                </details>
            </td>
            <td style="text-align: center">
                (None)
            </td>
        </tr>
        <tr>
            <td>Container</td>
            <td><code>/containertrust</code></td>
            <td>Grants access to let users open containers&mdash;such as chests, hoppers, furnaces, etc&mdash;and edit the items in them (and all the below).</td>
            <td>
                <details>
                <summary>View list</summary>
                <ul>
                    <li>BLOCK_INTERACT</li>
                    <li>ENTITY_INTERACT</li>
                    <li>CONTAINER_OPEN</li>
                    <li>REDSTONE_INTERACT</li>
                    <li>ENDER_PEARL_TELEPORT</li>
                </ul>
                </details>
            </td>
            <td style="text-align: center">
                (None)
            </td>
        </tr>
        <tr>
            <td>Access</td>
            <td><code>/accesstrust</code></td>
            <td>Grants access to let users press buttons, levers, and pressure plates; and open doors, trapdoors, and fence gates.</td>
            <td>
                <details>
                <summary>View list</summary>
                <ul>
                    <li>BLOCK_INTERACT</li>
                    <li>ENTITY_INTERACT</li>
                    <li>REDSTONE_INTERACT</li>
                    <li>ENDER_PEARL_TELEPORT</li>
                </ul>
                </details>
            </td>
            <td style="text-align: center">
                (None)
            </td>
        </tr>
    </tbody>
</table>

The owner of a claim, or other users who are trusted at a level with the `MANAGE_TRUSTEES` privilege can manage the trust of users with a lower trust levels than them through these command. To trust a user, stand in the claim and use the relevant trust command followed by the `user`/`@group`/`#tag`; to revoke trust, do the same but with `/untrust`. You can trust/untrust multiple parties at once by simply adding more `users`/`@groups`/`#tags` to the end of the command. Finally, to view a list of trusted users, use `/trustlist`.

![Trusting users, groups and tags in a claim](https://raw.githubusercontent.com/WiIIiam278/HuskClaims/master/images/trusting.gif)

### Configuring Trust Levels
> **Warning:** Removing or changing the IDs of existing trust levels in your config file is a destructive action! Players will lose their trust levels and will need to be re-trusted by claim owners.

Trust levels may be configured in the `trust_levels.yml` config file. Trust levels are ordered by their `weight` value; users with access to the `MANAGE_TRUSTEES` privilege cannot change the trust level of a user/group/tag if their rank is or would be a higher weight than their current trust level.

The default trust levels are defined below:

<details>
<summary>Trust levels config (trust_levels.yml)</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃   HuskClaims - Trust Levels  ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ List of trust levels users & groups can be assigned to in claims
# ┣╸ Config Help: https://william278.net/docs/huskclaims/trust-levels/
# ┗╸ Documentation: https://william278.net/docs/huskclaims/

trust_levels:
- id: manage
  display_name: Manage
  description: Allows users to manage trustees & make child claims
  color: '#fc4e03'
  command_aliases:
  - managetrust
  - permissiontrust
  flags:
  - BLOCK_BREAK
  - BLOCK_PLACE
  - BLOCK_INTERACT
  - REDSTONE_INTERACT
  - ENTITY_INTERACT
  - CONTAINER_OPEN
  - FARM_BLOCK_BREAK
  - FARM_BLOCK_PLACE
  - FARM_BLOCK_INTERACT
  - PLACE_HANGING_ENTITY
  - BREAK_HANGING_ENTITY
  - PLAYER_DAMAGE_PLAYER
  - PLAYER_DAMAGE_PERSISTENT_ENTITY
  - PLAYER_DAMAGE_MONSTER
  - PLAYER_DAMAGE_ENTITY
  - FILL_BUCKET
  - EMPTY_BUCKET
  - USE_SPAWN_EGG
  - ENDER_PEARL_TELEPORT
  privileges:
  - MANAGE_TRUSTEES
  - MANAGE_CHILD_CLAIMS
  - MANAGE_OPERATION_GROUPS
  - MANAGE_BANS
  - MAKE_PRIVATE
  weight: 400
- id: build
  display_name: Build
  description: Allows users to build in the claim
  color: '#fcd303'
  command_aliases:
  - trust
  - buildtrust
  flags:
  - BLOCK_BREAK
  - BLOCK_PLACE
  - BLOCK_INTERACT
  - REDSTONE_INTERACT
  - ENTITY_INTERACT
  - CONTAINER_OPEN
  - FARM_BLOCK_BREAK
  - FARM_BLOCK_PLACE
  - FARM_BLOCK_INTERACT
  - PLACE_HANGING_ENTITY
  - BREAK_HANGING_ENTITY
  - PLAYER_DAMAGE_PLAYER
  - PLAYER_DAMAGE_PERSISTENT_ENTITY
  - PLAYER_DAMAGE_MONSTER
  - PLAYER_DAMAGE_ENTITY
  - FILL_BUCKET
  - EMPTY_BUCKET
  - USE_SPAWN_EGG
  - ENDER_PEARL_TELEPORT
  privileges: []
  weight: 300
- id: container
  display_name: Container
  description: Allows users to open chests & other containers
  color: '#5efc03'
  command_aliases:
  - containertrust
  flags:
  - BLOCK_INTERACT
  - ENTITY_INTERACT
  - CONTAINER_OPEN
  - REDSTONE_INTERACT
  - ENDER_PEARL_TELEPORT
  privileges: []
  weight: 200
- id: access
  display_name: Access
  description: Allows users to use doors, buttons, levers, etc.
  color: '#36e4ff'
  command_aliases:
  - accesstrust
  flags:
  - BLOCK_INTERACT
  - ENTITY_INTERACT
  - REDSTONE_INTERACT
  - ENDER_PEARL_TELEPORT
  privileges: []
  weight: 100
```
</details>

## Users, Groups & Tags
Users, user groups, and tags can be trusted at a trust level.

### Users
To trust a user in a claim, stand in the claim and type the trust level command followed by their username.

### User Groups
User groups are a way of managing the trust of multiple groups of users at once, handy for large projects. User groups belong to the claim owner; note you cannot trust groups in admin claims for this reason. Below is an example of a claim owner managing a user group on their claim:

| Command                                    | Result                                                                                                   |
|--------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `/usergroup awesome_people add Steve Alex` | Creates a user group named `awesome_people` and adds the players `Steve` and `Alex`.                     |
| `/trust @awesome_people`                   | Trusts the group `awesome_people` in the claim you are in (effectively giving `Steve` and `Alex` access) |
| `/usergroup awesome_people add William278` | Adds `William278` to the group `awesome_people`, giving `William278` access to the claim too.            |
| `/usergroup awesome_people add Steve`      | Removes `Steve` from the group `awesome_people`, effectively revoking their access to the claim.         |
| `/usergroup awesome_people delete`         | Deletes the group `awesome_people`. `Alex` and `William278` will lose access to the claim.               |

Note that deleting a user group won't remove it from the claim; it will remain trusted but greyed out. If the claim owner re-creates the group with new members, the group will be re-enabled.

### Trust Tags
Trust tags are an abstract way of representing a set of users, and can be provided by other plugins through the [[API]] or through special built-in hooks. Below are the list of built-in tags.

| Tag            | Description                                                                                                                           |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `#public`      | Grant public access to a claim.                                                                                                       |
| `#role/(name)` | Grant access to a [LuckPerms](https://luckperms.net/) permission group in a claim. Requires LuckPerms installed and the hook enabled. |

Please note that the use of trust tags can be restricted by [[Permissions]]; by default, users cannot trust LuckPerms roles.

### Trustable calculation order
The effective trust level of a user is calculated in the order described below if a user belongs to multiple trustable parties, they will receive the most explicit trust level defined for them:

1. User &mdash; If the user has an explicit permission in a claim, they are trusted at that level.
2. User Group &mdash; If the user is the member of one of the claim owner's groups, they are trusted at that level.
3. Trust Tag &mdash; If the user meets the criteria of a trust tag, they are trusted at that level.

**Example:** The owner of a claim has trusted the tag `#public` at the "Manage" trust level, granting everyone management access. The owner has also explicitly trusted the user `Steve` at the "Build" trust level. Since "Steve" is trusted as a User (a higher priority trustable type than a Trust Tag), their effective trust level is "Build Trust," not "Manage Trust."

### Owner allowed permissions
The owner has access to all permissions in their claim, regardless of their trust level.
You can remove certain permissions from the owner by editing the `allowed_owner_operations` section in the `trust_levels.yml` file.
This could be useful for removing the ability for the owner to pvp in their claim, for example.
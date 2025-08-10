<table align="right">
    <thead>
        <tr><th colspan="2">Key</th></tr>
    </thead>
    <tbody>
        <tr><td>✅</td><td>Accessible by all players by default</td></tr>
        <tr><td>❌</td><td>Accessible only by server operators by default</td></tr>
    </tbody>
</table>

HuskClaims provides permissions for restricting access to commands and features. 

These permissions are detailed below.

## Commands
Please see the [[Commands]] page reference for a full list of commands and their permissions.

## Inspection
These permissions restrict the ability to inspect claims.

| Permission                          | Description                                                                                   | Default |
|-------------------------------------|-----------------------------------------------------------------------------------------------|---------|
| `huskclaims.inspect`                | Inspect claims with the inspection tool.                                                      | ✅       |
| `huskclaims.inspect.nearby`         | Inspect all nearby claims by holding SNEAK and using the inspection tool.                     | ✅       |
| `huskclaims.inspect.view_last_seen` | When inspecting, whether the user can see how many days since the claim owner last logged on. | ❌       |

## Claims
These permissions restrict the ability to create certain types of claims. See [[Claims]] for more details on claiming land. It is also possible to disable creating admin/child claims in the plugin config.

| Permission               | Description                                                      | Default |
|--------------------------|------------------------------------------------------------------|:-------:|
| `huskclaims.claim`       | Create regular user-owned claims.                                |    ✅    |
| `huskclaims.admin_claim` | Create admin claims, **and manage all other admin claims.**      |    ❌    |
| `huskclaims.child_claim` | Create child claims; sub-divisions of land within parent claims. |    ✅    |

## Claim Blocks
These numerical permissions let you control the number of [[claim blocks]] to award players each hour, and specify the upper limit. By default, these permission nodes are not granted and the [[config]] file defaults are used for all players.

| Permission                             | Description                                                                            |
|----------------------------------------|----------------------------------------------------------------------------------------|
| `huskclaims.hourly_blocks.(amount)`    | Specify the integer `(amount)` of claim blocks this player should receive hourly.      |
| `huskclaims.max_claim_blocks.(amount)` | Specify the maximum integer `(amount)` of claim blocks this player can have and spend. |


## Trust Tags
These permissions restrict being able to use certain trust tags when granting trust to other players. See [[Trust]] for more details on trust tags.

| Permission                   | Description                                                                                                    | Default |
|------------------------------|----------------------------------------------------------------------------------------------------------------|:-------:|
| `huskclaims.trust.public`    | Use the `#public` trust tag in claims to grant public access.                                                  |    ✅    |
| `huskclaims.trust.luckperms` | Use the `#role/(name)` trust tags in claims to grant LuckPerms role-based access. Requires the LuckPerms hook. |    ❌    |

You can turn off the permission requirement for using LuckPerms groups in claims in the [[config]] hook settings. 

## Bypass Permissions
These permissions allow users to bypass certain claim restrictions.

| Permission                    | Description                                              | Default |
|-------------------------------|----------------------------------------------------------|:-------:|
| `huskclaims.bypass.ban`       | Bypass claim bans and enter claims the user is banned from. |    ❌    |
| `huskclaims.bypass.private`   | Bypass private claim restrictions and enter private claims without trust. |    ❌    |

## Flags
These permissions restrict the use of flags.

| Permission                       | Description        | Default |
|----------------------------------|--------------------|:-------:|
| `huskclaims.flag.<name of flag>` | Access to the flag |    ✅    |
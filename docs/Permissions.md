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

| Permission               | Description                                                     | Default |
|--------------------------|-----------------------------------------------------------------|:-------:|
| `huskclaims.claim`       | Create regular user-owned claims.                               |    ✅    |
| `huskclaims.admin_claim` | Create admin claims, **and manage all other admin claims.**     |    ❌    |
| `huskclaims.child_claim` | Create child claims; sub-divisons of land within parent claims. |    ✅    |

## Trust Tags
These permissions restrict being able to use certain trust tags when granting trust to other players. See [[Trust]] for more details on trust tags.

| Permission                   | Description                                                                                                    | Default |
|------------------------------|----------------------------------------------------------------------------------------------------------------|:-------:|
| `huskclaims.trust.public`    | Use the `#public` trust tag in claims to grant public access.                                                  |    ✅    |
| `huskclaims.trust.luckperms` | Use the `#role/(name)` trust tags in claims to grant LuckPerms role-based access. Requires the LuckPerms hook. |    ❌    |

You can turn off the permission requirement for using LuckPerms groups in claims in the [[config]] hook settings. 

## Flags
These permissions restrict the use of flags.

| Permission                       | Description        | Default |
|----------------------------------|--------------------|:-------:|
| `huskclaims.flag.<name of flag>` | Access to the flag |    ✅    |
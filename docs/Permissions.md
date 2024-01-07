HuskClaims provides permissions for restricting access to commands and features. The permissions are listed below.

<table align="right">
    <thead>
        <tr><th colspan="2">Key</th></tr>
    </thead>
    <tbody>
        <tr><td>✅</td><td>Accessible by all players by default</td></tr>
        <tr><td>❌</td><td>Accessible only by server operators by default</td></tr>
    </tbody>
</table>

## Commands
Please see the [[Commands]] page reference for a full list of commands and their permissions.

## Claims
These permissions restrict the ability to create certain types of claims. See [[Claims]] for more details on claiming land. It is also possible to disable creating admin/child claims in the plugin config.

| Permission               | Description                                                     | Default |
|--------------------------|-----------------------------------------------------------------|---------|
| `huskclaims.claim`       | Create regular user-owned claims.                               | ✅       |
| `huskclaims.admin_claim` | Create admin claims.                                            | ❌       |
| `huskclaims.child_claim` | Create child claims; sub-divisons of land within parent claims. | ✅       |

## Trust Tags
These permissions restrict being able to use certain trust tags when granting trust to other players. See [[Trust]] for more details on trust tags.

| Permission                   | Description                                                                                                     | Default |
|------------------------------|-----------------------------------------------------------------------------------------------------------------|---------|
| `huskclaims.trust.public`    | Use the `#public` trust tag in claims to grant public access. This permission check is disabled by default.     | ✅       |
| `huskclaims.trust.luckperms` | Use the `#role/(name)` trust tags in claims to grant LuckPerms role-based access.\nRequires the LuckPerms hook. | ❌       |

Note you can turn on/off restricting these trust tags behind permissions.
HuskClaims supports toggling Operation Groups within a claim to determine whether the plugin should prevent certain operations from occurring in a claim.

## Toggling Operation Groups
Operation Groups can be configured in the plugin [[config]] and managed by any user who has the `MANAGE_OPERATION_GROUPS` [[trust]] privilege in a claim. Effectively, Operation Groups provide a way of letting players fine tune the flag settings of your claim. 

By default, HuskClaims provides the `Claim Explosions` operation group to let you toggle whether explosion damage should be allowed in a claim.

| Operation Group    | Toggle Command     | Description                                                                                            | Default |
|--------------------|--------------------|--------------------------------------------------------------------------------------------------------|:-------:|
| `Claim Explosions` | `/claimexplosions` | Toggle whether explosion block damage should be allowed in a claim. Includes block and mob explosions. |    ❌    |

## Customizing Operation Groups
Operation groups can be customised in the plugin config as follows:

<details>
<summary>Operation Groups (config.yml)</summary>

```yaml
# Groups of operations that can be toggled on/off in claims
operation_groups:
- name: Claim Explosions
  description: Toggle whether explosions can damage terrain in claims
  toggle_command_aliases:
  - claimexplosions
  allowed_operations:
  - EXPLOSION_DAMAGE_TERRAIN
  - MONSTER_DAMAGE_TERRAIN
```
</details>

Whether an Operation Group is the default in a claim depends on whether the `allowed_operations` of the group are also present in the `default_flags` list in the config.
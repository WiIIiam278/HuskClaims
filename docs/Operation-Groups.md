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

## Fine-Grained Flag Management
> **Note:** This is a powerful command, only accessible to operators by default. Be careful granting player access to this command!

The `/claimflags` command allows you to fine-tune the allowed operation group settings within a claim. This is a powerful command, and can cause users confusion if they accidentally mess with the wrong flags. We therefore recommend creating operation group commands for end-user needs, and restricting this command for moderator actions.

Use `/claimflags list` to bring up a list of enabled operation types in the claim you are standing in. This command requires the user to have the `MANAGE_OPERATION_GROUPS` privilege in the claim they are stood in, or the `huskclaims.command.claimflags.other` bypass permission.

Click on the listed flags to toggle their allow-state on or off, or use `/claimflags set [operation_type] <true/false>`.

### Adjusting the flags outside of claims
You can also can adjust the value of flags outside of claims (the "Wilderness") by using `/claimflags` while standing outside a claim. This requires the `huskclaims.command.claimflags.world` permission.
HuskClaims offers a moderation feature for protecting player death drops from being picked up by other users. Users can use `/unlockdrops` to unlock their drops (to allow a friend to pick them up, for instance). This system can be configured under the `moderation.drops` section of `config.yml`:

<details>
<summary>Drops Moderation &mdash; config.yml</summary>

```yaml
  drops:
    # Whether to lock ground items dropped by players when they die from being picked up by others
    lock_items: true
    # Whether to also prevent death drops from being destroyed by lava, fire, cacti, etc.
    prevent_destruction: false
```
</details>

## Locking drops
When users die, items will be tagged and marked as "locked" &mdash; only the player who dropped the items will be able to pick them up. Note though that they still may be removed by damage sources (lava, fire, cacti, etc.) &mdash; unless destruction preventing is enabled &mdash; and can still despawn.

### Unlocking drops
Users can use `/unlockdrops` to unlock drops. Staff with the `huskclaims.command.unlockdrops.other` permission may unlock the drops of another user with `/unlockdrops [username]`

## Preventing destruction
You can prevent death drops from being destroyed by damage sources (such as fire, lava, etc.) by enabling `prevent_destruction`. This is disabled by default as it goes somewhat against the vanilla game. Items will still be removed if they fall into the void or by another plugin.
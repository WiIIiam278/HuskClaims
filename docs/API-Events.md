HuskClaims provides several API events your plugin can listen to when players do certain town-related things. These events deal in HuskClaims class types, so you may want to familiarize yourself with the [API basics](API) first. Note that on cross-server setups, events only fire on the *server the event occurred on* and will not fire as a result of API calls/updates.

## Bukkit Platform Events
> **Tip:** Remember to register your listener when listening for these event calls.

| Bukkit Event class             | Since | Cancellable | Description                                                          |
|--------------------------------|:-----:|:-----------:|----------------------------------------------------------------------|
| `BukkitCreateClaimEvent`       |  1.0  |      ✅      | When a player creates a claim or admin claim                         |
| `BukkitCreateChildClaimEvent`  |  1.0  |      ✅      | When a player creates a child claim                                  |
| `BukkitDeleteClaimEvent`       |  1.0  |      ✅      | When a player deletes a claim or admin claim                         |
| `BukkitDeleteChildClaimEvent`  |  1.0  |      ✅      | When a player deletes a child claim                                  |
| `BukkitDeleteAllClaimsEvent`   |  1.0  |      ✅      | When a player deletes all their claims or all admin claims           |
| `BukkitResizeClaimEvent`       |  1.0  |      ✅      | When a player resizes a claim or admin claim                         |
| `BukkitResizeChildClaimEvent`  |  1.0  |      ✅      | When a player resizes a child claim                                  |
| `BukkitTrustEvent`             |  1.0  |      ✅      | When a player trusts a user, group or trust tag in any kind of claim |
| `BukkitUnTrustEvent`           |  1.0  |      ✅      | When a player removes trust from a user, group, or trust tag         |
| `BukkitTransferClaimEvent`     |  1.0  |      ✅      | When a player changes who owns a claim or admin claim                |
| `BukkitEnterClaimEvent`        |  1.0  |      ✅      | When a player walks into a (child/admin/regular) claim               |
| `BukkitExitClaimEvent`         | 1.1.2 |      ✅      | When a player walks out of a (child/admin/regular) claim             |
| `BukkitClaimBlocksChangeEvent` |  1.0  |      ✅      | When a user has their claim block balance changed                    |
| `BukkitClaimWorldPruneEvent`   |  1.3  |      ✅      | When a claim world is pruned of its claims (on startup)              |
| `BukkitClaimBanEvent`          |  1.3  |      ✅      | When a player bans someone from a claim                              |
| `BukkitClaimUnBanEvent`        |  1.3  |      ✅      | When a player unbans someone from a claim                            |
| `BukkitClaimMakePrivateEvent`  | 1.4.1 |      ✅      | When a player makes a claim private (cant be entered without trust)  |
| `BukkitClaimMakePublicEvent`   | 1.4.1 |      ✅      | When a player makes a claim public                                   |
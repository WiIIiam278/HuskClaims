Claim Blocks (■) are a special currency that HuskClaims provides that limit how much land players can claim. Players accrue claim blocks for every hour they play on the server, and can additionally be bought (if the Vault hook is enabled, and you have an economy plugin installed) with the `/buyclaimblocks` command. Players can check their claim block balance using the `/claimblocks` command.

When a user creates a claim, a number of claim blocks equal to the lateral rectangular surface area of the claim will be deducted from the player's balance. Removing a claim will refund the player the claim blocks that were used to create it; resizing a claim will either refund or charge the user additional claim blocks based on the claim's change in surface area after the resize. Users will not be permitted to create or resize a claim if they lack the sufficient number of claim blocks.

Neither child claims nor admin claims cost claim blocks to create.

## Claim Block Accrual
By default, players accrue 100 claim blocks for every hour they play on the server. This can be changed in the config file:

<details>
<summary>Hourly Claim Blocks (config.yml)</summary>

```yaml
# The number of claim blocks a user gets hourly.
# Override with the "huskclaims.hourly_blocks.(amount)" permission
hourly_claim_blocks: 100
```
</details>

This value can additionally be overridden by granting the `huskclaims.hourly_blocks.(amount)` permission node to a user/group, where `(amount)` is the number of claim blocks to grant that user hourly. Note this permission node does not stack; the value of the highest effective permission will be taken. On servers running HuskClaims using cross-server mode, claim blocks will be globally synchronised.

## Buying Claim Blocks
If the Vault ("economy") hook is enabled, and you have an economy plugin installed, players can buy claim blocks with the `/buyclaimblocks (amount)` command. The cost of buying claim blocks can be configured in the config file, and is `1.0` unit of currency by default:

<details>
<summary>Economy Hook (config.yml)</summary>

```yaml
economy:
  # Whether to hook into an economy plugin to allow buying claim blocks
  enabled: true
  # The cost of buying 1 claim block
  cost_per_block: 1.0
```
</details>

## Adjusting Claim Blocks
Admins can adjust a player's claim block balance with the `/claimblocks (player) add|remove|set (amount)` command. This command can additionally be executed through the server automation to facilitate automation.

Furthermore, admins can view a transaction log of a user's claim block balance with the `/huskclaims logs (player)` command. See the [[Commands]] reference for details on necessary permissions.

## Gifting Claim Blocks
Players can gift others claim blocks from their balances using `/claimblocks (player) gift (amount)`. This command is only accessible to operators by default, and can be allowed for users with the `huskclaims.command.claimblocks.gift` permission.
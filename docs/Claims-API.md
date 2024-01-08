HuskClaims provides API for getting, creating resizing, & deleting [[claims]], child claims, and admin claims, and managing [[claim blocks]].

This page assumes you have read the general [[API]] introduction and that you have both imported HuskClaims into your project and added it as a dependency.

## Table of Contents
* [1. Getting if a location is claimed](#1-getting-if-a-location-is-claimed)
  * [1.1 Getting the ClaimWorld for a World](#11-getting-the-claimworld-for-a-world)
* [2. Checking what a user can do at a location](#2-checking-what-a-user-can-do-at-a-location)
* [3. Editing claims](#3-editing-claims)
  * [3.1 Resizing & deleting claims](#31-resizing--deleting-claims)
* [4. Checking & updating a user's claim blocks](#4-checking--updating-a-users-claim-blocks)

## 1. Getting if a location is claimed
* On the Bukkit platform, get a `Position` object using `#getPosition(org.bukkit.Location location)`
* Use `#isClaimAt(Position position)` to check if the location has been claimed
* Or, use `#getClaimAt(Position position)` to get the `Optional<Claim>` at the location
  * With an `Optional<Claim>`, you can use `Optional#isPresent()` to check if a claim exists at the location
  * With a `Claim` object, you can also check if it's an admin claim (`#isAdminClaim()`) or a child claim (`#isChildClaim(ClaimWorld)`) &mdash; more on claim worlds in a bit!
* You can get the displayed name of who owns the claim using `#getClaimOwnerNameAt(Position position)`, returning an `Optional<String>`
  * If the claim is an admin claim, the owner name will be `an administrator` (or whatever you have set in the locales)
* Check the [[Trust API]] for trusting users in a claim.

<details>
<summary>Example &mdash; Getting if a location is claimed</summary>

```java
void showClaimerNameAt(org.bukkit.Location location) {
    Position position = huskClaims.getPosition(location);
    Optional<Claim> claim = huskClaims.getClaimAt(position);
    if (claim.isPresent()) {
        System.out.println("This location is claimed by " + huskClaims.getClaimOwnerNameAt(position).get());
    }
}
```
</details>

### 1.1 Getting the ClaimWorld for a World
* Claims exist within a `ClaimWorld` in HuskClaims. `World`s without `ClaimWorld`s are not protected by HuskClaims.
* On the Bukkit platform, get a `World` object from a Bukkit World using `#getWorld(org.bukkit.World)` (or call `#getWorld()` on a `Position` object)
* You can then get the `ClaimWorld` for a world using `#getClaimWorld(World world)` which will return an `Optional<ClaimWorld>`

<details>
<summary>Example &mdash; Getting the claim world for a world</summary>

```java
void showClaimWorld(org.bukkit.World world) {
    Optional<ClaimWorld> claimWorld = huskClaims.getClaimWorld(world);
    if (claimWorld.isPresent()) {
        System.out.println("This world is protected by HuskClaims, and contains " + claimWorld().getClaimCount() + " claims!");
    }
}
```
</details>

## 2. Checking what a user can do at a location
* On the Bukkit platform, get an `OnlineUser` object using `#getOnlineUser(@NotNull org.bukkit.Player player)`
  * Use `#getPosition()` to get the `Position` of an `OnlineUser` to check if there's a claim where they stand (see #1)
* Check if a user can perform `OperationTypes` using `isOperationAllowed(user, type, position)` 
  * Use the `#isOperationAllowed` method that accepts and build an `Operation` via `Operation.builder()` for more complex operation checks!
* Additionally, we can check if a user has a `TrustLevel.Privileges` at a location using `#isPrivilegeAllowed(TrustLevel.Privilege privilege, User user, Position position)`
* Finally, we can also look up a user''s [[trust]] level in a claim using `#getTrustLevelAt(Position position, Trustable user)` (`User` and `OnlineUser` implement `Trustable`!)
  * This will return an `Optional<TrustLevel>`, which you can check if it is present using `Optional#isPresent()`, or through the `#ifPresent((trustLevel) -> {})` lambda syntax.

<details>
<summary>Example &mdash; Checking what a user can do at a location</summary>

```java
void checkUserAccessAt(org.bukkit.Player player, org.bukkit.Location location) {
    OnlineUser user = huskClaims.getOnlineUser(player);
    Position position = huskClaims.getPosition(location);
    if (huskClaims.isOperationAllowed(user, OperationType.BREAK_BLOCKS, position)) {
        System.out.println("User can build here!");
    }
    if (huskClaims.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, user, position)) {
        System.out.println("User can manage child claims here!");
    }
    huskClaims.getTrustLevelAt(position, user).ifPresent((level) -> {
        System.out.println("User has " + level.name() + " trust here!"); // "User has Build Trust here!"
    });
}
```
</details>

## 3. Editing claims
* Create a region using `Region.from(BlockPosition pos1, BlockPosition pos2)`
  * `Position` extends `BlockPosition`, so you can use `Position` objects for this
  * Or, get a `BlockPosition` using `#getBlockPosition(x, z)`
  * You can also create a region from a square radial selection using `Region.from(BlockPosition center, int radius)`
* Check if a region contains claims using `#isRegionClaimed(World world, Region region)` or `#isRegionClaimed(ClaimWorld world, Region region)`
* If not, create an admin claim using `#createAdminClaim(ClaimWorld world, Region region)`
  * This method returns a `CompletableFuture<Claim>`, which you can use to get the created claim when it has been created asynchronously
* You can also create regular claims through the API using `#createClaim(ClaimWorld world, Region region, User user)` &mdash; note that the user needs claim blocks for this (more on how to check/update this below)

<details>
<summary>Example &mdash; Creating an admin claim</summary>

```java
void createAdminClaimAround(org.bukkit.Player player, org.bukkit.Location location) {
    OnlineUser user = huskClaims.getOnlineUser(player);
    Position position = huskClaims.getPosition(location);
    Region region = Region.from(position, 100); // Create a 100-block radius region around the player
    if (!huskClaims.isRegionClaimed(position.getWorld(), region)) {
        huskClaims.createAdminClaim(position.getWorld(), region).thenAccept((claim) -> {
            // This future will complete when the claim has been created
            System.out.println("Created admin claim at " + claim.getCenter().toString());
        }).exceptionally((e) -> { 
            // This future can also complete exceptionally if the claim could not be created
            System.out.println("Failed to create admin claim: " + e.getMessage());
            return null;
        }); 
    } else {
        System.out.println("This region is already claimed!");
    }
}
```
</details>

### 3.1 Resizing & deleting claims
* You can resize claims using `#resizeClaim(Claim claim, Region region)`, or `#resizeChildClaim(Claim claim, Region region)` for child claims
  * These methods return a `CompletableFuture<Claim>`, which you can use to get the resized claim when it has been resized asynchronously
* You can delete claims using `#deleteClaim(Claim claim)`, or `#deleteChildClaim(Claim claim)` for child claims

<details>
<summary>Example &mdash; Resizing a claim</summary>

```java
void resizeClaim(org.bukkit.Player player, org.bukkit.Location location) {
    OnlineUser user = huskClaims.getOnlineUser(player);
    Position position = huskClaims.getPosition(location);
    Optional<Claim> claim = huskClaims.getClaimAt(position);
    if (claim.isPresent()) {
        Region region = Region.from(position, 100); // Create a 100-block radius region around the player
        if (huskClaims.isRegionClaimed(position.getWorld(), region)) {
            System.out.println("This region is already claimed!");
            return;
        }
        huskClaims.resizeClaim(claim.get(), region).thenAccept((resizedClaim) -> {
            // This future will complete when the claim has been resized
            System.out.println("Resized claim at " + resizedClaim.getCenter().toString());
        }).exceptionally((e) -> { 
            // This future can also complete exceptionally if the claim could not be resized
            System.out.println("Failed to resize claim: " + e.getMessage());
            return null;
        }); 
    } else {
        System.out.println("This region is not claimed!");
    }
}
```
</details>

### 4. Checking & updating a user's claim blocks
* You can get an `OnlineUser`'s claim block balance using `#getClaimBlocks(OnlineUser user)`
  * For offline users, use the `#getClaimBlocks(User user)` or `#getClaimBlocks(UUID uuid)` methods which will return a `CompletableFuture<Long>` with their claim block count.
  * You can also quickly check if a user has enough claim blocks using `#hasClaimBlocks(OnlineUser user, long amount)`
* You can update a user's claim block balance using the `#giveClaimBlocks`, `#takeClaimBlocks` and `#setClaimBlocks` methods which accept a `User` and a `long` amount
* Note that these methods will fire the platform `ClaimBlocksChangeEvent`, which you can listen to for when a user's claim block balance changes
* Also note a player cannot have a negative claim block balance and `#takeClaimBlocks` will throw an `IllegalArgumentException` if you attempt to take more claim blocks than the user has

<details>
<summary>Example &mdash; Checking & updating a user's claim blocks</summary>

```java
void giveClaimBlocks(org.bukkit.Player player, long amount) {
    OnlineUser user = huskClaims.getOnlineUser(player);
    if (huskClaims.hasClaimBlocks(user, amount)) {
        huskClaims.takeClaimBlocks(user, amount); // This will empty the user's claim block balance
        System.out.println("Took " + amount + " claim blocks from " + player.getName());
    } else {
        System.out.println(player.getName() + " does not have enough claim blocks!");
    }
}
```
</details>
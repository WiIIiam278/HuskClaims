HuskClaims provides API for trusting users in claims and registering trust tags.

## 1. Getting the list of trust levels
* You can get a list of registered trust levels using `HuskClaimsAPI#getTrustLevels()`.
* Within the `TrustLevel` object, you can get the various privileges and their associated `TrustLevel.Privilege`s using `#getPrivileges()`, and allowed `OperationType`s using `#getFlags()`.

<details>
<summary>Example &mdash; Getting the list of TrustLevels</summary>

```java
void showTrustLevels(org.bukkit.command.CommandSender sender) {
    sender.sendMessage("Registered trust levels: " + huskClaims.getTrustLevels().stream()
            .map(TrustLevel::getName).collect(Collectors.joining(", ")));
}
```
</details>

## 2. Getting UserGroups
* You can get a list of a user's created `UserGroup`s using `HuskClaimsAPI#getUserGroups(User user)`.
* You can get a `UserGroup` by name using `HuskClaimsAPI#getUserGroupByName(User owner, String name)`.


<details>
<summary>Example &mdash; Getting a user's created User Groups</summary>

```java
void showUserGroups(org.bukkit.Player player) {
  final OnlineUser user = huskClaims.getOnlineUser(player);
  player.sendMessage("Registered user groups for " + user.getName() + ": " + huskClaims.getUserGroups(user).stream()
            .map(UserGroup::getName).collect(Collectors.joining(", ")));
}
```
</details>

## 3. Trusting Trustables in a claim
* You can use the `HuskClaimsAPI#setTrustLevel(Claim claim, ClaimWorld claimWorld, Trustable trustable, TrustLevel trustLevel)` method to set a user's trust level in a claim.
  * Check the [[Claims API]] documentation for _getting_ a `Claim` and `ClaimWorld` from a `Position`
  * Also check the Claims API page for getting a user's trust level in any given claim.
* Valid `Trustables` include a `User` (or extending classes, such as an `OnlineUser`), a `TrustTag`, or a `UserGroup`.

<details>
<summary>Example &mdash; Trusting a user in a claim</summary>

```java
void trustUserInClaim(Claim claim, ClaimWorld claimWorld, User user) {
    huskClaims.setTrustLevel(claim, claimWorld, user, TrustLevel.BUILD);
}
```
</details>

## 4. TrustTags
* A `TrustTag` is a tag that can be trusted on claims that represents a set of `Users`.
* HuskClaims provides a `TrustTag` abstract class which you can extend to create your own trust tag, which you can then provide via the `HuskClaimsAPI#registerTrustTag(TrustTag)` method.
* Users can then trust users in your tag on claims using the relevant trust command, followed by your `#(tag)`.
* You can check which trust tags are registered in-game in the `/huskclaims status` menu.

## 4.1 Getting TrustTags
* You can get a list of registered trust tags using `HuskClaimsAPI#getTrustTags()`.
* You can get a `TrustTag` by name using `HuskClaimsAPI#getTrustTagByName(String name)`.

<details>
<summary>Example &mdash; Getting the list of registered TrustTags</summary>

```java
void showTrustTags(org.bukkit.command.CommandSender sender) {
    sender.sendMessage("Registered trust tags: " + huskClaims.getTrustTags().stream()
            .map(TrustTag::getName).collect(Collectors.joining(", ")));
}
```
</details>

### 4.2 Extending the TrustTag class
> **Note:** Check the built-in `LuckPermsTrustTag` class for an example of a trust tag implementation.

* `TrustTag` is a simple abstract class. In the constructor, you must pass a tag `name` and `description`, and you must implement one method: `boolean includes(@NotNull User trustable);`
  * This method should return `true` if the supplied `User` is included in the tag, and `false` otherwise.
  * Tag names should not contain spaces or non-alphanumeric (a-z, A-Z, 0-9) characters!
* Additionally, you may override the `boolean canUse(@NotNull User user);` method to return `true` if the supplied `User` is allowed to trust others with the tag, or `false` otherwise (e.g. to restrict the tag behind a permission node).

<details>
<summary>Example Trust Tag</summary>

```java
public class MyTrustTag extends TrustTag {
    
    public TrustTag() {
        // Name / description
        super("mytag", "Grants access to upside-down people.");
    }
    
    @Override
    public boolean includes(@NotNull User trustable) {
        // Return true if the supplied user is included in the tag, false otherwise
        return user.getName().equals("Dinnerbone") || user.getName().equals("Grumm"); // Include Dinnerbone or Grumm!
    }

    @Override
    public boolean canUse(@NotNull User user) {
        // Return true if the supplied user is allowed to trust others with the tag, false otherwise
        return true; // Let everyone trust others with this tag in their claims
    }

}
```
</details>

### 4.3 Registering your TrustTag
You can register your trust tag by calling `HuskClaimsAPI#registerTrustTag(TrustTag)` with your created trust tag instance.

```java
void onEnable() {
    huskClaims.registerTrustTag(new MyTrustTag());
}
```

And users will be able to use `/(trust_level_command) #mytag` to trust the tag in their claims!
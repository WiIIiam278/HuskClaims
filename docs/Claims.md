Claims are rectangular regions owned by players that have been protected from grief by others. Claims cost [[Claim Blocks]] to create and extend the full height of the world.

## Creating Claims
Creating a claim requires the claim tool, which is a Golden Shovel by default. To create a claim, simply right-click a block to mark down a corner for a claim selection, then right-click a second block to mark down the opposite corner. A claim will be created over the rectangular region between the two corner points.

![Creating a claim by right-clicking two corner points](https://raw.githubusercontent.com/WiIIiam278/HuskClaims/master/images/claiming_land.gif)

A claim can alternatively be created by using the `/claim (radius)` command, which will create a claim with a radius of `(radius)` blocks around the player's current location. Claims have a minimum size and area, configurable in the plugin [[config]].

## Inspecting Claims
Inspecting claims is done with the inspection tool, which is a Stick by default. Simply right-click the stick at a block to inspect whether it has been claimed. Different types of claims will be highlighted with different glowing block displays (Paper 1.19.4+)/ghost blocks (Spigot 1.19.4 and below).

![Inspecting claims with the claim inspection tool](https://raw.githubusercontent.com/WiIIiam278/HuskClaims/master/images/inspecting_claims.gif)

Holding the Sneak key while right-clicking will inspect all nearby claims, highlighting all claims within a configurable radius of the block you right-clicked.

## Operations in Claims
Operations in claims are restricted to those with [[Trust]] in a claim (except admin claims&mdash;[see below](#admin-claims)), with different Trust levels allowing users to perform different operations.

### Natural Operations
Within claims, natural events&mdash;such as crop growth and mob spawning&mdash;may occur, depending on the default flags set by the server administrator (see the `default_flags`, `admin_flags`, and `wilderness_flags` sections in `config.yml`). However, you may use [[Operation Groups]] to; by default, the `/claimexplosions (on|off)` command allows claim managers and owners to toggle whether explosion damage should be allowed in a claim. 

### Ignoring Claims
Using the `/ignoreclaims (on|off)` command, it is possible for administrators to toggle whether they wish to ignore claim operation restrictions; while ignoring claims, administrators will be able to perform any operation in any claim, regardless of their trust level within it.

## Child Claims
Child claims (also known as "subdivisions") are essentially claims-within-a-claim. This is very useful for town projects, where you may want to limit which area of a claim a player can build in. Child claims are created by first using the `/childclaim` command to toggle child claim creation mode, then right-clicking two corner points within a parent claim to create the child claim. The `/childclaim (radius)` command also works provided you are standing within a parent claim and the radius is small enough to fit within it. Creating and managing child claims requires the `MANAGE_CHILD_CLAIMS` privilege in the parent claim.

By default, child claims inherit the parent claim's [[Trust]] access list, meaning users trusted in the parent claim will also have access to the child claim&mdash;but _not vice versa_. Access inheritance may be restricted by the owner with the `/restrictclaim (on|off)` toggle command.

## Admin Claims
Admin claims are special claims that are not owned by any player, and are not subject to the same restrictions as normal claims. Admin claims can be created by first using the `/adminclaim` command to toggle admin claim creation mode, then right-clicking two corner points to create the claim. The `/adminclaim (radius)` command also works if you prefer radial claim creation.

Unlike regular claims, all administrators with the `huskclaims.admin_claim` permission have full management privileges in every admin claim, allowing them to trust themselves and create child claims without being explicitly trusted.
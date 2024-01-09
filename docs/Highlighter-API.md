HuskClaims provides the `Highlighter` interface which you can implement to create your own highlighter for displaying claims to players, which you can then provide via the `HuskClaimsAPI#setHighlighter(Highlighter)` method.

## Table of contents
* [1. The Highlighter interface](#1-the-highlighter-interface)
* [2. Highlightable points](#2-highlightable-points)
* [3. Creating your own highlighter](#3-creating-your-own-highlighter)
* [4. Registering your highlighter](#4-registering-your-highlighter)

## 1. The Highlighter interface
> **Note:** Check the built-in abstract BlockHighlighter and extending BlockDisplayHighlighter and BlockUpdateHighlighter classes for an example of a highlighter implementation.

`Highlighter` is an interface for highlighting any number of `Highlightable`s for a supplied `OnlineUser` in a `World`. Highlighters must implement the following fairly self-explanatory methods:
* `#startHighlighting(OnlineUser user, World world, Collection<? extends Highlightable> toHighlight, boolean showOverlap)`
  * `user` &mdash; The `OnlineUser` to highlight for (on the Bukkit platform, cast this to a `BukkitUser` and use `getBukkitPlayer` to get the `org.bukkit.Player` object!)
  * `world` &mdash; The World to highlight in.
  * `toHighlight` &mdash; A collection of `Highlightable`s to highlight (see below)
  * `showOverlap` &mdash; A flag for whether this highlight is for showing to the user that their claim selection would overlap other claims
* `#stopHighlighting(OnlineUser user)`
  * `user` &mdash; The `OnlineUser` to stop highlighting for

## 2. Highlightable points
A `Highlightable` is an interface for any world object that can be highlighted; a `Claim` or a `ClaimSelection`. `Highlightable`s contain "points" that the highlighter may choose to get and highlight. Highlighters can choose to do whatever they want with these points, or not use this API at all; you might want to do an `instanceof` check to see if a `Highlightable` is in fact a `Region` and call `#getCorners()` for example.

Call `#getHighlightPoints(ClaimWorld world, boolean showOverlap, BlockPosition viewerPosition, long rangeFromViewerToReturnPointsFor)` to get a `Map<Region.Point, Type>`; a map of that type of point to a `Type` enum. The `Type` enum is used to determine the type of point, and can be one of: 
* `CORNER`,
* `EDGE`,
* `CHILD_CORNER`,
* `CHILD_EDGE`,
* `ADMIN_CORNER`,
* `ADMIN_EDGE`,
* `OVERLAP_CORNER`, used when highlighting overlapping claims (pass the `showOverlap` argument from your `startHighlighting` method implementation)
* `OVERLAP_EDGE`, ditto above
* or `SELECTION`

## 3. Creating your own highlighter
Create your own Highlighter class by implementing the `Highlighter` interface:

```java
public class MyHighlighter implements Highlighter {
    @Override
    public void startHighlighting(OnlineUser user, World world, Collection<? extends Highlightable> toHighlight, boolean showOverlap) {
        // Highlight the supplied Highlightables in the supplied world for the supplied user
    }

    @Override
    public void stopHighlighting(OnlineUser user) {
        // Stop highlighting for the supplied user
    }
}
```

## 4. Registering your highlighter
You can register your highlighter by calling `HuskClaimsAPI#setHighlighter(Highlighter)` with your created highlighter instance.

```java
void onEnable() {
    huskClaims.setHighlighter(new MyHighlighter());
}
```
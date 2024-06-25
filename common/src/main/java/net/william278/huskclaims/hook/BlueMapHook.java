/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.hook;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import net.kyori.adventure.text.format.TextColor;
import net.william278.desertwell.util.ThrowingConsumer;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@PluginHook(
        name = "BlueMap",
        register = PluginHook.Register.ON_ENABLE
)
public class BlueMapHook extends MapHook {

    private Map<String, MarkerSet> claimsMarkerSets;

    public BlueMapHook(@NotNull HuskClaims plugin) {
        super(plugin);
        BlueMapAPI.onEnable(api -> load());
        BlueMapAPI.onDisable(api -> unload());
    }

    @Override
    public void load() {
        this.claimsMarkerSets = new ConcurrentHashMap<>();

        for (Map.Entry<String, ClaimWorld> world : plugin.getClaimWorlds().entrySet()) {
            this.editMapWorld(world.getValue(), (mapWorld -> {
                final MarkerSet claimMarkers = MarkerSet.builder().label(getSettings().getMarkerSetName()).build();

                for (BlueMapMap map : mapWorld.getMaps()) {
                    map.getMarkerSets().put(getMarkerSetKey(), claimMarkers);
                }

                claimsMarkerSets.put(world.getKey(), claimMarkers);
            }));
        }

        this.markAllClaims();
    }

    private void editMapWorld(@NotNull ClaimWorld world, @NotNull ThrowingConsumer<BlueMapWorld> editor) {
        BlueMapAPI.getInstance().flatMap(api -> api.getWorld(world.getName(plugin))).ifPresent(editor);
    }

    @Override
    public void unload() {
        unMarkAllClaims();
    }

    private void editClaimsMarkerSet(@NotNull ClaimWorld world, @NotNull ThrowingConsumer<MarkerSet> editor) {
        editMapWorld(world, (mapWorld -> {
            if (claimsMarkerSets != null) {
                editor.accept(claimsMarkerSets.get(world.getName(plugin)));
            }
        }));
    }

    @Override
    public void markClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        final Optional<String> color = getClaimColor(claim).map(TextColor::asHexString);
        if (color.isEmpty()) {
            return;
        }
        this.editClaimsMarkerSet(claimWorld, (markerSet -> {
            final String markerId = getClaimMarkerKey(claim, claimWorld);
            markerSet.remove(markerId);
            markerSet.put(markerId, getClaimMarker(claim, claimWorld, color.get()));
        }));
    }

    @NotNull
    private ShapeMarker getClaimMarker(@NotNull Claim claim, @NotNull ClaimWorld claimWorld, @NotNull String hex) {
        final Color color = new Color(hex);
        final int x1 = claim.getRegion().getNearCorner().getBlockX() - 1;
        final int z1 = claim.getRegion().getNearCorner().getBlockZ() - 1;
        final int x2 = claim.getRegion().getFarCorner().getBlockX() + 1;
        final int z2 = claim.getRegion().getFarCorner().getBlockZ() + 1;
        return ShapeMarker.builder()
                .label(claim.getOwnerName(claimWorld, plugin))
                .fillColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.5f))
                .lineColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 1f))
                .shape(Shape.createRect(x1, z1, x2, z2), 64)
                .lineWidth(1)
                .depthTestEnabled(false)
                .build();
    }

    @Override
    public void markClaims(@NotNull Iterable<Claim> claims, @NotNull ClaimWorld claimWorld) {
        claims.forEach(claim -> {
            markClaim(claim, claimWorld);
            claim.getChildren().forEach(child -> markClaim(child, claimWorld));
        });
    }


    @Override
    public void unMarkClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        this.editClaimsMarkerSet(claimWorld, (markerSet -> markerSet.remove(getClaimMarkerKey(claim, claimWorld))));
        claim.getChildren().forEach(child -> this.editClaimsMarkerSet(claimWorld, (markerSet -> markerSet.remove(getClaimMarkerKey(child, claimWorld)))));
    }

    @Override
    public void unMarkClaimsBy(@Nullable UUID owner) {
        plugin.getWorlds().forEach(world -> plugin.getClaimWorld(world).ifPresent(
                claimWorld -> claimWorld.getClaimsByUser(owner).forEach(claim -> unMarkClaim(claim, claimWorld))
        ));
    }

    @Override
    public void unMarkAllClaims() {
        this.claimsMarkerSets.values().forEach(markerSet ->
                markerSet.getMarkers().keySet().forEach(markerSet::remove));
    }

    @Override
    public void markAllClaims() {
        plugin.getClaimWorlds().forEach((name, claimWorld) -> {
            final Collection<Claim> claims = claimWorld.getClaims();
            markClaims(claims, claimWorld);
            plugin.log(Level.INFO, "Populated web map with %s claims in %s".formatted(claims.size(), name));
        });
    }
}
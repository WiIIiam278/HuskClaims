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

import net.kyori.adventure.text.format.TextColor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import org.apache.commons.text.StringEscapeUtils;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class DynmapHook extends MapHook {
    @Nullable
    private DynmapCommonAPI dynmapApi;
    @Nullable
    private MarkerSet markerSet;

    public DynmapHook(@NotNull HuskClaims plugin) {
        super("Dynmap", plugin);
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(@NotNull DynmapCommonAPI dynmapCommonAPI) {
                dynmapApi = dynmapCommonAPI;
                load();
            }
        });
    }

    @Override
    public void load() {
        getDynmap().ifPresent(api -> {
            unMarkAllClaims();
            getMarkerSet();
            plugin.log(Level.INFO, "Enabled Dynmap markers hook. Populating web map with claims...");
            markAllClaims();
        });
    }

    @Override
    public void unload() {
        unMarkAllClaims();
    }

    @Override
    public void markClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        plugin.runSync(() -> getMarkerSet().ifPresent(markerSet -> {
            addMarker(claim, claimWorld, markerSet);
            claim.getChildren().forEach(child -> addMarker(child, claimWorld, markerSet));
        }));
    }

    @Override
    public void markClaims(@NotNull Iterable<Claim> claims, @NotNull ClaimWorld claimWorld) {
        plugin.runSync(() -> getMarkerSet().ifPresent(
                markerSet -> claims.forEach(claim -> {
                    addMarker(claim, claimWorld, markerSet);
                    claim.getChildren().forEach(child -> addMarker(child, claimWorld, markerSet));
                })
        ));
    }

    @Override
    public void unMarkClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        getMarkerSet().ifPresent(markerSet -> {
            markerSet.findAreaMarker(getClaimMarkerKey(claim, claimWorld)).deleteMarker();
            claim.getChildren().forEach(c -> markerSet.findAreaMarker(getClaimMarkerKey(c, claimWorld)).deleteMarker());
        });
    }

    @Override
    public void unMarkClaimsBy(@Nullable UUID owner) {
        plugin.getWorlds().forEach(world -> plugin.getClaimWorld(world).ifPresent(
                claimWorld -> claimWorld.getClaimsByUser(owner).forEach(claim -> unMarkClaim(claim, claimWorld))
        ));
    }

    @Override
    public void unMarkAllClaims() {
        plugin.runSync(() -> getMarkerSet().ifPresent(markerSet -> markerSet.getAreaMarkers()
                .forEach(AreaMarker::deleteMarker)));
    }

    @Override
    public void markAllClaims() {
        plugin.getClaimWorlds().forEach((name, claimWorld) -> {
            final Collection<Claim> claims = claimWorld.getClaims();
            markClaims(claims, claimWorld);
            plugin.log(Level.INFO, "Populated web map with %s claims in %s".formatted(claims.size(), name));
        });
    }

    private void addMarker(@NotNull Claim claim, @NotNull ClaimWorld claimWorld, @NotNull MarkerSet markerSet) {
        final Optional<Integer> color = getClaimColor(claim).map(TextColor::value);
        if (color.isEmpty()) {
            return;
        }
        final String id = getClaimMarkerKey(claim, claimWorld);
        final String label = String.format(getSettings().getLabelFormat(), claim.getOwnerName(claimWorld, plugin));

        // Find a marker to update or create a new one
        AreaMarker marker = markerSet.findAreaMarker(id);
        if (marker == null) {
            final Region.Point near = claim.getRegion().getNearCorner();
            final Region.Point far = claim.getRegion().getFarCorner();
            final String worldName = plugin.getClaimWorlds().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(claimWorld)).map(Map.Entry::getKey)
                    .findFirst().orElse(null);

            marker = markerSet.createAreaMarker(
                    id, StringEscapeUtils.escapeHtml4(label), false, worldName,
                    new double[]{
                            near.getBlockX(),
                            far.getBlockX()
                    },
                    new double[]{
                            near.getBlockZ(),
                            far.getBlockZ()
                    },
                    false
            );
        }

        // Set the fill and stroke colors
        marker.setRangeY(64, 64);
        marker.setFillStyle(0.5f, color.get());
        marker.setLineStyle(1, 1, color.get());
        marker.setLabel(label);
    }

    private Optional<DynmapCommonAPI> getDynmap() {
        return Optional.ofNullable(dynmapApi);
    }

    private Optional<MarkerSet> getMarkerSet() {
        return getDynmap().map(api -> {
            markerSet = api.getMarkerAPI().getMarkerSet(getMarkerSetKey());
            if (markerSet == null) {
                markerSet = api.getMarkerAPI().createMarkerSet(
                        getMarkerSetKey(),
                        getSettings().getMarkerSetName(),
                        api.getMarkerAPI().getMarkerIcons(),
                        false
                );
            } else {
                markerSet.setMarkerSetLabel(getSettings().getMarkerSetName());
            }
            return markerSet;
        });
    }
}

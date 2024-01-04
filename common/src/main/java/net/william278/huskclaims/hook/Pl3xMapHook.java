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
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.event.EventHandler;
import net.pl3x.map.core.event.EventListener;
import net.pl3x.map.core.event.server.Pl3xMapEnabledEvent;
import net.pl3x.map.core.event.world.WorldLoadedEvent;
import net.pl3x.map.core.event.world.WorldUnloadedEvent;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.markers.option.Tooltip;
import net.pl3x.map.core.util.Colors;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public class Pl3xMapHook extends MapHook {

    private static final String CLAIMS_LAYER = "claim_markers";
    private final ConcurrentHashMap<ClaimWorld, ConcurrentLinkedQueue<Claim>> claims = new ConcurrentHashMap<>();

    public Pl3xMapHook(@NotNull HuskClaims plugin) {
        super("Pl3xMap", plugin);
    }

    @NotNull
    private String getLayerName() {
        return getSettings().getMarkerSetName();
    }

    private void registerLayers(@NotNull net.pl3x.map.core.world.World mapWorld) {
        final ClaimsLayer layer = new ClaimsLayer(this, mapWorld);
        mapWorld.getLayerRegistry().register(layer);
    }

    @NotNull
    public Options getMarkerOptions(@NotNull Claim claim, @NotNull ClaimWorld claimWorld, @NotNull TextColor color, int weight) {
        return Options.builder()
                .strokeWeight(weight)
                .tooltip(new Tooltip(claim.getOwnerName(claimWorld, plugin)).setDirection(Tooltip.Direction.TOP))
                .fillColor(Colors.argb(255 / 2, color.red(), color.green(), color.blue()))
                .strokeColor(Colors.rgb((int) (color.red() * 0.7), (int) (color.green() * 0.7), (int) (color.blue() * 0.7)))
                .build();
    }

    @Override
    public void load() {
        Pl3xMap.api().getEventRegistry().register(new Pl3xEvents(this));

        if (Pl3xMap.api().isEnabled()) {
            Pl3xMap.api().getWorldRegistry().forEach(this::registerLayers);
        }

        plugin.log(Level.INFO, "Enabled Pl3xMap markers hook. Populating web map with claims...");
        markAllClaims();
    }

    @Override
    public void unload() {
        unMarkAllClaims();
    }

    @Override
    public void markClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        final ConcurrentLinkedQueue<Claim> claimQueue = claims.computeIfAbsent(claimWorld, k -> new ConcurrentLinkedQueue<>());
        claimQueue.add(claim);
        claimQueue.addAll(claim.getChildren());
    }

    @Override
    public void markClaims(@NotNull Iterable<Claim> claims, @NotNull ClaimWorld claimWorld) {
        final ConcurrentLinkedQueue<Claim> claimQueue = this.claims.computeIfAbsent(claimWorld, k -> new ConcurrentLinkedQueue<>());
        claims.forEach(c -> {
            claimQueue.add(c);
            claimQueue.addAll(c.getChildren());
        });
    }

    @Override
    public void unMarkClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        final ConcurrentLinkedQueue<Claim> claimQueue = claims.get(claimWorld);
        if (claimQueue != null) {
            claimQueue.remove(claim);
            claim.getChildren().forEach(claimQueue::remove);
        }
    }

    @Override
    public void unMarkClaimsBy(@Nullable UUID owner) {
        claims.forEach((claimWorld, claimQueue) -> claimQueue.stream()
                .filter(claim -> claim.getOwner().map(o -> Objects.equals(o, owner)).orElse(false))
                .forEach(claim -> unMarkClaim(claim, claimWorld)));
    }

    @Override
    public void unMarkAllClaims() {
        claims.clear();
    }

    @Override
    public void markAllClaims() {
        plugin.getClaimWorlds().forEach((name, claimWorld) -> {
            final Collection<Claim> claims = claimWorld.getClaims();
            markClaims(claims, claimWorld);
            plugin.log(Level.INFO, "Populated web map with %s claims in %s".formatted(claims.size(), name));
        });
    }

    public static class ClaimsLayer extends SimpleLayer {

        private final Pl3xMapHook hook;
        private final net.pl3x.map.core.world.World mapWorld;

        public ClaimsLayer(@NotNull Pl3xMapHook hook, @NotNull net.pl3x.map.core.world.World mapWorld) {
            super(CLAIMS_LAYER, hook::getLayerName);
            this.hook = hook;
            this.mapWorld = mapWorld;
        }

        @Override
        @NotNull
        public Collection<Marker<?>> getMarkers() {
            final Collection<Marker<?>> markers = new ArrayList<>();

            final Optional<ClaimWorld> world = Optional.ofNullable(hook.plugin.getClaimWorlds().get(mapWorld.getName()));

            world.ifPresent(claimWorld -> {
                ConcurrentLinkedQueue<Claim> claimQueue = hook.claims.get(claimWorld);

                if (claimQueue != null) {
                    claimQueue.forEach(claim -> {
                        final Optional<TextColor> color = hook.getClaimColor(claim, claimWorld);
                        if (color.isEmpty()) {
                            return;
                        }

                        //used to see child claims above parent claims
                        final int weight = !claim.isChildClaim(claimWorld) ? claim.getOwner().map(o -> 2).orElse(1) : 0;

                        markers.add(Marker.rectangle(
                                hook.getClaimMarkerKey(claim, world.get()),
                                Point.of(claim.getRegion().getNearCorner().getBlockX(), claim.getRegion().getNearCorner().getBlockZ()),
                                Point.of(claim.getRegion().getFarCorner().getBlockX(), (claim.getRegion().getFarCorner().getBlockZ()))
                        ).setOptions(hook.getMarkerOptions(claim, world.get(), color.get(), weight)));
                    });
                }
            });

            return markers;
        }
    }

    @SuppressWarnings("unused")
    public class Pl3xEvents implements EventListener {

        private final Pl3xMapHook hook;

        public Pl3xEvents(@NotNull Pl3xMapHook hook) {
            this.hook = hook;
        }

        @EventHandler
        public void onPl3xMapEnabled(@NotNull Pl3xMapEnabledEvent event) {
            // Register layers for each world
            Pl3xMapHook.this.load();
            Pl3xMap.api().getWorldRegistry().forEach(hook::registerLayers);
        }

        @EventHandler
        public void onWorldLoaded(@NotNull WorldLoadedEvent event) {
            hook.registerLayers(event.getWorld());
        }

        @EventHandler
        public void onWorldUnloaded(@NotNull WorldUnloadedEvent event) {
            event.getWorld().getLayerRegistry().unregister(CLAIMS_LAYER);
        }
    }
}

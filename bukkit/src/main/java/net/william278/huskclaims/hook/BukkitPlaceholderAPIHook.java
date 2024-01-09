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

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.user.BukkitUser;
import net.william278.huskclaims.user.OnlineUser;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

public class BukkitPlaceholderAPIHook extends Hook {

    private HuskClaimsExpansion expansion;

    protected BukkitPlaceholderAPIHook(@NotNull HuskClaims plugin) {
        super("PlaceholderAPI", plugin);
    }

    @Override
    public void load() {
        this.expansion = new HuskClaimsExpansion(
                plugin, plugin.getPluginVersion().toStringWithoutMetadata()
        );
        this.expansion.register();
    }

    @Override
    public void unload() {
        if (this.expansion != null) {
            this.expansion.unregister();
        }
    }

    @AllArgsConstructor
    public static class HuskClaimsExpansion extends PlaceholderExpansion {

        private final HuskClaims plugin;
        @Getter
        private final String version;
        @Getter
        private final String author = "William278";
        @Getter
        private final String name = "HuskClaims";
        @Getter
        private final String identifier = Placeholder.IDENTIFIER;
        @Getter
        private final List<String> placeholders = Placeholder.getFormattedList();

        @Override
        public String onRequest(@NotNull OfflinePlayer offlinePlayer, @NotNull String identifier) {
            final Player player = offlinePlayer.getPlayer();
            if (player == null) {
                return null;
            }
            return Placeholder.format(plugin, BukkitUser.adapt(player, plugin), identifier);
        }

        @AllArgsConstructor
        private enum Placeholder {
            CLAIM_BLOCKS((plugin, user) -> Long.toString(plugin.getClaimBlocks(user))),
            CURRENT_IS_CLAIMED((plugin, user) -> formatBoolean(plugin.getClaimAt(user.getPosition()).isPresent())),
            CURRENT_CLAIM_OWNER((plugin, user) -> plugin.getClaimWorld(user.getPosition().getWorld())
                    .flatMap(world -> world.getClaimAt(user.getPosition())
                            .map(claim -> claim.getOwnerName(world, plugin)))
                    .orElse(plugin.getLocales().getNotApplicable())),
            CURRENT_CLAIM_IS_TRUSTED((plugin, user) -> formatBoolean(plugin.getClaimWorld(user.getPosition().getWorld())
                    .map(world -> world.getClaimAt(user.getPosition())
                            .flatMap(claim -> claim.getTrustLevel(user, plugin)))
                    .isPresent())),
            CURRENT_CLAIM_TRUST_LEVEL((plugin, user) -> plugin.getClaimWorld(user.getPosition().getWorld())
                    .flatMap(world -> world.getClaimAt(user.getPosition())
                            .flatMap(claim -> claim.getTrustLevel(user, plugin)))
                    .map(TrustLevel::getDisplayName)
                    .orElse(plugin.getLocales().getNotApplicable())),
            CAN_BUILD((plugin, user) -> formatBoolean(plugin.cancelOperation(
                    Operation.of(user, OperationType.BLOCK_PLACE, user.getPosition())
            ))),
            CAN_OPEN_CONTAINERS((plugin, user) -> formatBoolean(plugin.cancelOperation(
                    Operation.of(user, OperationType.CONTAINER_OPEN, user.getPosition())
            ))),
            CAN_INTERACT((plugin, user) -> formatBoolean(plugin.cancelOperation(
                    Operation.of(user, OperationType.BLOCK_INTERACT, user.getPosition())
            )));

            private static final String IDENTIFIER = "huskclaims";

            private final BiFunction<HuskClaims, OnlineUser, String> resolver;

            @Nullable
            public static String format(@NotNull HuskClaims plugin, @NotNull OnlineUser user, @NotNull String identifier) {
                return Arrays.stream(values())
                        .filter(placeholder -> placeholder.name().toLowerCase(Locale.ENGLISH).equals(identifier))
                        .findFirst()
                        .map(placeholder -> placeholder.resolve(plugin, user))
                        .orElse(null);
            }

            @NotNull
            private static List<String> getFormattedList() {
                return Arrays.stream(values()).map(Placeholder::getFormattedName).toList();
            }

            @NotNull
            private static String formatBoolean(boolean bool) {
                return bool ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
            }

            @NotNull
            private String resolve(@NotNull HuskClaims plugin, @NotNull OnlineUser user) {
                return resolver.apply(plugin, user);
            }

            @NotNull
            private String getFormattedName() {
                return "%s_%s".formatted(
                        Placeholder.IDENTIFIER,
                        name().toLowerCase(Locale.ENGLISH).replaceAll("[^A-Za-z0-9]", "_")
                );
            }

        }

    }

}

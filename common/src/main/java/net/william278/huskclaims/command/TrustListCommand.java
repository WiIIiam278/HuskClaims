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

package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrustListCommand extends InClaimCommand {

    private static final int DESCRIPTION_WRAP = 40;

    protected TrustListCommand(@NotNull HuskClaims plugin) {
        super(List.of("trustlist"), TrustLevel.Privilege.MANAGE_TRUSTEES, plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        this.sendTrustListMenu(executor, claim, world);
    }

    private void sendTrustListMenu(@NotNull OnlineUser executor, @NotNull Claim claim, @NotNull ClaimWorld world) {
        plugin.getLocales().getLocale("trust_list_header",
                claim.getOwnerName(world, plugin)).ifPresent(executor::sendMessage);
        final List<TrustLevel> levels = plugin.getTrustLevels();
        levels.sort((o1, o2) -> Integer.compare(o2.getWeight(), o1.getWeight()));
        levels.forEach(level -> sendTrustListRow(executor, level, claim, world));
    }

    private void sendTrustListRow(@NotNull OnlineUser executor, @NotNull TrustLevel level,
                                  @NotNull Claim claim, @NotNull ClaimWorld world) {
        final StringJoiner joiner = new StringJoiner(plugin.getLocales().getListJoiner());
        plugin.getLocales().getRawLocale("none").ifPresent(joiner::setEmptyValue);

        // Add trusted users and groups
        claim.getTrustedUsers().entrySet().stream()
                .filter(e -> e.getValue().equals(level.getId()))
                .forEach(e -> joiner.add(getMemberEntry(e.getKey(), world)));
        claim.getOwner().ifPresent(owner -> claim.getTrustedGroups().entrySet().stream()
                .filter(e -> e.getValue().equals(level.getId()))
                .forEach(e -> joiner.add(getGroupEntry(e.getKey(), owner, plugin))));

        // Return the row
        plugin.getLocales().getRawLocale("trust_list_row",
                        Locales.escapeText(level.getDisplayName()),
                        plugin.getLocales().wrapText(Locales.escapeText(level.getDescription()), DESCRIPTION_WRAP),
                        joiner.toString()
                )
                .map(t -> plugin.getLocales().format(t))
                .ifPresent(executor::sendMessage);
    }

    @NotNull
    private String getMemberEntry(@NotNull UUID uuid, @NotNull ClaimWorld world) {
        return world.getUser(uuid)
                .flatMap(user -> plugin.getLocales().getRawLocale("trust_list_user",
                        Locales.escapeText(user.getName())))
                .orElse(uuid.toString());
    }

    @NotNull
    private String getGroupEntry(@NotNull String name, @Nullable UUID owner, @NotNull HuskClaims plugin) {
        if (owner == null) {
            return name;
        }
        return plugin.getUserGroup(owner, name)
                .flatMap(group -> plugin.getLocales().getRawLocale("trust_list_group",
                        Locales.escapeText(group.getTrustIdentifier(plugin)),
                        Integer.toString(group.members().size()),
                        group.members().stream()
                                .map(user -> Locales.escapeText(user.getName()))
                                .collect(Collectors.joining("\n"))
                ))
                .or(() -> plugin.getLocales().getRawLocale("trust_list_deleted_group",
                        String.format("%s%s", plugin.getSettings().getUserGroups().getGroupSpecifierPrefix(), name)
                ))
                .orElse(getPlugin().getLocales().getNotApplicable());
    }

}

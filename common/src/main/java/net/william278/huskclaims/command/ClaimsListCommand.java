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

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.hook.HuskHomesHook;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import net.william278.paginedown.ListOptions;
import net.william278.paginedown.PaginatedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class ClaimsListCommand extends Command implements GlobalClaimsProvider {
    private static final int CLAIMS_PER_PAGE = 8;
    private static final int TELEPORT_Y_LEVEL = 96;

    protected ClaimsListCommand(@NotNull List<String> aliases, @NotNull String usage, @NotNull HuskClaims plugin) {
        super(aliases, usage, plugin);
    }

    protected void showClaimList(@NotNull CommandUser executor, @Nullable User user,
                                 final List<ServerWorldClaim> claims,
                                 int page, @NotNull SortOption sort, boolean ascend) {
        claims.sort(ascend ? sort.getComparator() : sort.getComparator().reversed());
        executor.sendMessage(PaginatedList.of(
                claims.stream().map(c -> getClaimListRow(c, executor)).toList(),
                getListOptions(user, claims, sort, ascend)
        ).getNearestValidPage(page));
    }

    @NotNull
    private String getClaimListRow(@NotNull ServerWorldClaim claim, @NotNull CommandUser user) {
        return plugin.getLocales().getRawLocale("claim_list_item_separator")
                .map(separator -> String.join(separator, List.of(
                        getClaimPosition(claim.claim(), claim.serverWorld(), user),
                        getClaimSize(claim.claim()),
                        getClaimChildren(claim.claim()),
                        getClaimMembers(claim.claim())
                ))).orElse("");
    }

    @NotNull
    private String getClaimPosition(@NotNull Claim claim, @NotNull ServerWorld serverWorld, @NotNull CommandUser user) {
        final boolean crossServer = plugin.getSettings().getCrossServer().isEnabled();
        return plugin.getLocales().getRawLocale(
                switch (serverWorld.world().getEnvironment().toLowerCase(Locale.ENGLISH)) {
                    case "nether" -> "claim_list_position_nether";
                    case "the_end" -> "claim_list_position_end";
                    default -> "claim_list_position_overworld";
                },
                crossServer ? serverWorld.toString() : serverWorld.world().getName(),
                Integer.toString(claim.getRegion().getCenter().getBlockX()),
                Integer.toString(claim.getRegion().getCenter().getBlockZ()),
                plugin.getLocales().getRawLocale(
                        "claim_list_%sworld_tooltip".formatted(!crossServer ? "" : "server_")
                ).orElse(""),
                user instanceof OnlineUser online ? getTeleportOption(claim, serverWorld, online) : ""
        ).orElse("");
    }

    @NotNull
    private String getTeleportOption(@NotNull Claim claim, @NotNull ServerWorld serverWorld, @NotNull OnlineUser user) {
        final Optional<HuskHomesHook> homesHook = plugin.getHook(HuskHomesHook.class);
        if (plugin.canUseCommand(HuskClaimsCommand.class, user, "teleport") || homesHook.isEmpty()) {
            return "";
        }

        return plugin.getHook(HuskHomesHook.class).map(hook -> String.format(
                "%s run_command=/huskclaims teleport %s %s %s %s %s",
                getPlugin().getLocales().getRawLocale("claim_list_teleport_tooltip")
                        .orElse(""),
                serverWorld.server(),
                claim.getRegion().getCenter().getBlockX(),
                TELEPORT_Y_LEVEL,
                claim.getRegion().getCenter().getBlockZ(),
                serverWorld.world().getName()
        )).orElse("");
    }

    @NotNull
    private String getClaimSize(@NotNull Claim claim) {
        return plugin.getLocales().getRawLocale(
                "claim_list_blocks",
                Long.toString(claim.getRegion().getSurfaceArea()),
                Integer.toString(claim.getRegion().getLongestEdge()),
                Integer.toString(claim.getRegion().getShortestEdge())
        ).orElse("");
    }

    @NotNull
    private String getClaimChildren(@NotNull Claim claim) {
        return plugin.getLocales().getRawLocale(
                "claim_list_children",
                Integer.toString(claim.getChildren().size())
        ).orElse("");
    }

    @NotNull
    private String getClaimMembers(@NotNull Claim claim) {
        return plugin.getLocales().getRawLocale(
                "claim_list_trustees",
                Integer.toString(claim.getTrustedUsers().size() + claim.getTrustedGroups().size())
        ).orElse("");
    }

    @NotNull
    private ListOptions getListOptions(@Nullable User user, @NotNull List<ServerWorldClaim> claims,
                                       @NotNull SortOption sort, boolean ascend) {
        return plugin.getLocales().getBaseList(CLAIMS_PER_PAGE)
                .setHeaderFormat(getListTitle(plugin.getLocales(), user, claims.size(), sort, ascend))
                .setItemSeparator("\n")
                .setCommand(String.format(
                        "/%s%s %s %s",
                        getName(),
                        (user == null ? "" : " " + user.getName()),
                        sort.getId(),
                        (ascend ? "ascending" : "descending")
                ))
                .build();
    }

    protected Optional<SortOption> parseSortArg(@NotNull String[] args, int index) {
        return parseStringArg(args, index).flatMap(SortOption::matchSortOption);
    }

    protected Optional<Boolean> parseOrderArg(@NotNull String[] args, int index) {
        return parseStringArg(args, index).flatMap(s -> {
            if (s.equalsIgnoreCase("ascending")) {
                return Optional.of(true);
            } else if (s.equalsIgnoreCase("descending")) {
                return Optional.of(false);
            }
            return Optional.empty();
        });
    }

    @NotNull
    protected abstract String getListTitle(@NotNull Locales locales, @Nullable User user, int claimCount,
                                           @NotNull SortOption sort, boolean ascend);

    @NotNull
    protected String getSortButtons(@NotNull Locales locales, @NotNull SortOption sort, boolean ascend) {
        final StringJoiner options = new StringJoiner(
                locales.getRawLocale("claim_list_sort_option_separator").orElse("|")
        );

        for (SortOption option : SortOption.LISTED_OPTIONS) {
            boolean selected = option == sort;
            if (selected) {
                options.add(locales.getRawLocale("claim_list_sort_option_selected",
                                option.getDisplayName(plugin.getLocales()))
                        .orElse(option.getId()));
                continue;
            }
            options.add(locales.getRawLocale("claim_list_sort_option",
                            option.getDisplayName(plugin.getLocales()),
                            getName(),
                            option.getId(),
                            ascend ? "ascending" : "descending", "%current_page%")
                    .orElse(option.getId()));
        }
        return options.toString();
    }

    @Getter
    @AllArgsConstructor
    public enum SortOption {
        DIMENSION(Comparator.comparing(c -> c.serverWorld().world().getEnvironment().toLowerCase(Locale.ENGLISH))),
        WORLD(Comparator.comparing(c -> c.serverWorld().world().getName())),
        SERVER(Comparator.comparing(c -> c.serverWorld().server())),
        SIZE(Comparator.comparing(c -> c.claim().getRegion().getSurfaceArea())),
        MEMBERS(Comparator.comparingInt(c -> c.claim().getTrustedUsers().size())),
        CHILDREN(Comparator.comparingInt(c -> c.claim().getChildren().size()));

        public static final List<SortOption> LISTED_OPTIONS = List.of(SIZE, DIMENSION, CHILDREN);

        private final Comparator<ServerWorldClaim> comparator;

        @NotNull
        public String getDisplayName(@NotNull Locales locales) {
            return locales.getRawLocale(String.format("claim_list_sort_option_%s", getId())).orElse(getId());
        }

        @NotNull
        public String getId() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Optional<SortOption> matchSortOption(@NotNull String text) {
            return Arrays.stream(values()).filter(o -> o.getId().equals(text.toLowerCase(Locale.ENGLISH))).findFirst();
        }
    }

}

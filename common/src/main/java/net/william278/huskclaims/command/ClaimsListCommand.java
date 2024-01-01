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
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.User;
import net.william278.paginedown.PaginatedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class ClaimsListCommand extends Command {

    protected static final int LIST_CACHE_MINUTES = 5;
    private static final int CLAIMS_PER_PAGE = 8;

    protected ClaimsListCommand(@NotNull List<String> aliases, @NotNull String usage, @NotNull HuskClaims plugin) {
        super(aliases, usage, plugin);
    }

    protected void showClaimList(@NotNull CommandUser executor, @Nullable User user,
                                 final List<ServerWorldClaim> claims,
                                 int page, @NotNull SortOption sort, boolean ascend) {
        final Locales locales = plugin.getLocales();
        final boolean crossServer = plugin.getSettings().getCrossServer().isEnabled();
        claims.sort(ascend ? sort.getComparator().reversed() : sort.getComparator());
        executor.sendMessage(PaginatedList.of(
                claims.stream().map(claim -> locales.getRawLocale(
                        "claim_list_item",
                        locales.getRawLocale(
                                switch (claim.serverWorld().world().getEnvironment().toLowerCase(Locale.ENGLISH)) {
                                    case "nether" -> "claim_list_dimension_nether";
                                    case "the_end" -> "claim_list_dimension_end";
                                    default -> "claim_list_dimension_overworld";
                                },
                                crossServer ? claim.serverWorld().toString() : claim.serverWorld.world().getName(),
                                locales.getRawLocale(
                                        "claim_list_%sworld_tooltip".formatted(!crossServer ? "" : "server_")
                                ).orElse("")
                        ).orElse(""),
                        locales.getRawLocale(
                                "claim_list_coordinates",
                                Integer.toString(claim.claim().getRegion().getNearCorner().getBlockX()),
                                Integer.toString(claim.claim().getRegion().getNearCorner().getBlockZ())
                        ).orElse(""),
                        locales.getRawLocale(
                                "claim_list_blocks",
                                Long.toString(claim.claim().getRegion().getSurfaceArea()),
                                Integer.toString(claim.claim().getRegion().getLongestEdge()),
                                Integer.toString(claim.claim().getRegion().getShortestEdge())
                        ).orElse(""),
                        locales.getRawLocale(
                                "claim_list_children",
                                Integer.toString(claim.claim().getChildren().size())
                        ).orElse(""),
                        locales.getRawLocale(
                                "claim_list_trustees",
                                Integer.toString(claim.claim().getTrustedUsers().size()
                                        + claim.claim().getTrustedGroups().size())
                        ).orElse("")
                ).orElse("")).toList(),
                locales.getBaseList(CLAIMS_PER_PAGE)
                        .setHeaderFormat(getListTitle(locales, user, claims.size(), sort, ascend))
                        .setItemSeparator("\n")
                        .setCommand(String.format(
                                "/%s%s %s %s",
                                getName(),
                                (user == null ? "" : " " + user.getName()),
                                sort.getId(),
                                (ascend ? "ascending" : "descending")
                        )).build()
        ).getNearestValidPage(page));
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

    protected record ServerWorldClaim(@NotNull ServerWorld serverWorld, @NotNull Claim claim) {
    }

}

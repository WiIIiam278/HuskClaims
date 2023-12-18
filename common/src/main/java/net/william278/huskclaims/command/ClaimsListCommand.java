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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

import java.util.*;

public class ClaimsListCommand extends Command implements UserListTabCompletable {

    private static final int CLAIMS_PER_PAGE = 10;
    private final Map<UUID, List<ServerWorldClaim>> claimLists = Maps.newHashMap();

    protected ClaimsListCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("claimslist", "claims"),
                "[player] [sort_by] [ascending|descending] [page]",
                plugin
        );
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<User> optionalUser = resolveUser(executor, args);
        final SortOption sort = parseSortArg(args, 0).or(() -> parseSortArg(args, 1)).orElse(SortOption.SIZE);
        final boolean ascend = parseOrderArg(args, 1).or(() -> parseOrderArg(args, 2)).orElse(true);
        final int page = Math.min(1, parseIntArg(args, 2).or(() -> parseIntArg(args, 3)).orElse(1));
        if (optionalUser.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }
        showClaimList(executor, optionalUser.get(), page, sort, ascend);
    }

    private void showClaimList(@NotNull CommandUser executor, @NotNull User user,
                               int page, @NotNull SortOption sort, boolean ascend) {
        if (claimLists.containsKey(user.getUuid())) {
            showClaimList(executor, user, claimLists.get(user.getUuid()), page, sort, ascend);
            return;
        }


        final List<ServerWorldClaim> claims = Lists.newArrayList(getUserClaims(user));
        if (claims.isEmpty()) {
            plugin.getLocales().getLocale("error_no_claims_made", user.getName())
                    .ifPresent(executor::sendMessage);
            return;
        }
        claimLists.put(user.getUuid(), claims);
        showClaimList(executor, user, claims, page, sort, ascend);
    }

    private void showClaimList(@NotNull CommandUser executor, @NotNull User user,
                               final List<ServerWorldClaim> claims,
                               int page, @NotNull SortOption sort, boolean ascend) {
        final Locales locales = plugin.getLocales();
        claims.sort(sort.getComparator());
        executor.sendMessage(PaginatedList.of(
                claims.stream().map(claim -> locales.getRawLocale(
                        "claim_list_item",
                        claim.serverWorld().toString(),
                        locales.getRawLocale("claim_position",
                                Integer.toString(claim.claim().getRegion().getNearCorner().getBlockX()),
                                Integer.toString(claim.claim().getRegion().getNearCorner().getBlockZ())
                        ).orElse(""),
                        Integer.toString(claim.claim().getRegion().getLongestEdge()),
                        Integer.toString(claim.claim().getRegion().getShortestEdge()),
                        Long.toString(claim.claim().getRegion().getSurfaceArea()),
                        Integer.toString(claim.claim().getChildren().size()),
                        Integer.toString(
                                claim.claim().getTrustedUsers().size() + claim.claim().getTrustedGroups().size()
                        )
                ).orElse("")).toList(),
                locales.getBaseList(CLAIMS_PER_PAGE)
                        .setHeaderFormat(getListTitle(locales, user, claims.size(), sort, ascend))
                        .setItemSeparator("\n")
                        .setCommand(String.format(
                                "/claimslist %s %s %s",
                                user.getName(),
                                sort.getId(),
                                (ascend ? "ascending" : "descending")
                        )).build()
        ).getNearestValidPage(page));
    }

    @NotNull
    private List<ServerWorldClaim> getUserClaims(@NotNull User user) {
        return plugin.getDatabase().getAllClaimWorlds().entrySet().stream()
                .flatMap(e -> e.getValue().getClaims().stream()
                        .filter(c -> user.getUuid().equals(c.getOwner().orElse(null)))
                        .map(c -> new ServerWorldClaim(e.getKey(), c)))
                .toList();
    }

    private Optional<SortOption> parseSortArg(@NotNull String[] args, int index) {
        return parseStringArg(args, index).flatMap(SortOption::matchSortOption);
    }

    private Optional<Boolean> parseOrderArg(@NotNull String[] args, int index) {
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
    private String getListTitle(@NotNull Locales locales, @NotNull User user, int claimCount,
                                @NotNull SortOption sort, boolean ascend) {
        return locales.getRawLocale(
                "claim_list_title",
                locales.getRawLocale(
                        String.format("claim_list_sort_%s", ascend ? "ascending" : "descending"),
                        sort.getId(),
                        "%current_page%"
                ).orElse(""),
                Locales.escapeText(user.getName()),
                Integer.toString(claimCount),
                locales.getRawLocale(
                        "claim_list_sort_options",
                        getSortButtons(locales, sort, ascend)
                ).orElse("")
        ).orElse("");
    }

    @NotNull
    private String getSortButtons(@NotNull Locales locales, @NotNull SortOption sort, boolean ascend) {
        final StringJoiner options = new StringJoiner(
                locales.getRawLocale("claim_list_sort_option_separator").orElse("|")
        );

        for (SortOption option : SortOption.values()) {
            boolean selected = option == sort;
            if (selected) {
                options.add(locales.getRawLocale("claim_list_sort_option_selected",
                                option.getDisplayName(plugin.getLocales()))
                        .orElse(option.getId()));
                continue;
            }
            options.add(locales.getRawLocale("claim_list_sort_option",
                            option.getDisplayName(plugin.getLocales()),
                            option.getId(),
                            ascend ? "ascending" : "descending", "%current_page%")
                    .orElse(option.getId()));
        }
        return options.toString();
    }

    @Getter
    @AllArgsConstructor
    public enum SortOption {
        SIZE(Comparator.comparing(c -> c.claim().getRegion().getSurfaceArea())),
        WORLD(Comparator.comparing(c -> c.serverWorld().toString())),
        TRUSTEES(Comparator.comparingInt(c -> c.claim().getTrustedUsers().size())),
        CHILDREN(Comparator.comparingInt(c -> c.claim().getChildren().size()));

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

    private record ServerWorldClaim(@NotNull ServerWorld serverWorld, @NotNull Claim claim) {
    }

}

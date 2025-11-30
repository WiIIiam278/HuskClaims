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
import net.william278.huskclaims.user.CommandUser;
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
        final List<String> parts = new ArrayList<>(List.of(
                plugin.getLocales().getPositionText(claim.claim().getRegion().getCenter(),
                        TELEPORT_Y_LEVEL, claim.serverWorld(), user, plugin),
                getClaimSize(claim.claim()),
                getClaimChildren(claim.claim()),
                getClaimMembers(claim.claim())
        ));
        
        // Add tax info if property tax is enabled
        final java.util.Optional<net.william278.huskclaims.claim.ClaimWorld> claimWorld =
                plugin.getClaimWorld(claim.serverWorld().world());
        if (claimWorld.isPresent()) {
            final String taxInfo = getClaimTax(claim.claim(), claimWorld.get());
            if (!taxInfo.isEmpty()) {
                parts.add(taxInfo);
            }
        }
        
        return plugin.getLocales().getRawLocale("claim_list_item_separator")
                .map(separator -> String.join(separator, parts))
                .orElse("");
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
    private String getClaimTax(@NotNull Claim claim, @NotNull net.william278.huskclaims.claim.ClaimWorld world) {
        try {
            final net.william278.huskclaims.config.Settings.ClaimSettings.PropertyTaxSettings taxSettings =
                    plugin.getSettings().getClaims().getPropertyTax();
            if (!taxSettings.isEnabled() || claim.isAdminClaim() || claim.getOwner().isEmpty()) {
                return "";
            }

            // Check if world is excluded
            if (taxSettings.getExcludedWorlds().contains(world.getName(plugin))) {
                return "";
            }

            // Get user's tax balance
            final java.util.Optional<net.william278.huskclaims.user.SavedUser> savedUser =
                    plugin.getDatabase().getUser(claim.getOwner().get());
            if (savedUser.isEmpty()) {
                return "";
            }

            // Check if user is excluded
            final net.william278.huskclaims.user.User owner = savedUser.get().getUser();
            if (taxSettings.getExcludedUsers().contains(owner.getUuid().toString())
                    || taxSettings.getExcludedUsers().contains(owner.getName())) {
                return "";
            }

            final double taxBalance = savedUser.get().getTaxBalance();
            final double taxOwed = plugin.getPropertyTaxManager().calculateTaxOwed(claim, world);
            final double totalOwed = taxOwed - taxBalance;

            final java.util.Optional<net.william278.huskclaims.hook.EconomyHook> hook =
                    plugin.getHook(net.william278.huskclaims.hook.EconomyHook.class);
            final String formattedAmount = hook.map(h -> h.format(Math.abs(totalOwed)))
                    .orElse(String.format("%.2f", Math.abs(totalOwed)));

            // Always show tax status if tax is enabled for this claim
            if (totalOwed > 0.01) { // Use small threshold to avoid floating point issues
                // Overdue or owing
                final long daysOverdue = plugin.getPropertyTaxManager().getDaysOverdue(claim, world, taxBalance);
                if (daysOverdue >= taxSettings.getDueDays()) {
                    return plugin.getLocales().getRawLocale("claim_list_tax_overdue", formattedAmount).orElse("");
                } else {
                    return plugin.getLocales().getRawLocale("claim_list_tax_owed", formattedAmount).orElse("");
                }
            } else if (taxBalance > 0.01) {
                // Prepaid
                return plugin.getLocales().getRawLocale("claim_list_tax_prepaid", formattedAmount).orElse("");
            } else {
                // Show current status even if 0 (for new claims, show rate info or "paid")
                if (taxOwed <= 0.01 && taxBalance >= 0.01) {
                    // Tax is paid up
                    return plugin.getLocales().getRawLocale("claim_list_tax_paid").orElse("");
                } else {
                    // Show daily rate for new claims
                    final double taxRate = plugin.getPropertyTaxManager().getTaxRate(savedUser.get().getUser());
                    final long claimBlocks = claim.getRegion().getSurfaceArea();
                    final double dailyTax = claimBlocks * taxRate;
                    if (dailyTax > 0.01) {
                        final String dailyFormatted = hook.map(h -> h.format(dailyTax)).orElse(String.format("%.2f", dailyTax));
                        return plugin.getLocales().getRawLocale("claim_list_tax_current", dailyFormatted).orElse("");
                    }
                }
            }

            return "";
        } catch (Exception e) {
            // Silently fail if tax calculation fails (e.g., database not ready)
            return "";
        }
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
                        .orElse(option.getDisplayName(plugin.getLocales())));
                continue;
            }
            options.add(locales.getRawLocale("claim_list_sort_option",
                            option.getDisplayName(plugin.getLocales()),
                            getName(),
                            option.getId(),
                            ascend ? "ascending" : "descending", "%current_page%")
                    .orElse(option.getDisplayName(plugin.getLocales())));
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

        public static final List<SortOption> LISTED_OPTIONS = List.of(SIZE, WORLD, CHILDREN);

        private final Comparator<ServerWorldClaim> comparator;

        @NotNull
        public String getDisplayName(@NotNull Locales locales) {
            return locales.getRawLocale(String.format("claim_list_sort_option_label_%s", getId())).orElse(getId());
        }

        @NotNull
        public String getId() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Optional<SortOption> matchSortOption(@NotNull String text) {
            return Arrays.stream(values())
                    .filter(o -> o.getId().equals(text.toLowerCase(Locale.ENGLISH)))
                    .findFirst();
        }
    }

}

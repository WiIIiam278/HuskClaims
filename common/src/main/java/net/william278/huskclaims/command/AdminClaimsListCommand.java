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
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public class AdminClaimsListCommand extends ClaimsListCommand implements GlobalClaimsProvider {
    @Nullable
    @Unmodifiable
    private List<ServerWorldClaim> adminClaims = null;

    protected AdminClaimsListCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("adminclaimslist", "adminclaims"),
                "[sort_by] [ascending|descending] [page]",
                plugin
        );
        setOperatorCommand(true);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final SortOption sort = parseSortArg(args, 0).orElse(SortOption.DIMENSION);
        final boolean ascend = parseOrderArg(args, 1).orElse(false);
        final int page = parseIntArg(args, 2).orElse(1);

        showAdminClaimList(executor, page, sort, ascend);
    }

    protected void showAdminClaimList(@NotNull CommandUser executor, int page,
                                      @NotNull SortOption sort, boolean ascend) {
        if (adminClaims != null) {
            showClaimList(executor, null, Lists.newArrayList(adminClaims), page, sort, ascend);
            return;
        }

        final List<ServerWorldClaim> claims = Lists.newArrayList(getAdminClaims());
        if (claims.isEmpty()) {
            plugin.getLocales().getLocale("error_no_admin_claims_made")
                    .ifPresent(executor::sendMessage);
            return;
        }
        adminClaims = claims;

        showClaimList(executor, null, claims, page, sort, ascend);
    }

    protected void invalidateCache() {
        adminClaims = null;
    }


    @Override
    @NotNull
    protected String getListTitle(@NotNull Locales locales, @Nullable User user, int claimCount,
                                  @NotNull SortOption sort, boolean ascend) {
        return locales.getRawLocale(
                "admin_claim_list_title",
                locales.getRawLocale(
                        String.format("claim_list_sort_%s", ascend ? "ascending" : "descending"),
                        getName(),
                        sort.getId(),
                        "%current_page%"
                ).orElse(""),
                Integer.toString(claimCount),
                locales.getRawLocale(
                        "claim_list_sort_options",
                        getSortButtons(locales, sort, ascend)
                ).orElse("")
        ).orElse("");
    }

}

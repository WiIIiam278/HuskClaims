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

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static net.william278.huskclaims.command.ClaimsListCommand.SortOption.SIZE;

public class ClaimsListCommand extends Command implements UserListTabCompletable {

    private static final int CLAIMS_PER_PAGE = 10;
    private final Map<UUID, Map<ServerWorld, List<Claim>>> claimLists = Maps.newHashMap();

    protected ClaimsListCommand(@NotNull HuskClaims plugin) {
        super(List.of("claimslist", "claims"), "<player>", plugin);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<User> optionalUser = parseStringArg(args, 0).flatMap(a -> resolveUser(executor, a));
        final int page = Math.min(1, parseIntArg(args, 0).or(() -> parseIntArg(args, 1)).orElse(1));
        final SortOption sort = parseSortArg(args, 1).or(() -> parseSortArg(args, 2)).orElse(SIZE);
        if (optionalUser.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }
        showClaimList(executor, optionalUser.get(), page, sort);
    }

    // Show a claims list
    private void showClaimList(@NotNull CommandUser executor, @NotNull User user, int page, @NotNull SortOption sort) {
        if (claimLists.containsKey(user.getUuid())) {
            showClaimList(user, page, sort);
            return;
        }

        final Map<ServerWorld, List<Claim>> claims = getUserClaims(user);
        if (claims.isEmpty()) {
            plugin.getLocales().getLocale("error_no_claims_made", user.getName())
                    .ifPresent(executor::sendMessage);
            return;
        }
        claimLists.put(user.getUuid(), claims);
        showClaimList(user, page, sort);
    }

    private void showClaimList(@NotNull User user, int page, @NotNull SortOption sort) {
        final Map<ServerWorld, List<Claim>> claims = claimLists.get(user.getUuid());
        plugin.getLocales().getBaseList(CLAIMS_PER_PAGE)
                .setCommand(getAliases().get(0));
        //todo
    }

    @NotNull
    private Optional<User> resolveUser(@NotNull CommandUser executor, @NotNull String name) {
        return plugin.getDatabase().getUser(name).map(SavedUser::user).or(() -> {
            if (executor instanceof OnlineUser online) {
                return Optional.of(online);
            }
            return Optional.empty();
        });
    }

    @NotNull
    public Map<ServerWorld, List<Claim>> getUserClaims(@NotNull User user) {
        return plugin.getDatabase().getAllClaimWorlds().entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().getClaimsByUser(user.getUuid())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Optional<SortOption> parseSortArg(@NotNull String[] args, int index) {
        return parseStringArg(args, index).flatMap(SortOption::matchSortOption);
    }

    @AllArgsConstructor
    public enum SortOption {
        SIZE,
        WORLD,
        TRUSTEES,
        CHILDREN;

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

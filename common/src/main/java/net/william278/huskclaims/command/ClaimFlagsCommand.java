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
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.paginedown.PaginatedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClaimFlagsCommand extends OnlineUserCommand implements TabCompletable {

    private final int CLAIM_FLAGS_PER_PAGE = 8;
    private final TrustLevel.Privilege MANAGE_FLAGS = TrustLevel.Privilege.MANAGE_OPERATION_GROUPS;

    public ClaimFlagsCommand(@NotNull HuskClaims plugin) {
        super(List.of("claimflags"), "[set|list]", plugin);

        this.setOperatorCommand(true);
        this.addAdditionalPermissions(Map.of(
                "other", true,
                "world", true
        ));
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final String action = parseStringArg(args, 0).orElse("list");
        this.handleFlagsCommand(executor, action, removeFirstArg(args));
    }

    @Override
    @Nullable
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        final boolean setting = parseStringArg(args, 0).map(a -> a.equals("set")).orElse(false);
        return switch (args.length) {
            case 0, 1 -> Lists.newArrayList("set", "list");
            case 2 -> setting ? Arrays.stream(OperationType.values()).map(Enum::name).toList() : null;
            case 3 -> setting ? List.of("true", "false") : null;
            default -> null;
        };
    }

    private void handleFlagsCommand(@NotNull OnlineUser executor, @NotNull String action, @NotNull String[] args) {
        final Optional<ClaimWorld> optionalWorld = plugin.getClaimWorld(executor.getWorld());
        if (optionalWorld.isEmpty()) {
            plugin.getLocales().getLocale("world_not_claimable")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Resolve the region being modified
        final ClaimWorld world = optionalWorld.get();
        final @Nullable Claim claim = world.getClaimAt(executor.getPosition()).orElse(null);
        if (!canManageFlagsIn(executor, claim)) {
            plugin.getLocales().getLocale(claim != null ? "no_claim_privilege" : "error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        switch (action.toLowerCase(Locale.ENGLISH)) {
            case "set" -> this.handleSetClaimFlag(executor, claim, world, args);
            case "list" -> this.sendClaimFlagsList(executor, claim, world, parseIntArg(args, 0).orElse(1));
        }
    }

    private void handleSetClaimFlag(@NotNull OnlineUser onlineUser, @Nullable Claim claim,
                                    @NotNull ClaimWorld world, @NotNull String[] args) {
        final Optional<OperationType> operationType = parseOperationTypeArg(args, 0);
        if (operationType.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(onlineUser::sendMessage);
            return;
        }

        // Parse operation type
        final OperationType type = operationType.get();
        final boolean value = parseBooleanArg(args, 1).orElse(claim == null
                ? world.getWildernessFlags().contains(type)
                : claim.getDefaultFlags().contains(type));

        // Update flags
        final Collection<OperationType> types = claim == null ? world.getWildernessFlags() : claim.getDefaultFlags();
        if (value) {
            types.add(type);
        } else {
            types.remove(type);
        }
        plugin.getDatabase().updateClaimWorld(world);

        // Send flag list on correct page to indicated the update
        final double changedIndex = Lists.newArrayList(OperationType.values()).indexOf(type);
        final int changedPage = (int) Math.ceil(changedIndex / CLAIM_FLAGS_PER_PAGE);
        this.sendClaimFlagsList(onlineUser, claim, world, changedPage);
    }

    private void sendClaimFlagsList(@NotNull OnlineUser onlineUser, @Nullable Claim claim,
                                    @NotNull ClaimWorld world, int page) {
        final String header;
        if (claim != null) {
            header = plugin.getLocales().getRawLocale("claim_flags_header", claim.getOwnerName(world, plugin))
                    .orElse(claim.getOwnerName(world, plugin));
        } else {
            header = plugin.getLocales().getRawLocale("claim_flags_wilderness_header", world.getName(plugin))
                    .orElse(world.getName(plugin));
        }

        onlineUser.sendMessage(
                PaginatedList.of(
                        Arrays.stream(OperationType.values())
                                .map(op -> plugin.getLocales().getRawLocale("claim_flag_%s"
                                                .formatted((claim == null ? world.getWildernessFlags() : claim.getDefaultFlags())
                                                        .contains(op) ? "enabled" : "disabled"),
                                        op.name()
                                ).orElse(op.name())).toList(),
                        plugin.getLocales().getBaseList(CLAIM_FLAGS_PER_PAGE)
                                .setCommand("/%s list".formatted(getName()))
                                .setHeaderFormat(header).setItemSeparator("\n")
                                .build()
                ).getNearestValidPage(page)
        );
    }

    // Check that the user has permission to modify flags in this claim
    private boolean canManageFlagsIn(@NotNull OnlineUser user, @Nullable Claim claim) {
        if (claim == null) {
            return hasPermission(user, "world");
        }
        return hasPermission(user, "other") || claim.isPrivilegeAllowed(MANAGE_FLAGS, user, plugin);
    }

}

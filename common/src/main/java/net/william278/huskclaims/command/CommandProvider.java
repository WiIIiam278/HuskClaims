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
import net.william278.huskclaims.user.CommandUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provider for HuskClaims commands
 *
 * @since 1.0
 */
public interface CommandProvider {

    /**
     * Get the list of registered commands
     *
     * @return the list of commands
     * @since 1.0
     */
    @NotNull
    List<Command> getCommands();

    /**
     * Get a command by its class
     *
     * @param commandClass the class of the command to get
     * @param <T>          the type of command
     * @return the command, or null if not found
     */
    default <T extends Command> Optional<T> getCommand(@NotNull Class<T> commandClass) {
        return getCommands().stream()
                .filter(commandClass::isInstance)
                .map(commandClass::cast)
                .findFirst();
    }

    /**
     * Returns whether a user can use a command
     *
     * @param commandClass the class of the command to check
     * @param user         the user to check
     * @param nodes        the nodes to check
     * @param <T>          the type of command
     * @return whether the user can use the command
     */
    default <T extends Command> boolean canUseCommand(@NotNull Class<T> commandClass, @NotNull CommandUser user,
                                                      @NotNull String... nodes) {
        return getCommand(commandClass).map(command -> command.hasPermission(user, nodes)).orElse(false);
    }

    /**
     * Register a batch of command with the platform implementation
     *
     * @param commands the list of commands to register
     * @since 1.0
     */
    void registerCommands(@NotNull List<Command> commands);

    /**
     * Registers all plugin commands: built-in commands, trust level commands, then operation group commands
     *
     * @since 1.0
     */
    default void loadCommands() {
        final List<Command> commands = Lists.newArrayList();

        // Register built-in commands
        commands.add(new HuskClaimsCommand(getPlugin()));
        commands.add(new ClaimFlagsCommand(getPlugin()));
        commands.add(new TrustListCommand(getPlugin()));
        commands.add(new UnTrustCommand(getPlugin()));
        commands.add(new TransferClaimCommand(getPlugin()));
        commands.add(new UserGroupsCommand(getPlugin()));
        commands.add(new UnClaimCommand(getPlugin()));
        commands.add(new UnClaimAllCommand(getPlugin()));
        commands.add(new UserClaimsListCommand(getPlugin()));
        commands.add(new AdminClaimsListCommand(getPlugin()));
        commands.add(new ExtendClaimCommand(getPlugin()));
        commands.add(new ClaimBlocksCommand(getPlugin()));
        commands.add(new RestrictClaimCommand(getPlugin()));
        commands.add(new IgnoreClaimsCommand(getPlugin()));

        // Register sign spying
        if (getPlugin().getSettings().getModeration().getSigns().isNotifyModerators()) {
            commands.add(new SignSpyCommand(getPlugin()));
        }

        // Register drop unlocking
        if (getPlugin().getSettings().getModeration().getDrops().isLockItems()) {
            commands.add(new UnlockDropsCommand(getPlugin()));
        }

        // Register pet transfer command
        if (getPlugin().getSettings().getPets().isEnabled()) {
            commands.add(new TransferPetCommand(getPlugin()));
        }

        // Register claim block purchasing
        if (getPlugin().getSettings().getHooks().getEconomy().isEnabled()) {
            commands.add(new BuyClaimBlocksCommand(getPlugin()));
        }

        // Register claim banning
        if (getPlugin().getSettings().getClaims().getBans().isEnabled()) {
            commands.add(new ClaimBanCommand(getPlugin()));
        }

        if (getPlugin().getSettings().getClaims().getBans().isPrivateClaims()) {
            commands.add(new ClaimPrivateCommand(getPlugin()));
        }

        // Register claim commands
        getPlugin().getSettings().getClaims().getEnabledClaimingModes().stream()
                .filter(mode -> !mode.getCommandAliases().isEmpty())
                .forEach((mode) -> commands.add(new ClaimCommand(mode, getPlugin())));

        // Register trust level commands
        getPlugin().getTrustLevels().stream()
                .filter(level -> !level.getCommandAliases().isEmpty())
                .forEach((level) -> commands.add(new TrustCommand(level, getPlugin())));

        // Register operation group commands
        getPlugin().getSettings().getOperationGroups().stream()
                .filter(group -> !group.getToggleCommandAliases().isEmpty())
                .forEach((group) -> commands.add(new OperationGroupCommand(group, getPlugin())));

        // Sort and register
        commands.sort(Comparator.comparing(Node::getName));
        registerCommands(commands);
    }

    /**
     * Invalidates the cached claim lists for a user
     *
     * @param userUuid the UUID of the user to invalidate the claim lists for,
     *                 or null to invalidate all admin claim lists
     * @since 1.0
     */
    default void invalidateClaimListCache(@Nullable UUID userUuid) {
        boolean admin = userUuid == null;
        getCommands().stream()
                .filter(c -> admin ? c instanceof AdminClaimsListCommand : c instanceof UserClaimsListCommand)
                .findFirst().ifPresent(c -> {
                    if (admin) {
                        ((AdminClaimsListCommand) c).invalidateCache();
                    } else {
                        ((UserClaimsListCommand) c).invalidateCache(userUuid);
                    }
                });
    }

    /**
     * Invalidates all cached admin claim lists
     *
     * @since 1.0
     */
    default void invalidateAdminClaimListCache() {
        invalidateClaimListCache(null);
    }

    @NotNull
    HuskClaims getPlugin();

}

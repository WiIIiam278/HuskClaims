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
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
        commands.add(new UnTrustCommand(getPlugin()));
        commands.add(new TrustListCommand(getPlugin()));
        commands.add(new UserGroupsCommand(getPlugin()));
        commands.add(new UnClaimCommand(getPlugin()));

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

        registerCommands(commands);
    }

    @NotNull
    HuskClaims getPlugin();

}

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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.user.CommandUser;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FabricCommand {

    private final FabricHuskClaims plugin;
    private final Command command;

    public FabricCommand(@NotNull Command command, @NotNull FabricHuskClaims plugin) {
        this.command = command;
        this.plugin = plugin;
    }

    public void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register brigadier command
        final Predicate<ServerCommandSource> predicate = Permissions
                .require(command.getPermission(), command.isOperatorCommand() ? 3 : 0);
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(command.getName())
                .requires(predicate).executes(getBrigadierExecutor());
        plugin.getPermissions().put(command.getPermission(), command.isOperatorCommand());
        if (!command.getRawUsage().isBlank()) {
            builder.then(argument(command.getRawUsage().replaceAll("[<>\\[\\]]", ""), greedyString())
                    .executes(getBrigadierExecutor())
                    .suggests(getBrigadierSuggester()));
        }

        // Register additional permissions
        final Map<String, Boolean> permissions = command.getAdditionalPermissions();
        permissions.forEach((permission, isOp) -> plugin.getPermissions().put(permission, isOp));
        PermissionCheckEvent.EVENT.register((player, node) -> {
            if (permissions.containsKey(node) && permissions.get(node) &&
                    (!(player instanceof ServerCommandSource source) || source.hasPermissionLevel(3))) {
                return TriState.TRUE;
            }
            return TriState.DEFAULT;
        });

        // Register aliases
        final LiteralCommandNode<ServerCommandSource> node = dispatcher.register(builder);
        dispatcher.register(literal("huskclaims:" + command.getName())
                .requires(predicate).executes(getBrigadierExecutor()).redirect(node));
        command.getAliases().forEach(alias -> dispatcher.register(literal(alias)
                .requires(predicate).executes(getBrigadierExecutor()).redirect(node)));
    }

    private com.mojang.brigadier.Command<ServerCommandSource> getBrigadierExecutor() {
        return (context) -> {
            command.onExecuted(
                    resolveExecutor(context.getSource()),
                    command.removeFirstArg(context.getInput().split(" "))
            );
            return 1;
        };
    }

    private com.mojang.brigadier.suggestion.SuggestionProvider<ServerCommandSource> getBrigadierSuggester() {
        if (!(command instanceof TabCompletable provider)) {
            return (context, builder) -> com.mojang.brigadier.suggestion.Suggestions.empty();
        }
        return (context, builder) -> {
            final String[] args = command.removeFirstArg(context.getInput().split(" ", -1));
            provider.getSuggestions(resolveExecutor(context.getSource()), args).stream()
                    .map(suggestion -> {
                        final String completedArgs = String.join(" ", args);
                        int lastIndex = completedArgs.lastIndexOf(" ");
                        if (lastIndex == -1) {
                            return suggestion;
                        }
                        return completedArgs.substring(0, lastIndex + 1) + suggestion;
                    })
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    @NotNull
    private CommandUser resolveExecutor(@NotNull ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            return plugin.getOnlineUser(player);
        }
        return plugin.getConsole();
    }

}

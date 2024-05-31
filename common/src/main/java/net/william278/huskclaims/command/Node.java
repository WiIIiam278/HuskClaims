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
import lombok.Getter;
import lombok.Setter;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class Node implements Executable {

    protected static final String PERMISSION_PREFIX = "huskclaims.command";

    protected final HuskClaims plugin;
    @Getter
    private final List<String> aliases;
    @Getter
    @Setter
    private boolean operatorCommand = false;

    protected Node(@NotNull List<String> aliases, @NotNull HuskClaims plugin) {
        if (aliases.isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be blank");
        }
        this.aliases = aliases;
        this.plugin = plugin;
    }

    @NotNull
    public String getPermission(@NotNull String... child) {
        final StringJoiner joiner = new StringJoiner(".")
                .add(PERMISSION_PREFIX)
                .add(getName());
        for (String node : child) {
            joiner.add(node);
        }
        return joiner.toString().trim();
    }

    public boolean hasPermission(@NotNull CommandUser executor, @NotNull String... child) {
        return executor.hasPermission(getPermission(child)) || executor.hasPermission(getPermission("*"));
    }

    @NotNull
    public String getName() {
        if (aliases.isEmpty()) {
            throw new IllegalStateException("Primary alias of command node is blank");
        }
        return aliases.get(0);
    }

    protected Optional<User> resolveUser(@NotNull CommandUser executor, @NotNull String[] args) {
        return parseStringArg(args, 0)
                .flatMap(a -> plugin.getDatabase().getUser(a)).map(SavedUser::getUser)
                .or(() -> {
                    if (executor instanceof OnlineUser online) {
                        return Optional.of(online);
                    }
                    return Optional.empty();
                });
    }

    protected Optional<OnlineUser> resolveOnlineUser(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<OnlineUser> user = parseStringArg(args, 0)
                .flatMap(a -> plugin.getOnlineUsers().stream()
                        .filter(o -> o.getName().equalsIgnoreCase(a)).findFirst());
        if (user.isEmpty() && executor instanceof OnlineUser online) {
            return Optional.of(online);
        }
        return user;
    }

    @NotNull
    protected List<String> parseStringList(@NotNull String[] args, int index) {
        final List<String> names = Lists.newArrayList();
        if (args.length <= index) {
            return names;
        }
        Arrays.asList(args).subList(index, args.length).stream()
                .filter(n -> names.stream().noneMatch(n::equalsIgnoreCase))
                .forEach(names::add);
        return names;
    }

    @NotNull
    protected Map<String, String> parseKeyValues(@NotNull String[] args, int index) {
        final List<String> list = parseStringList(args, index);
        final Map<String, String> map = Maps.newHashMap();
        for (String s : list) {
            final String[] split = s.split(":", 2);
            if (split.length >= 2) {
                map.put(split[0], split[1]);
            }
        }
        return map;
    }

    protected Optional<String> parseStringArg(@NotNull String[] args, int index) {
        if (args.length > index) {
            return Optional.of(args[index]);
        }
        return Optional.empty();
    }

    protected boolean parseConfirmArg(@NotNull String[] args) {
        return args.length > 0 && parseStringArg(args, args.length - 1)
                .map(arg -> arg.toLowerCase(Locale.ENGLISH).equals("confirm"))
                .orElse(false);
    }

    protected Optional<Position> parsePositionArgs(@NotNull String[] args, int index) {
        final Optional<Integer> x = parseIntArg(args, index);
        final Optional<Integer> y = parseIntArg(args, index + 1);
        final Optional<Integer> z = parseIntArg(args, index + 2);
        final Optional<String> world = parseStringArg(args, index + 3);
        if (x.isPresent() && y.isPresent() && z.isPresent() && world.isPresent()) {
            return Optional.of(Position.at(
                    x.get(), y.get(), z.get(),
                    World.of(world.get(), UUID.randomUUID(), "normal")
            ));
        }
        return Optional.empty();
    }

    protected Optional<OperationType> parseOperationTypeArg(@NotNull String[] args, int index) {
        return parseStringArg(args, index).flatMap(arg -> {
            try {
                return Optional.of(OperationType.valueOf(arg.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        });
    }

    protected Optional<Long> parseClaimBlocksArg(@NotNull String[] args, int index) {
        return parseStringArg(args, index).flatMap(arg -> {
            try {
                return Optional.of(Math.min(SavedUser.MAX_CLAIM_BLOCKS, Math.abs(Long.parseLong(arg))));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    protected Optional<Integer> parseIntArg(@NotNull String[] args, int index) {
        return parseStringArg(args, index).flatMap(arg -> {
            try {
                return Optional.of(Integer.parseInt(arg));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    protected Optional<Boolean> parseBooleanArg(@NotNull String[] args, int index) {
        if (args.length <= index) {
            return Optional.empty();
        }
        final String arg = args[index].toLowerCase(Locale.ROOT);
        if (arg.equals("true") || arg.equals("enable") || arg.equals("on")) {
            return Optional.of(true);
        } else if (arg.equals("false") || arg.equals("disable") || arg.equals("off")) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

}

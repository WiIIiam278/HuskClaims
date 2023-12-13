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

import lombok.Getter;
import lombok.Setter;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

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
        for (final String node : child) {
            joiner.add(node);
        }
        return joiner.toString().trim();
    }

    @NotNull
    public String getName() {
        return aliases.get(0);
    }

    protected Optional<String> parseStringArg(@NotNull String[] args, int index) {
        if (args.length > index) {
            return Optional.of(args[index]);
        }
        return Optional.empty();
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

    protected Optional<String> parseGreedyArguments(@NotNull String[] args) {
        if (args.length > 1) {
            final StringJoiner sentence = new StringJoiner(" ");
            for (int i = 1; i < args.length; i++) {
                sentence.add(args[i]);
            }
            return Optional.of(sentence.toString().trim());
        }
        return Optional.empty();
    }

}

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

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.importer.GPImporter;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.ConsoleUser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class GPImportCommand extends Command {

    private final BukkitHuskClaims plugin;
    private final Map<String, String> confirm;

    public GPImportCommand(@NotNull BukkitHuskClaims plugin) {
        super(List.of("gpi", "griefpreventionimport"), "gpi <uri> <username> <password> / <confirm_code>", plugin);
        this.plugin = plugin;
        this.confirm = new HashMap<>();
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        if (!(executor instanceof ConsoleUser)) {
            plugin.getLocales().getLocale("error_command_console_only").ifPresent(executor::sendMessage);
        }

        if (args.length != 3 && args.length != 1) {
            plugin.getLocales().getLocale("error_command_invalid_syntax", getRawUsage()).ifPresent(executor::sendMessage);
            return;
        }

        final String code = args.length == 1 ? args[0] : "";
        final GPImporter importer = plugin.getHook(GPImporter.class).orElseThrow();
        String[] credentials = confirm.getOrDefault(code, "").split(" ");
        credentials = credentials.length == 3 ? credentials : new String[]{"", "", ""};
        final String uri = args.length == 3 ? args[0] : credentials[0];
        final String username = args.length == 3 ? args[1] : credentials[1];
        final String password = args.length == 3 ? args[2] : credentials[2];

        if (code.isEmpty()) {
            String random = UUID.randomUUID().toString().substring(0, 5);
            confirm.put(random, uri + " " + username + " " + password);
            plugin.getLocales().getLocale("confirm_code", random).ifPresent(executor::sendMessage);
            return;
        }

        if (!confirm.containsKey(code)) {
            plugin.getLocales().getLocale("error_gp_wrong_code").ifPresent(executor::sendMessage);
            return;
        }

        confirm.remove(code);


        plugin.runAsync(() -> {
            try {
                importer.prepare(Map.of("uri", uri, "username", username, "password", password));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to prepare GP importer", e);
                return;
            }

            importer.start(executor);
        });
    }
}

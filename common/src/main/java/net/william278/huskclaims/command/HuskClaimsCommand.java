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

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.william278.desertwell.about.AboutMenu;
import net.william278.desertwell.util.UpdateChecker;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.hook.HuskHomesHook;
import net.william278.huskclaims.hook.Importer;
import net.william278.huskclaims.hook.PluginHook;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.AuditLogger;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.paginedown.PaginatedList;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HuskClaimsCommand extends Command implements TabCompletable {

    private static final int ITEMS_PER_LIST_PAGE = 8;
    private static final Map<String, Boolean> SUB_COMMANDS = Map.of(
            "about", false,
            "help", false,
            "teleport", true,
            "logs", true,
            "status", true,
            "import", true,
            "reload", true,
            "update", true
    );

    private final UpdateChecker updateChecker;
    private final AboutMenu aboutMenu;

    protected HuskClaimsCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("huskclaims"),
                "[" + String.join("|", SUB_COMMANDS.keySet()) + "]",
                plugin
        );
        addAdditionalPermissions(SUB_COMMANDS);

        this.updateChecker = plugin.getUpdateChecker();
        this.aboutMenu = AboutMenu.builder()
                .title(Component.text("HuskClaims"))
                .description(Component.text("A clean, cross-server compatible grief prevention plugin"))
                .version(plugin.getPluginVersion())
                .credits("Author",
                        AboutMenu.Credit.of("William278").description("Click to visit website").url("https://william278.net"))
                .credits("Contributors",
                        AboutMenu.Credit.of("AlexDev_").description("Code"),
                        AboutMenu.Credit.of("djoey123").description("Code"))
                .credits("Translators",
                        AboutMenu.Credit.of("jhqwqmc").description("Simplified Chinese (zh-cn)"),
                        AboutMenu.Credit.of("yichifauzi").description("Traditional Chinese (zh-tw)"),
                        AboutMenu.Credit.of("Artem4ikBaik").description("Russian (ru-ru)"),
                        AboutMenu.Credit.of("Edward205").description("Romanian (ro-ro)"),
                        AboutMenu.Credit.of("iRazvan2745").description("Romanian (ro-ro)")
                )
                .buttons(
                        AboutMenu.Link.of("https://william278.net/docs/huskclaims").text("Documentation").icon("⛏"),
                        AboutMenu.Link.of("https://github.com/WiIIiam278/HuskClaims/issues").text("Issues").icon("❌").color(TextColor.color(0xff9f0f)),
                        AboutMenu.Link.of("https://discord.gg/tVYhJfyDWG").text("Discord").icon("⭐").color(TextColor.color(0x6773f5)))
                .build();
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final String subCommand = parseStringArg(args, 0).orElse("about").toLowerCase(Locale.ENGLISH);
        if (SUB_COMMANDS.containsKey(subCommand) && !executor.hasPermission(getPermission(subCommand))) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        switch (subCommand) {
            case "about" -> executor.sendMessage(aboutMenu.toComponent());
            case "help" -> executor.sendMessage(
                    getCommandList(executor).getNearestValidPage(parseIntArg(args, 1).orElse(1))
            );
            case "teleport" -> handleTeleportCommand(executor, removeFirstArg(args));
            case "logs" -> handleLogsCommand(executor, removeFirstArg(args));
            case "status" -> {
                getPlugin().getLocales().getLocale("system_status_header").ifPresent(executor::sendMessage);
                executor.sendMessage(Component.join(
                        JoinConfiguration.newlines(),
                        Arrays.stream(StatusLine.values()).map(s -> s.get(plugin)).toList()
                ));
            }
            case "import" -> handleImportCommand(executor, removeFirstArg(args));
            case "reload" -> plugin.runSync(() -> {
                try {
                    plugin.unloadHooks();
                    plugin.loadLocales();
                    plugin.loadSettings();
                    plugin.loadHooks();
                    plugin.loadTrustLevels();
                    plugin.registerHooks(PluginHook.Register.values());
                    plugin.getLocales().getLocale("reload_complete").ifPresent(executor::sendMessage);
                } catch (Throwable e) {
                    executor.sendMessage(new MineDown(
                            "[Error:](#ff3300) [Failed to reload the plugin. Check console for errors.](#ff7e5e)"
                    ));
                    plugin.log(Level.SEVERE, "Failed to reload the plugin", e);
                }
            });
            case "update" -> updateChecker.check().thenAccept(checked -> {
                if (checked.isUpToDate()) {
                    plugin.getLocales().getLocale("up_to_date", plugin.getPluginVersion().toString())
                            .ifPresent(executor::sendMessage);
                    return;
                }
                plugin.getLocales().getLocale("update_available", checked.getLatestVersion().toString(),
                        plugin.getPluginVersion().toString()).ifPresent(executor::sendMessage);
            });
            default -> plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
        }
    }

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> SUB_COMMANDS.keySet().stream().filter(n -> user.hasPermission(getPermission(n))).toList();
            default -> switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "import" -> switch (args.length - 2) {
                    case 0 -> plugin.getImporters().stream().map(Importer::getName).toList();
                    case 1 -> List.of("start", "set", "reset");
                    default -> plugin.getImporters().stream().filter(i -> i.getName().equalsIgnoreCase(args[1]))
                            .flatMap(i -> i.getRequiredParameters().keySet().stream()
                                    .filter(p -> parseKeyValues(args, 3).keySet().stream()
                                            .noneMatch(p::equalsIgnoreCase))
                                    .map("%s:"::formatted)).toList();
                };
                case "help" -> IntStream.rangeClosed(1, getCommandList(user).getTotalPages())
                        .mapToObj(Integer::toString).toList();
                default -> null;
            };
        };
    }

    private void handleImportCommand(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<Importer> optionalImporter = parseStringArg(args, 0).flatMap(plugin::getImporterByName);
        if (optionalImporter.isEmpty()) {
            plugin.getLocales().getLocale("available_importers", plugin.getImporters().stream()
                            .map(Importer::getName).collect(Collectors.joining(", ")))
                    .ifPresent(executor::sendMessage);
            return;
        }

        final Importer importer = optionalImporter.get();
        switch (parseStringArg(args, 1).map(a -> a.toLowerCase(Locale.ENGLISH)).orElse("start")) {
            case "start", "begin" -> importer.start(executor);
            case "set", "config", "configure" -> parseKeyValues(args, 2)
                    .forEach((k, v) -> importer.setValue(executor, k, v, k.equalsIgnoreCase("password")));
            case "reset" -> importer.reset(executor);
            default -> plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
        }
    }

    private void handleLogsCommand(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<SavedUser> optionalUser = parseStringArg(args, 0).flatMap(plugin.getDatabase()::getUser);
        if (optionalUser.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        final SavedUser user = optionalUser.get();
        final List<AuditLogger.TimestampedEntry> auditLog = user.getPreferences().getTimestampedLogEntries();
        if (auditLog.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_user", user.getUser().getName())
                    .ifPresent(executor::sendMessage);
            return;
        }

        final Locales locales = plugin.getLocales();
        final String userName = user.getUser().getName();
        final PaginatedList list = PaginatedList.of(auditLog
                        .stream().map(
                                entry -> locales.getRawLocale("audit_log_row",
                                        DateTimeFormatter.ofPattern("dd MMM, yyyy HH:mm").format(entry.timestamp()),
                                        Locales.escapeText(entry.entry().getAction().getFormattedName()),
                                        entry.entry().getUser() != null ? Locales.escapeText(entry.entry().getUser().getName()) : "",
                                        entry.entry().getMessage() != null ? Locales.escapeText(entry.entry().getMessage()) : ""
                                ).orElse("")
                        ).toList(),
                locales.getBaseList(ITEMS_PER_LIST_PAGE)
                        .setItemSeparator("\n").setCommand("/%s logs %s".formatted(getName(), userName))
                        .setHeaderFormat(locales.getRawLocale("audit_log_header",
                                Locales.escapeText(userName)).orElse(""))
                        .build()
        );
        executor.sendMessage(list.getNearestValidPage(parseIntArg(args, 1).orElse(1)));
    }

    private void handleTeleportCommand(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<Position> position = parsePositionArgs(args, 0)
                .or(() -> parsePositionArgs(args, 1));
        final String server = parseStringArg(args, 0).orElse(plugin.getServerName());
        if (position.isEmpty() || !(executor instanceof OnlineUser online)) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }
        plugin.getHook(HuskHomesHook.class).ifPresentOrElse(
                hook -> hook.teleport(online, position.get(), server),
                () -> plugin.getLocales().getLocale("error_huskhomes_not_found")
                        .ifPresent(executor::sendMessage)
        );
    }

    @NotNull
    private PaginatedList getCommandList(@NotNull CommandUser executor) {
        final Locales locales = plugin.getLocales();
        return PaginatedList.of(
                plugin.getCommands().stream()
                        .filter(command -> command.hasPermission(executor))
                        .map(command -> locales.getRawLocale("command_list_row",
                                Locales.escapeText(String.format("/%s", command.getName())),
                                Locales.escapeText(command.getUsage()),
                                Locales.escapeText(locales.truncateText(command.getDescription(), 40)),
                                Locales.escapeText(command.getDescription())
                        ).orElse(command.getUsage()))
                        .toList(),
                locales.getBaseList(ITEMS_PER_LIST_PAGE)
                        .setHeaderFormat(locales.getRawLocale("command_list_header").orElse(""))
                        .setItemSeparator("\n").setCommand("/%s help".formatted(getName()))
                        .build()
        );
    }

    private enum StatusLine {
        PLUGIN_VERSION(plugin -> Component.text("v" + plugin.getPluginVersion().toStringWithoutMetadata())
                .appendSpace().append(plugin.getPluginVersion().getMetadata().isBlank() ? Component.empty()
                        : Component.text("(build " + plugin.getPluginVersion().getMetadata() + ")"))),
        SERVER_VERSION(plugin -> Component.text(plugin.getServerType())),
        LANGUAGE(plugin -> Component.text(plugin.getSettings().getLanguage())),
        MINECRAFT_VERSION(plugin -> Component.text(plugin.getMinecraftVersion().toString())),
        JAVA_VERSION(plugin -> Component.text(System.getProperty("java.version"))),
        JAVA_VENDOR(plugin -> Component.text(System.getProperty("java.vendor"))),
        SERVER_NAME(plugin -> Component.text(plugin.getServerName())),
        DATABASE_TYPE(plugin -> Component.text(plugin.getSettings().getDatabase().getType().getDisplayName())),
        IS_DATABASE_LOCAL(plugin -> getLocalhostBoolean(plugin.getSettings().getDatabase().getCredentials().getHost())),
        USING_REDIS_SENTINEL(plugin -> getBoolean(!plugin.getSettings().getCrossServer().getRedis().getSentinel()
                .getMasterName().isBlank())),
        USING_REDIS_PASSWORD(plugin -> getBoolean(!plugin.getSettings().getCrossServer().getRedis().getPassword()
                .isBlank())),
        REDIS_USING_SSL(plugin -> getBoolean(!plugin.getSettings().getCrossServer().getRedis().isUseSsl())),
        IS_REDIS_LOCAL(plugin -> getLocalhostBoolean(plugin.getSettings().getCrossServer().getRedis().getHost())),
        REGISTERED_TRUST_TAGS(plugin -> Component.join(
                JoinConfiguration.commas(true),
                plugin.getTrustTags().stream().map(tag -> Component.text(tag.getName())).toList()
        )),
        LOADED_HOOKS(plugin -> Component.join(
                JoinConfiguration.commas(true),
                plugin.getHooks().stream().filter(hook -> !(hook instanceof Importer))
                        .map(hook -> Component.text(hook.getName())).toList()
        )),
        LOADED_IMPORTERS(plugin -> Component.join(
                JoinConfiguration.commas(true),
                plugin.getImporters().stream().map(hook -> Component.text(hook.getName())).toList()
        ));

        private final Function<HuskClaims, Component> supplier;

        StatusLine(@NotNull Function<HuskClaims, Component> supplier) {
            this.supplier = supplier;
        }

        @NotNull
        private Component get(@NotNull HuskClaims plugin) {
            return Component
                    .text("•").appendSpace()
                    .append(Component.text(
                            WordUtils.capitalizeFully(name().replaceAll("_", " ")),
                            TextColor.color(0x848484)
                    ))
                    .append(Component.text(':')).append(Component.space().color(NamedTextColor.WHITE))
                    .append(supplier.apply(plugin));
        }

        @NotNull
        private static Component getBoolean(boolean value) {
            return Component.text(value ? "Yes" : "No", value ? NamedTextColor.GREEN : NamedTextColor.RED);
        }

        @NotNull
        private static Component getLocalhostBoolean(@NotNull String value) {
            return getBoolean(value.equals("127.0.0.1") || value.equals("0.0.0.0")
                              || value.equals("localhost") || value.equals("::1"));
        }
    }

}

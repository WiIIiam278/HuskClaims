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

package net.william278.huskclaims.util;

import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.hook.Hook;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.toilet.DumpOptions;
import net.william278.toilet.Toilet;
import net.william278.toilet.dump.DumpUser;
import net.william278.toilet.dump.PluginStatus;
import net.william278.toilet.dump.ProjectMeta;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import static net.william278.toilet.DumpOptions.*;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface DumpProvider {

    @NotNull String BYTEBIN_URL = "https://bytebin.lucko.me";
    @NotNull String VIEWER_URL = "https://william278.net/dump";

    @NotNull
    Toilet getToilet();

    @NotNull
    @Blocking
    default String createDump(@NotNull CommandUser u) {
        return getToilet().dump(getPluginStatus(), u instanceof OnlineUser o
                ? new DumpUser(o.getName(), o.getUuid()) : null).toString();
    }

    @NotNull
    default DumpOptions getDumpOptions() {
        return builder()
                .bytebinUrl(BYTEBIN_URL)
                .viewerUrl(VIEWER_URL)
                .projectMeta(ProjectMeta.builder()
                        .id("huskclaims")
                        .name("HuskClaims")
                        .version(getPlugin().getPluginVersion().toString())
                        .md5("unknown")
                        .author("William278")
                        .sourceCode("https://github.com/WiIIiam278/HuskClaims")
                        .website("https://william278.net/project/huskclaims")
                        .support("https://discord.gg/tVYhJfyDWG")
                        .build())
                .fileInclusionRules(List.of(
                        FileInclusionRule.configFile("config.yml", "Config File"),
                        FileInclusionRule.configFile("trust_levels.yml", "Trust Levels"),
                        FileInclusionRule.configFile(getMessagesFile(), "Locales File")
                ))
                .build();
    }

    @NotNull
    @Blocking
    private PluginStatus getPluginStatus() {
        return PluginStatus.builder()
                .blocks(List.of(getSystemStatus(), getClaimStatus(), getHookStatus(),
                        getHighlightersStatus(), getOperationTypeStatus()))
                .build();
    }

    @NotNull
    @Blocking
    private PluginStatus.MapStatusBlock getSystemStatus() {
        return new PluginStatus.MapStatusBlock(
                Map.of(
                        "Language", StatusLine.LANGUAGE.getValue(getPlugin()),
                        "Database Type", StatusLine.DATABASE_TYPE.getValue(getPlugin()),
                        "Database Local", StatusLine.IS_DATABASE_LOCAL.getValue(getPlugin()),
                        "Cross Server", StatusLine.IS_CROSS_SERVER.getValue(getPlugin()),
                        "Server Name", StatusLine.SERVER_NAME.getValue(getPlugin()),
                        "Message Broker", StatusLine.MESSAGE_BROKER_TYPE.getValue(getPlugin()),
                        "Redis Sentinel", StatusLine.USING_REDIS_SENTINEL.getValue(getPlugin()),
                        "Redis Password", StatusLine.USING_REDIS_PASSWORD.getValue(getPlugin()),
                        "Redis SSL", StatusLine.REDIS_USING_SSL.getValue(getPlugin()),
                        "Redis Local", StatusLine.IS_REDIS_LOCAL.getValue(getPlugin())
                ),
                "Plugin Status", "fa6-solid:wrench"
        );
    }

    @NotNull
    @Blocking
    private PluginStatus.ChartStatusBlock getClaimStatus() {
        return new PluginStatus.ChartStatusBlock(
                getPlugin().getClaimWorlds().entrySet().stream()
                        .map((e) -> Map.entry(
                                new PluginStatus.ChartKey(e.getKey(), "", getColorFor(e.getKey())),
                                e.getValue().getClaimCount()
                        ))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                PluginStatus.ChartType.PIE, "Claims by Worlds", "mdi:shovel"
        );
    }

    @NotNull
    @Blocking
    private PluginStatus.ListStatusBlock getHookStatus() {
        return new PluginStatus.ListStatusBlock(
                getPlugin().getHooks().stream().map(Hook::getName).toList(),
                "Loaded Hooks", "fa6-solid:plug"
        );
    }

    @NotNull
    @Blocking
    private PluginStatus.ListStatusBlock getHighlightersStatus() {
        return new PluginStatus.ListStatusBlock(
                getPlugin().getHighlighters().stream()
                        .map(h -> "%s (%s)".formatted(h.getClass().getSimpleName(), h.getPriority()))
                        .toList(),
                "Highlighters", "mdi:star-cog"
        );
    }

    @NotNull
    @Blocking
    private PluginStatus.ListStatusBlock getOperationTypeStatus() {
        return new PluginStatus.ListStatusBlock(
                getPlugin().getOperationListener().getRegisteredOperationTypes().stream()
                        .map(OperationType::asMinimalString).toList(),
                "Operation Types", "ci:flag"
        );
    }

    @NotNull
    private String getMessagesFile() {
        return "messages-%s.yml".formatted(getPlugin().getSettings().getLanguage());
    }

    private int getColorFor(@NotNull String seed) {
        int hash = seed.hashCode();
        return new Color((hash >> 16) & 0xFF, (hash >> 8) & 0xFF, hash & 0xFF).getRGB();
    }

    @NotNull
    HuskClaims getPlugin();

}

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

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.toilet.DumpOptions;
import net.william278.toilet.Toilet;
import net.william278.toilet.dump.DumpUser;
import net.william278.toilet.dump.PluginStatus;
import net.william278.toilet.dump.ProjectMeta;
import org.jetbrains.annotations.NotNull;

import static net.william278.toilet.DumpOptions.*;

import java.util.List;

public interface ToiletProvider {

    @NotNull String BYTEBIN_URL = "https://bytebin.lucko.me";
    @NotNull String VIEWER_URL = "https://william278.net/dump";

    @NotNull
    Toilet getToilet();

    @NotNull
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
    private String getMessagesFile() {
        return "messages-%s.yml".formatted(getPlugin().getSettings().getLanguage());
    }

    @NotNull
    private PluginStatus getPluginStatus() {
        return new PluginStatus(StatusLine.toMap(getPlugin()));
    }

    @NotNull
    HuskClaims getPlugin();

}

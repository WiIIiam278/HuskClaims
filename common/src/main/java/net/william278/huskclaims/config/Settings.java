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

package net.william278.huskclaims.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.*;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Configuration
public final class Settings {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      HuskClaims - Config     ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/huskclaims/
            ┣╸ Config Help: https://william278.net/docs/huskclaims/config-files/
            ┗╸ Documentation: https://william278.net/docs/huskclaims/""";

    @Comment("Locale of the default language file to use. Docs: https://william278.net/docs/huskclaims/translations")
    private String language = Locales.DEFAULT_LOCALE;

    @Comment("Whether to automatically check for plugin updates on startup")
    private boolean checkForUpdates = true;

    @Comment("Database settings")
    private DatabaseSettings database = new DatabaseSettings();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DatabaseSettings {

        @Comment("Type of database to use (SQLITE, MYSQL or MARIADB)")
        private Database.Type type = Database.Type.SQLITE;

        @Comment("Specify credentials here if you are using MYSQL or MARIADB")
        private Credentials credentials = new Credentials();

        @Comment({"MYSQL / MARIADB database Hikari connection pool properties",
                "Don't modify this unless you know what you're doing!"})
        PoolOptions poolOptions;

        @Comment("Names of tables to use on your database. Don't modify this unless you know what you're doing!")
        Map<Database.Table, String> tableNames = Map.of(
                Database.Table.META_DATA, Database.Table.META_DATA.getDefaultName(),
                Database.Table.USER_DATA, Database.Table.USER_DATA.getDefaultName(),
                Database.Table.USER_GROUP_DATA, Database.Table.USER_GROUP_DATA.getDefaultName(),
                Database.Table.CLAIM_DATA, Database.Table.CLAIM_DATA.getDefaultName()
        );

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Credentials {
            private String host = "localhost";
            int port = 3306;
            String database = "huskclaims";
            String username = "root";
            String password = "pa55w0rd";
            String parameters = "?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8";
        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class PoolOptions {
            private int size = 12;
            private long idle = 12;
            private long lifetime = 1800000;
            private long keepAlive = 30000;
            private long timeout = 20000;
        }

        @NotNull
        public String getTableName(@NotNull Database.Table tableName) {
            return Optional.ofNullable(tableNames.get(tableName)).orElse(tableName.getDefaultName());
        }
    }

    @Comment("Cross-server settings")
    private CrossServerSettings crossServer = new CrossServerSettings();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CrossServerSettings {
        @Comment("Whether to enable cross-server mode")
        private boolean enabled = false;
    }


    @Comment("Claim flags & world settings")
    private ClaimSettings claims = new ClaimSettings();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ClaimSettings {
        @Comment("Default flags for regular claims")
        private List<OperationType> defaultFlags = List.of(
                OperationType.PLAYER_DAMAGE_MONSTER,
                OperationType.EXPLOSION_DAMAGE_ENTITY,
                OperationType.PLAYER_DAMAGE_PLAYER,
                OperationType.MONSTER_SPAWN
        );

        @Comment("Default flags for admin claims")
        private List<OperationType> adminFlags = List.of(
                OperationType.PLAYER_DAMAGE_MONSTER,
                OperationType.EXPLOSION_DAMAGE_ENTITY,
                OperationType.PLAYER_DAMAGE_PLAYER
        );

        @Comment("List of worlds where users cannot claim")
        private List<String> unclaimableWorlds = List.of();

        @Comment("Claim inspection tool (right click with this to inspect claims)")
        private String inspectionTool = "minecraft:stick";

        @Comment("Claim creation & resize tool (right click with this to create/resize claims)")
        private String claimTool = "minecraft:golden_shovel";

        @Comment("Max range of inspector tools")
        private int inspectionDistance = 40;

        @Comment("Claim inspection highlighting settings")
        private HighlighterSettings highlighting = new HighlighterSettings();

        public boolean isWorldUnclaimable(@NotNull World world) {
            return unclaimableWorlds.stream().anyMatch(world.getName()::equalsIgnoreCase);
        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class HighlighterSettings {
            @Comment("Highlight block for regular claim corners")
            private String regularClaimCornerBlock = "minecraft:glowstone";

            @Comment("Highlight block for regular claims")
            private String regularClaimBlock = "minecraft:gold_block";

            @Comment("Highlight block for child/sub claim corners")
            private String childClaimCornerBlock = "minecraft:sea_lantern";

            @Comment("Highlight block for child/sub claims")
            private String childClaimBlock = "minecraft:iron_block";

            @Comment("Highlight block for admin claim corners")
            private String adminClaimCornerBlock = "minecraft:jack_o_lantern";

            @Comment("Highlight block for admin claims")
            private String adminClaimBlock = "minecraft:pumpkin";

            @Comment("Highlight block for other players' claim corners")
            private String otherClaimCornerBlock = "minecraft:netherrack";

            @Comment("Highlight block for other players' claims")
            private String otherClaimBlock = "minecraft:netherrack";
        }

    }

    @Comment("Groups of operations that can be toggled on/off in claims")
    public List<OperationGroup> operationGroups = List.of(
            OperationGroup.builder()
                    .name("Claim Explosions")
                    .allowedOperations(List.of(
                            OperationType.EXPLOSION_DAMAGE_TERRAIN,
                            OperationType.MONSTER_DAMAGE_TERRAIN
                    ))
                    .toggleCommandAliases(List.of("claimexplosions"))
                    .build()
    );

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OperationGroup {
        private String name;
        private List<String> toggleCommandAliases;
        private List<OperationType> allowedOperations;
    }

    public static class UserGroupSettings {
        private boolean enabled;
        private int maxMembersPerGroup;
        private int maxGroupsPerPlayer;
    }

}

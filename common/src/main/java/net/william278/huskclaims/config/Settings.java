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

import com.google.common.collect.Lists;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.william278.cloplib.listener.InspectorCallbackProvider;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.highlighter.Highlightable;
import net.william278.huskclaims.network.Broker;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.util.BlockProvider;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Settings {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      HuskClaims - Config     ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/huskclaims/
            ┣╸ Config Help: https://william278.net/docs/huskclaims/config/
            ┗╸ Documentation: https://william278.net/docs/huskclaims/""";

    @Comment("Locale of the default language file to use. Docs: https://william278.net/docs/huskclaims/translations")
    private String language = Locales.DEFAULT_LOCALE;

    @Comment("Whether to automatically check for plugin updates on startup")
    private boolean checkForUpdates = true;

    @Comment("Database settings")
    private DatabaseSettings database = new DatabaseSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DatabaseSettings {

        @Comment("Type of database to use (SQLITE, MYSQL or MARIADB)")
        private Database.Type type = Database.Type.SQLITE;

        @Comment("Specify credentials here if you are using MYSQL or MARIADB")
        private DatabaseCredentials credentials = new DatabaseCredentials();

        @Comment({"MYSQL / MARIADB database Hikari connection pool properties",
                "Don't modify this unless you know what you're doing!"})
        private PoolOptions poolOptions = new PoolOptions();

        @Comment("Names of tables to use on your database. Don't modify this unless you know what you're doing!")
        private Map<Database.Table, String> tableNames = new TreeMap<>(Map.of(
                Database.Table.META_DATA, Database.Table.META_DATA.getDefaultName(),
                Database.Table.USER_DATA, Database.Table.USER_DATA.getDefaultName(),
                Database.Table.USER_GROUP_DATA, Database.Table.USER_GROUP_DATA.getDefaultName(),
                Database.Table.CLAIM_DATA, Database.Table.CLAIM_DATA.getDefaultName()
        ));

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class DatabaseCredentials {
            private String host = "localhost";
            int port = 3306;
            String database = "huskclaims";
            String username = "root";
            String password = "pa55w0rd";
            String parameters = "?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
        }

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class PoolOptions {
            private int size = 12;
            private int idle = 12;
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
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CrossServerSettings {
        @Comment("Whether to enable cross-server mode")
        private boolean enabled = false;

        @Comment({"The cluster ID, for if you're networking multiple separate groups of HuskClaims-enabled servers.",
                "Do not change unless you know what you're doing"})
        private String clusterId = "main";

        @Comment("Type of network message broker to ues for data synchronization (PLUGIN_MESSAGE or REDIS)")
        private Broker.Type brokerType = Broker.Type.PLUGIN_MESSAGE;

        @Comment("Settings for if you're using REDIS as your message broker")
        private RedisSettings redis = new RedisSettings();

        @Getter
        @Configuration
        @NoArgsConstructor
        public static class RedisSettings {
            private String host = "localhost";
            private int port = 6379;
            @Comment("Password for your Redis server. Leave blank if you're not using a password.")
            private String password = "";
            private boolean useSsl = false;

            @Comment({"Settings for if you're using Redis Sentinels.",
                    "If you're not sure what this is, please ignore this section."})
            private SentinelSettings sentinel = new SentinelSettings();

            @Getter
            @Configuration
            @NoArgsConstructor
            public static class SentinelSettings {
                private String masterName = "";
                @Comment("List of host:port pairs")
                private List<String> nodes = Lists.newArrayList();
                private String password = "";
            }
        }
    }


    @Comment("Claim flags & world settings")
    private ClaimSettings claims = new ClaimSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ClaimSettings {
        @Comment("Default flags for new regular claims")
        private List<OperationType> defaultFlags = List.of(
                OperationType.PLAYER_DAMAGE_MONSTER,
                OperationType.EXPLOSION_DAMAGE_ENTITY,
                OperationType.PLAYER_DAMAGE_PLAYER,
                OperationType.MONSTER_SPAWN,
                OperationType.PASSIVE_MOB_SPAWN
        );

        @Comment("Default flags for new admin claims")
        private List<OperationType> adminFlags = List.of(
                OperationType.PLAYER_DAMAGE_MONSTER,
                OperationType.EXPLOSION_DAMAGE_ENTITY,
                OperationType.PLAYER_DAMAGE_PLAYER,
                OperationType.MONSTER_SPAWN,
                OperationType.PASSIVE_MOB_SPAWN
        );

        @Comment("List of enabled claim types. Must include at least the regular \"CLAIMS\" mode")
        private List<ClaimingMode> enabledClaimingModes = List.of(
                ClaimingMode.values() // Allow all claiming modes
        );

        @Comment({"Default flags for the wilderness (outside claims)",
                "To modify existing worlds, use /claimflags set <flag> <true/false> while standing outside a claim."})
        private List<OperationType> wildernessRules = List.of(
                OperationType.values() // Allow all operation types
        );

        @Comment("List of worlds where users cannot claim")
        private List<String> unclaimableWorlds = List.of();

        @Comment("The number of claim blocks a user gets when they first join the server")
        private long startingClaimBlocks = 100;

        @Comment({"The number of claim blocks a user gets hourly.",
                "Override with the \"huskclaims.hourly_blocks.(amount)\" permission"})
        private long hourlyClaimBlocks = 100;

        @Comment("Claim inspection tool (right click with this to inspect claims)")
        private String inspectionTool = "minecraft:stick";

        @Comment("Model data settings for the inspection tool")
        private ModelDataSettings inspectionToolModelData = new ModelDataSettings();

        @Comment("Claim creation & resize tool (right click with this to create/resize claims)")
        private String claimTool = "minecraft:golden_shovel";

        @Comment("Model data settings for the claim tool")
        private ModelDataSettings claimToolModelData = new ModelDataSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class ModelDataSettings {
            private boolean required = false;
            private int modelData = 0;
        }

        @Comment("Require players to hold the claim tool to use claim commands (e.g. /claim <radius>, /extendclaim)")
        private boolean requireToolForCommands = true;

        @Comment("Minimum size of claims. This does not affect child or admin claims.")
        private int minimumClaimSize = 5;

        @Comment("Max range of inspector tools")
        private int inspectionDistance = 64;

        @Comment("Whether to allow inspecting nearby claims by sneaking when using the inspection tool")
        private boolean allowNearbyClaimInspection = true;

        @Comment("Whether to require confirmation when deleting claims that have children")
        private boolean confirmDeletingParentClaims = true;

        @Comment("Whether to send a message when a player enters a claim")
        private boolean sendEntryMessage = false;

        @Comment("Whether to send a message when a player exits a claim")
        private boolean sendExitMessage = false;

        @Comment("Settings for automatically removing claims made by now-inactive users")
        private InactivityPruningSettings inactivityPruning = new InactivityPruningSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class InactivityPruningSettings {

            @Comment("Whether to delete all claims made by users marked as inactive. (Warning: Dangerous!)")
            private boolean enabled = false;

            @Comment("The number of days a user must not log on for to be marked as inactive (Minimum: 1)")
            private long inactiveDays = 60;

            @Comment("List of worlds to exclude from being pruned.")
            private List<String> excludedWorlds = Lists.newArrayList();

            @Comment("List of users (by either UUID or username) to exclude from inactive claim pruning")
            private List<String> excludedUsers = Lists.newArrayList();

        }

        @Comment("Settings for banning users from claims")
        private BanSettings bans = new BanSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class BanSettings {

            @Comment({"Whether to let users ban others from their claims (prevent them from entering) using /claimban",
                    "Also requires the MANAGE_BANS privilege (by default, restricted to those with 'manage' trust)"})
            private boolean enabled = false;

            @Comment({"Whether to let users set their claim claims to private (prevent others from entering) using /claimprivate",
                    "Also requires the MANAGE_BANS privilege (by default, restricted to those with 'manage' trust)"})
            private boolean privateClaims = false;

        }

        public boolean isWorldUnclaimable(@NotNull World world) {
            return unclaimableWorlds.stream().anyMatch(world.getName()::equalsIgnoreCase);
        }

        @NotNull
        public InspectorCallbackProvider.InspectionTool getInspectionToolData() {
            return new InspectorCallbackProvider.InspectionTool(
                    inspectionTool, inspectionToolModelData.isRequired(), inspectionToolModelData.getModelData()
            );
        }

        @NotNull
        public InspectorCallbackProvider.InspectionTool getClaimToolData() {
            return new InspectorCallbackProvider.InspectionTool(
                    claimTool, claimToolModelData.isRequired(), claimToolModelData.getModelData()
            );
        }

    }

    @Comment("Groups of operations that can be toggled on/off in claims")
    public List<OperationGroup> operationGroups = List.of(
            OperationGroup.builder()
                    .name("Claim Explosions")
                    .description("Toggle whether explosions can damage terrain in claims")
                    .allowedOperations(Set.of(
                            OperationType.EXPLOSION_DAMAGE_TERRAIN,
                            OperationType.MONSTER_DAMAGE_TERRAIN
                    ))
                    .toggleCommandAliases(List.of("claimexplosions"))
                    .build()
    );

    @Getter
    @Builder
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OperationGroup {
        private String name;
        private String description;
        private List<String> toggleCommandAliases;
        private Set<OperationType> allowedOperations;
    }

    @Comment("Settings for user groups, letting users quickly manage trust for groups of multiple players at once")
    private UserGroupSettings userGroups = new UserGroupSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UserGroupSettings {
        @Comment("Whether to enable user groups")
        private boolean enabled = true;

        @Comment("The prefix to use when specifying a group in a trust command (e.g. /trust @groupname)")
        private String groupSpecifierPrefix = "@";

        @Comment("Whether to restrict group names with a regex filter")
        private boolean restrictGroupNames = true;

        @Comment("Regex for group names")
        private String groupNameRegex = "[a-zA-Z0-9-_]*";

        @Comment("Max members per group")
        private int maxMembersPerGroup = 10;

        @Comment("Max groups per player")
        private int maxGroupsPerPlayer = 3;
    }

    @Comment("Settings for trust tags, special representations of things you can trust in a claim")
    private TrustTagSettings trustTags = new TrustTagSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TrustTagSettings {
        @Comment("Whether to enable trust tags")
        private boolean enabled = true;

        @Comment("The prefix to use when specifying a trust tag in a trust command (e.g. /trust #tagname)")
        private String tagSpecifierPrefix = "#";

        @Comment("The name of the default public access tag (to let anyone access certain claim levels)")
        private String publicAccessName = "public";

    }

    @Comment("Settings for the claim inspection/creation highlighter")
    private HighlighterSettings highlighter = new HighlighterSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HighlighterSettings {
        @Comment("Whether to use block display entities for glowing (requires Paper 1.19.4+)")
        private boolean blockDisplays = true;

        @Comment("If using block displays, whether highlights should use a glow effect (requires Paper 1.19.4+)")
        private boolean glowEffect = true;

        @Comment("The blocks to use when highlighting claims")
        private Map<Highlightable.Type, String> blockTypes = new TreeMap<>(Map.of(
                Highlightable.Type.EDGE, "minecraft:gold_block",
                Highlightable.Type.CORNER, "minecraft:glowstone",
                Highlightable.Type.ADMIN_CORNER, "minecraft:glowstone",
                Highlightable.Type.ADMIN_EDGE, "minecraft:pumpkin",
                Highlightable.Type.CHILD_CORNER, "minecraft:iron_block",
                Highlightable.Type.CHILD_EDGE, "minecraft:white_wool",
                Highlightable.Type.OVERLAP_CORNER, "minecraft:red_nether_bricks",
                Highlightable.Type.OVERLAP_EDGE, "minecraft:netherrack",
                Highlightable.Type.SELECTION, "minecraft:diamond_block"
        ));

        @NotNull
        public BlockProvider.MaterialBlock getBlock(@NotNull Highlightable.Type type,
                                                    @NotNull HuskClaims plugin) {
            return Optional.ofNullable(blockTypes.get(type))
                    .map(plugin::getBlockFor)
                    .orElse(plugin.getBlockFor("minecraft:yellow_concrete"));
        }

        @Comment("The color of the glow effect used for blocks when highlighting claims")
        private Map<Highlightable.Type, GlowColor> glowColors = new TreeMap<>(Map.of(
                Highlightable.Type.EDGE, GlowColor.YELLOW,
                Highlightable.Type.CORNER, GlowColor.YELLOW,
                Highlightable.Type.ADMIN_CORNER, GlowColor.ORANGE,
                Highlightable.Type.ADMIN_EDGE, GlowColor.ORANGE,
                Highlightable.Type.CHILD_CORNER, GlowColor.SILVER,
                Highlightable.Type.CHILD_EDGE, GlowColor.SILVER,
                Highlightable.Type.OVERLAP_CORNER, GlowColor.RED,
                Highlightable.Type.OVERLAP_EDGE, GlowColor.RED,
                Highlightable.Type.SELECTION, GlowColor.AQUA
        ));

        @NotNull
        public Settings.HighlighterSettings.GlowColor getGlowColor(@NotNull Highlightable.Type type) {
            return Optional.ofNullable(glowColors.get(type)).orElse(GlowColor.WHITE);
        }

        @Getter
        public enum GlowColor {

            WHITE(16777215),
            SILVER(12632256),
            GRAY(8421504),
            BLACK(0),
            RED(16711680),
            MAROON(8388608),
            YELLOW(16776960),
            OLIVE(8421376),
            LIME(65280),
            GREEN(32768),
            AQUA(65535),
            TEAL(32896),
            BLUE(255),
            NAVY(128),
            FUCHSIA(16711935),
            PURPLE(8388736),
            ORANGE(16753920);

            private final int rgb;

            GlowColor(int rgb) {
                this.rgb = rgb;
            }
        }
    }

    @Comment("Settings for protecting tamed animals (pets). Docs: https://william278.net/docs/huskclaims/pets")
    private PetSettings pets = new PetSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PetSettings {

        @Comment("Whether to enable protecting tamed animals to only be harmed/used by their owner")
        private boolean enabled = true;

    }

    @Comment("Settings for moderation tools")
    private ModerationSettings moderation = new ModerationSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ModerationSettings {

        private SignSettings signs = new SignSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class SignSettings {
            @Comment("Whether to notify users with /signspy on when signs are placed.edited. Requires Minecraft 1.19.4+"
                     + "Requires Minecraft 1.19.4+")
            private boolean notifyModerators = true;

            @Comment("Whether to filter messages")
            private boolean filterMessages = false;

            @Comment("Whether sign notifications should be limited to just filtered signs")
            private boolean onlyNotifyIfFiltered = false;

            @Comment("Single character to replace filtered message content with")
            private char replacementCharacter = '#';

            @Comment("List of words to filter out of signs")
            private List<String> filteredWords = List.of();
        }

        private DropSettings drops = new DropSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class DropSettings {
            @Comment("Whether to lock ground items dropped by players when they die from being picked up by others")
            private boolean lockItems = true;

            @Comment("Whether to also prevent death drops from being destroyed by lava, fire, cacti, etc.")
            private boolean preventDestruction = true;
        }

    }

    @Comment("Settings for integration hooks with other plugins")
    private HookSettings hooks = new HookSettings();

    @Getter
    @Configuration
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class HookSettings {

        private LuckPermsHookSettings luckPerms = new LuckPermsHookSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class LuckPermsHookSettings {
            @Comment("Whether to hook into LuckPerms for permission group trust tags")
            private boolean enabled = true;

            @Comment("Require users to have the \"huskclaims.trust.luckperms\" permission to use LuckPerms trust tags")
            private boolean trustTagUsePermission = true;

            @Comment("The prefix to use when specifying a LuckPerms group trust tag (e.g. /trust #role/groupname)")
            private String trustTagPrefix = "role/";
        }

        private PlanHookSettings plan = new PlanHookSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class PlanHookSettings {
            @Comment("Whether to hook into Plan to display claim analytics")
            private boolean enabled = true;
        }

        private HuskHomesHookSettings huskHomes = new HuskHomesHookSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class HuskHomesHookSettings {
            @Comment("Whether to hook into HuskHomes for claim teleportation and home restriction")
            private boolean enabled = true;

            @Comment("Whether to restrict setting a home in claims to a trust level")
            private boolean restrictSetHome = true;

            @Comment("The trust level required to set a home in a claim (the ID of a level in 'trust_levels.yml')")
            private String setHomeTrustLevel = "access";
        }

        private HuskTownsHookSettings huskTowns = new HuskTownsHookSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class HuskTownsHookSettings {
            @Comment("Whether to hook into HuskTowns to disable claiming over town claims")
            private boolean enabled = true;
        }


        private EconomyHookSettings economy = new EconomyHookSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class EconomyHookSettings {
            @Comment("Whether to hook into an economy plugin to allow buying claim blocks")
            private boolean enabled = true;

            @Comment("The cost of buying 1 claim block")
            private double costPerBlock = 1.0;
        }

        private PlaceholderSettings placeholders = new PlaceholderSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class PlaceholderSettings {
            @Comment("Whether to hook into PlaceholderAPI to provide a HuskClaims placeholder expansion")
            private boolean enabled = true;
        }

        private WorldGuardSettings worldGuard = new WorldGuardSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class WorldGuardSettings {
            @Comment("Whether to hook into WorldGuard to provide a flag to deny claiming in WorldGuard regions")
            private boolean enabled = true;
        }

        private MapHookSettings map = new MapHookSettings();

        @Getter
        @Configuration
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class MapHookSettings {
            @Comment("Whether to hook into Dynmap, BlueMap, or Pl3xMap to show claims on the map")
            private boolean enabled = true;

            @Comment("What colors to use for types of claims on the map. Remove a pairing to hide claims of that type.")
            private Map<ClaimingMode, String> colors = new TreeMap<>(Map.of(
                    ClaimingMode.CLAIMS, NamedTextColor.YELLOW.asHexString(),
                    ClaimingMode.ADMIN_CLAIMS, NamedTextColor.DARK_RED.asHexString(),
                    ClaimingMode.CHILD_CLAIMS, NamedTextColor.WHITE.asHexString()
            ));

            @Comment("The name of the marker set key")
            private String markerSetName = "Claims";

            @Comment("The label format for markers. '%s' will be replaced with the claim owner's name")
            private String labelFormat = "Claim by %s";
        }

    }
}

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.trust.TrustLevel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TrustLevels {

    protected static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃   HuskClaims - Trust Levels  ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ List of trust levels users & groups can be assigned to in claims
            ┣╸ Config Help: https://william278.net/docs/huskclaims/trust/
            ┗╸ Documentation: https://william278.net/docs/huskclaims/
            """;

    private List<TrustLevel> trustLevels = Lists.newArrayList(
            // Permission trust (manage trustees, make sub-divisions, etc.)
            TrustLevel.builder()
                    .id("manage")
                    .weight(400)
                    .color("#fc4e03")
                    .displayName("Manage")
                    .description("Allows users to manage trustees & make child claims")
                    .commandAliases(List.of("permissiontrust", "managetrust"))
                    .flags(List.of(
                            OperationType.BLOCK_BREAK,
                            OperationType.BLOCK_PLACE,
                            OperationType.BLOCK_INTERACT,
                            OperationType.REDSTONE_INTERACT,
                            OperationType.ENTITY_INTERACT,
                            OperationType.CONTAINER_OPEN,
                            OperationType.FARM_BLOCK_BREAK,
                            OperationType.FARM_BLOCK_PLACE,
                            OperationType.FARM_BLOCK_INTERACT,
                            OperationType.PLACE_HANGING_ENTITY,
                            OperationType.BREAK_HANGING_ENTITY,
                            OperationType.PLAYER_DAMAGE_PLAYER,
                            OperationType.PLAYER_DAMAGE_PERSISTENT_ENTITY,
                            OperationType.PLAYER_DAMAGE_MONSTER,
                            OperationType.PLAYER_DAMAGE_ENTITY,
                            OperationType.FILL_BUCKET,
                            OperationType.EMPTY_BUCKET,
                            OperationType.USE_SPAWN_EGG,
                            OperationType.ENDER_PEARL_TELEPORT
                    ))
                    .privileges(List.of(
                            TrustLevel.Privilege.MANAGE_TRUSTEES,
                            TrustLevel.Privilege.MANAGE_CHILD_CLAIMS,
                            TrustLevel.Privilege.MANAGE_OPERATION_GROUPS,
                            TrustLevel.Privilege.MANAGE_BANS,
                            TrustLevel.Privilege.MAKE_PRIVATE
                    ))
                    .build(),

            // Regular Build trust (place & break blocks, etc.)
            TrustLevel.builder()
                    .id("build")
                    .weight(300)
                    .color("#fcd303")
                    .displayName("Build")
                    .description("Allows users to build in the claim")
                    .commandAliases(List.of("trust", "buildtrust"))
                    .flags(List.of(
                            OperationType.BLOCK_BREAK,
                            OperationType.BLOCK_PLACE,
                            OperationType.BLOCK_INTERACT,
                            OperationType.REDSTONE_INTERACT,
                            OperationType.ENTITY_INTERACT,
                            OperationType.CONTAINER_OPEN,
                            OperationType.FARM_BLOCK_BREAK,
                            OperationType.FARM_BLOCK_PLACE,
                            OperationType.FARM_BLOCK_INTERACT,
                            OperationType.PLACE_HANGING_ENTITY,
                            OperationType.BREAK_HANGING_ENTITY,
                            OperationType.PLAYER_DAMAGE_PLAYER,
                            OperationType.PLAYER_DAMAGE_PERSISTENT_ENTITY,
                            OperationType.PLAYER_DAMAGE_MONSTER,
                            OperationType.PLAYER_DAMAGE_ENTITY,
                            OperationType.FILL_BUCKET,
                            OperationType.EMPTY_BUCKET,
                            OperationType.USE_SPAWN_EGG,
                            OperationType.ENDER_PEARL_TELEPORT
                    ))
                    .build(),

            // Container trust (chests, furnaces, etc.)
            TrustLevel.builder()
                    .id("container")
                    .weight(200)
                    .color("#5efc03")
                    .displayName("Container")
                    .description("Allows users to open chests & other containers")
                    .commandAliases(List.of("containertrust"))
                    .flags(List.of(
                            OperationType.BLOCK_INTERACT,
                            OperationType.ENTITY_INTERACT,
                            OperationType.CONTAINER_OPEN,
                            OperationType.REDSTONE_INTERACT,
                            OperationType.ENDER_PEARL_TELEPORT
                    ))
                    .build(),

            // Access trust (doors, buttons, levers, etc.)
            TrustLevel.builder()
                    .id("access")
                    .weight(100)
                    .color("#36e4ff")
                    .displayName("Access")
                    .description("Allows users to use doors, buttons, levers, etc.")
                    .commandAliases(List.of("accesstrust"))
                    .flags(List.of(
                            OperationType.BLOCK_INTERACT,
                            OperationType.ENTITY_INTERACT,
                            OperationType.REDSTONE_INTERACT,
                            OperationType.ENDER_PEARL_TELEPORT
                    ))
                    .build()
    );

    @Comment("The operation types that the claim owner can perform in their own claim")
    private Set<OperationType> allowedOwnerOperations = Set.of(
            OperationType.BLOCK_BREAK,
            OperationType.BLOCK_PLACE,
            OperationType.BLOCK_INTERACT,
            OperationType.REDSTONE_INTERACT,
            OperationType.ENTITY_INTERACT,
            OperationType.CONTAINER_OPEN,
            OperationType.FARM_BLOCK_BREAK,
            OperationType.FARM_BLOCK_PLACE,
            OperationType.FARM_BLOCK_INTERACT,
            OperationType.PLACE_HANGING_ENTITY,
            OperationType.BREAK_HANGING_ENTITY,
            OperationType.PLAYER_DAMAGE_PLAYER,
            OperationType.PLAYER_DAMAGE_PERSISTENT_ENTITY,
            OperationType.PLAYER_DAMAGE_MONSTER,
            OperationType.PLAYER_DAMAGE_ENTITY,
            OperationType.FILL_BUCKET,
            OperationType.EMPTY_BUCKET,
            OperationType.USE_SPAWN_EGG,
            OperationType.ENDER_PEARL_TELEPORT
    );

    @NotNull
    protected TrustLevels sortByWeight() {
        trustLevels.sort(TrustLevel::compareTo);
        return this;
    }

}

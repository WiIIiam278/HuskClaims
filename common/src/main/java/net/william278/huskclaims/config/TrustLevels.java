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

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.kyori.adventure.key.Key;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.claim.TrustLevel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Configuration
public class TrustLevels {

    protected static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      Trust Levels Config     ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Config Help: https://william278.net/docs/huskclaims/trust-levels/
            ┗╸ Documentation: https://william278.net/docs/huskclaims/
            """;

    protected List<TrustLevel> DEFAULTS = List.of(
            // Permission trust (manage trustees, make sub-divisions, etc.)
            TrustLevel.builder()
                    .key(Key.key("huskclaims", "permission_trust")).weight(400)
                    .displayName("Permission Trust")
                    .commandAliases(List.of("permisiontrust"))
                    .flags(List.of())
                    .privileges(List.of(
                            TrustLevel.Privilege.MANAGE_TRUSTEES,
                            TrustLevel.Privilege.MANAGE_SUBDIVISIONS
                    ))
                    .build(),

            // Regular Build trust (place & break blocks, etc.)
            TrustLevel.builder()
                    .key(Key.key("huskclaims", "build_trust")).weight(300)
                    .displayName("Build Trust")
                    .commandAliases(List.of("trust"))
                    .flags(List.of(
                            OperationType.BLOCK_PLACE,
                            OperationType.BLOCK_BREAK,
                            OperationType.CONTAINER_OPEN,
                            OperationType.BLOCK_INTERACT,
                            OperationType.ENTITY_INTERACT,
                            OperationType.ENDER_PEARL_TELEPORT
                    ))
                    .privileges(List.of(
                            TrustLevel.Privilege.MANAGE_TRUSTEES,
                            TrustLevel.Privilege.MANAGE_SUBDIVISIONS,
                            TrustLevel.Privilege.MANAGE_EXPLOSIONS
                    ))
                    .build(),

            // Container trust (chests, furnaces, etc.)
            TrustLevel.builder()
                    .key(Key.key("huskclaims", "container_trust")).weight(200)
                    .displayName("Container Trust")
                    .commandAliases(List.of("containertrust"))
                    .flags(List.of(
                            OperationType.CONTAINER_OPEN,
                            OperationType.BLOCK_INTERACT,
                            OperationType.ENTITY_INTERACT,
                            OperationType.ENDER_PEARL_TELEPORT
                    ))
                    .build(),

            // Access trust (doors, buttons, levers, etc.)
            TrustLevel.builder()
                    .key(Key.key("huskclaims", "access_trust")).weight(100)
                    .displayName("Access Trust")
                    .commandAliases(List.of("accesstrust"))
                    .flags(List.of(
                            OperationType.BLOCK_INTERACT,
                            OperationType.ENTITY_INTERACT,
                            OperationType.ENDER_PEARL_TELEPORT
                    ))
                    .build()
    );

    private List<TrustLevel> trustLevels = DEFAULTS;

    @NotNull
    protected TrustLevels sortByWeight() {
        trustLevels.sort(TrustLevel::compareTo);
        return this;
    }

}

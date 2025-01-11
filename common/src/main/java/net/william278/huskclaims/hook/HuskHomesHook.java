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

package net.william278.huskclaims.hook;

import net.kyori.adventure.key.Key;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HuskHomesHook extends Hook {

    private static final Key OPERATION_TYPE_KEY = Key.key("huskhomes", "set_home");

    @Nullable
    private OperationType setHomeOperation;

    protected HuskHomesHook(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        if (plugin.getSettings().getHooks().getHuskHomes().isRestrictSetHome()) {
            return;
        }

        // Register operation type
        setHomeOperation = plugin.getOperationListener().createOperationType(OPERATION_TYPE_KEY);
        plugin.getOperationListener().registerOperationType(setHomeOperation);
    }

    protected boolean cancelSetHomeAt(@NotNull OnlineUser user, @NotNull Position position) {
        return setHomeOperation != null && plugin.cancelOperation(Operation.of(user, setHomeOperation, position));
    }

    public abstract void teleport(@NotNull OnlineUser user, @NotNull Position position, @NotNull String server);

}

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

package net.william278.huskclaims.listener;

import net.william278.cloplib.listener.OperationListener;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

public interface ClaimsListener extends OperationListener, ClaimsToolHandler, InspectionToolHandler {

    void register();

    default void setInspectorCallbacks() {
        setInspectorCallback(getPlugin().getSettings().getClaims().getInspectionTool(), this::onInspectionToolUse);
        setInspectorCallback(getPlugin().getSettings().getClaims().getClaimTool(), this::onClaimToolUse);
    }

    @Override
    default int getInspectionDistance() {
        return getPlugin().getSettings().getClaims().getInspectionDistance();
    }

    @NotNull
    HuskClaims getPlugin();

}

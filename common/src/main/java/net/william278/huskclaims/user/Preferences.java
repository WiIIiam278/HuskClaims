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

package net.william278.huskclaims.user;

import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.*;
import net.william278.huskclaims.claim.ClaimingMode;
import org.jetbrains.annotations.NotNull;

import java.util.TreeMap;


@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Preferences implements AuditLogger {

    /**
     * Default preferences for new users
     */
    @NotNull
    public static Preferences DEFAULTS = new Preferences(Action.REGISTERED);

    /**
     * Preferences for users who have imported their data from another plugin
     */
    @NotNull
    public static Preferences IMPORTED = new Preferences(Action.USER_IMPORTED);

    @Setter
    @Expose
    @SerializedName("ignoring_claims")
    private boolean isIgnoringClaims = false;

    @Expose
    @SerializedName("audit_log")
    private TreeMap<String, Entry> logEntries = Maps.newTreeMap();

    @Setter
    @Expose
    @SerializedName("claiming_mode")
    private ClaimingMode claimingMode = ClaimingMode.CLAIMS;

    private Preferences(@NotNull Action openAction) {
        log(openAction);
    }

}

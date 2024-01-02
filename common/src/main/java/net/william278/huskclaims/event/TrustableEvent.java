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

package net.william278.huskclaims.event;

import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.trust.UserGroup;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface TrustableEvent {

    @NotNull
    Trustable getTrusted();

    default Optional<User> getTrustedUser() {
        return getTrusted() instanceof User ? Optional.of((User) getTrusted()) : Optional.empty();
    }

    default Optional<UserGroup> getTrustedGroup() {
        return getTrusted() instanceof UserGroup ? Optional.of((UserGroup) getTrusted()) : Optional.empty();
    }

    default Optional<TrustTag> getTrustedTag() {
        return getTrusted() instanceof TrustTag ? Optional.of((TrustTag) getTrusted()) : Optional.empty();
    }

    @NotNull
    TrustLevel getTrustLevel();

}

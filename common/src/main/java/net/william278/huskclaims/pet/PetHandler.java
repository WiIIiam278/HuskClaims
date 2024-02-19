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

package net.william278.huskclaims.pet;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

public interface PetHandler {

    void userTransferPet(@NotNull OnlineUser user, @NotNull User newOwner, boolean mustBeOwner);

    default boolean cancelPetOperation(@NotNull OnlineUser user, @NotNull User owner) {
        if (user.equals(owner)) {
            return false;
        }
        getPlugin().getLocales().getLocale("pet_transferred", owner.getName())
                .ifPresent(user::sendMessage);
        return true;
    }

    @NotNull
    HuskClaims getPlugin();

}

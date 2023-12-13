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

import net.william278.paginedown.ListOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface PaginatedListProvider {

    @NotNull
    default ListOptions.Builder getBaseList(int itemsPerPage) {
        return new ListOptions.Builder()
                .setFooterFormat(getRawLocale("list_footer",
                        "%previous_page_button%", "%current_page%",
                        "%total_pages%", "%next_page_button%", "%page_jumpers%").orElse(""))
                .setNextButtonFormat(getRawLocale("list_next_page_button",
                        "%next_page_index%", "%command%").orElse(""))
                .setPreviousButtonFormat(getRawLocale("list_previous_page_button",
                        "%previous_page_index%", "%command%").orElse(""))
                .setPageJumpersFormat(getRawLocale("list_page_jumpers",
                        "%page_jump_buttons%").orElse(""))
                .setPageJumperPageFormat(getRawLocale("list_page_jumper_button",
                        "%target_page_index%", "%command%").orElse(""))
                .setPageJumperCurrentPageFormat(getRawLocale("list_page_jumper_current_page",
                        "%current_page%").orElse(""))
                .setPageJumperPageSeparator(getRawLocale("list_page_jumper_separator").orElse(""))
                .setPageJumperGroupSeparator(getRawLocale("list_page_jumper_group_separator").orElse(""))
                .setItemSeparator(getRawLocale("list_item_divider").orElse(" "))
                .setItemsPerPage(itemsPerPage)
                .setEscapeItemsMineDown(false)
                .setSpaceAfterHeader(false)
                .setSpaceBeforeFooter(false);
    }

    Optional<String> getRawLocale(@NotNull String key, @NotNull String... placeholders);

}

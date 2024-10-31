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

package net.william278.huskclaims.highlighter;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Highlighter that uses ghost blocks - used to highlight {@link Highlightable}s to a user in-game
 */
public class BlockUpdateHighlighter extends BlockHighlighter<BlockUpdateHighlighter.UpdateHighlightBlock> {

    private static final int PRIORITY = 0;

    public BlockUpdateHighlighter(@NotNull HuskClaims plugin) {
        super(plugin);
    }

    @Override
    public void cacheBlock(@NotNull OnlineUser user, @NotNull HighlightBlock origin,
                           @NotNull UpdateHighlightBlock block) {
        replacedBlocks.put(user.getUuid(), origin);
    }

    @Override
    @NotNull
    public UpdateHighlightBlock getHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type type,
                                                  @NotNull HuskClaims plugin) {
        return new UpdateHighlightBlock(position, type, plugin);
    }

    @Override
    public void showBlocks(@NotNull OnlineUser user, @NotNull Collection<UpdateHighlightBlock> blocks) {
        this.sendBlockUpdates(user, blocks);
    }

    @Override
    public void stopHighlighting(@NotNull OnlineUser user) {
        plugin.runSync(() -> this.sendBlockUpdates(user, replacedBlocks.removeAll(user.getUuid())));
    }

    private void sendBlockUpdates(@NotNull OnlineUser user, @NotNull Collection<? extends HighlightBlock> blocks) {
        plugin.sendBlockUpdates(user, HighlightBlock.getMap(blocks));
    }

    @Override
    public short getPriority() {
        return PRIORITY;
    }

    public static class UpdateHighlightBlock extends HighlightBlock {

        private UpdateHighlightBlock(@NotNull Position position, @NotNull Highlightable.Type block,
                                     @NotNull HuskClaims plugin) {
            super(position, plugin.getSettings().getHighlighter().getBlock(block, plugin));
        }

    }

}

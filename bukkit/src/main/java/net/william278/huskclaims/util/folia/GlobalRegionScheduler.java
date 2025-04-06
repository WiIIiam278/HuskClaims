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

package net.william278.huskclaims.util.folia;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Represents a scheduler for executing global region tasks.
 */
public class GlobalRegionScheduler {

    private BukkitScheduler bukkitScheduler;
    private io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler globalRegionScheduler;

    protected GlobalRegionScheduler() {
        if (FoliaScheduler.isFolia) {
            globalRegionScheduler = Bukkit.getGlobalRegionScheduler();
        } else {
            bukkitScheduler = Bukkit.getScheduler();
        }
    }

    /**
     * Schedules a task to be executed on the global region.
     *
     * @param plugin The plugin that owns the task
     * @param run    The task to execute
     */
    public void execute(@NotNull Plugin plugin, @NotNull Runnable run) {
        if (!FoliaScheduler.isFolia) {
            bukkitScheduler.runTask(plugin, run);
            return;
        }

        globalRegionScheduler.execute(plugin, run);
    }

    /**
     * Schedules a task to be executed on the global region.
     *
     * @param plugin The plugin that owns the task
     * @param task   The task to execute
     * @return {@link TaskWrapper} instance representing a wrapped task
     */
    public TaskWrapper run(@NotNull Plugin plugin, @NotNull Consumer<Object> task) {
        if (!FoliaScheduler.isFolia) {
            return new TaskWrapper(bukkitScheduler.runTask(plugin, () -> task.accept(null)));
        }

        return new TaskWrapper(globalRegionScheduler.run(plugin, (o) -> task.accept(null)));
    }

    /**
     * Schedules a task to be executed on the global region after the specified delay in ticks.
     *
     * @param plugin The plugin that owns the task
     * @param task   The task to execute
     * @param delay  The delay, in ticks before the method is invoked. Any value less-than 1 is treated as 1.
     * @return {@link TaskWrapper} instance representing a wrapped task
     */
    public TaskWrapper runDelayed(@NotNull Plugin plugin, @NotNull Consumer<Object> task, long delay) {
        if (delay < 1) delay = 1;

        if (!FoliaScheduler.isFolia) {
            return new TaskWrapper(bukkitScheduler.runTaskLater(plugin, () -> task.accept(null), delay));
        }

        return new TaskWrapper(globalRegionScheduler.runDelayed(plugin, (o) -> task.accept(null), delay));
    }

    /**
     * Schedules a repeating task to be executed on the global region after the initial delay with the specified period.
     *
     * @param plugin            The plugin that owns the task
     * @param task              The task to execute
     * @param initialDelayTicks The initial delay, in ticks before the method is invoked. Any value less-than 1 is treated as 1.
     * @param periodTicks       The period, in ticks. Any value less-than 1 is treated as 1.
     * @return {@link TaskWrapper} instance representing a wrapped task
     */
    public TaskWrapper runAtFixedRate(@NotNull Plugin plugin, @NotNull Consumer<Object> task, long initialDelayTicks, long periodTicks) {
        if (initialDelayTicks < 1) initialDelayTicks = 1;
        if (periodTicks < 1) periodTicks = 1;

        if (!FoliaScheduler.isFolia) {
            return new TaskWrapper(bukkitScheduler.runTaskTimer(plugin, () -> task.accept(null), initialDelayTicks, periodTicks));
        }

        return new TaskWrapper(globalRegionScheduler.runAtFixedRate(plugin, (o) -> task.accept(null), initialDelayTicks, periodTicks));
    }

    /**
     * Attempts to cancel all tasks scheduled by the specified plugin.
     *
     * @param plugin Specified plugin.
     */
    public void cancel(@NotNull Plugin plugin) {
        if (!FoliaScheduler.isFolia) {
            Bukkit.getScheduler().cancelTasks(plugin);
            return;
        }

        globalRegionScheduler.cancelTasks(plugin);
    }
}
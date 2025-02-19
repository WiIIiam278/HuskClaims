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

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a wrapper around {@code BukkitTask} and Paper's {@code ScheduledTask}.
 * This class provides a unified interface for interacting with both Bukkit's task scheduler
 * and Paper's task scheduler.
 */
public class TaskWrapper {

	private BukkitTask bukkitTask;
	private ScheduledTask scheduledTask;

	/**
	 * Constructs a new TaskWrapper around a BukkitTask.
	 *
	 * @param bukkitTask the BukkitTask to wrap
	 */
	public TaskWrapper(@NotNull BukkitTask bukkitTask) {
		this.bukkitTask = bukkitTask;
	}

	/**
	 * Constructs a new TaskWrapper around Paper's ScheduledTask.
	 *
	 * @param scheduledTask the ScheduledTask to wrap
	 */
	public TaskWrapper(@NotNull ScheduledTask scheduledTask) {
		this.scheduledTask = scheduledTask;
	}

	/**
	 * Retrieves the Plugin that owns this task.
	 *
	 * @return the owning {@link Plugin}
	 */
	public Plugin getOwner() {
		return bukkitTask != null ? bukkitTask.getOwner() : scheduledTask.getOwningPlugin();
	}

	/**
	 * Checks if the task is canceled.
	 *
	 * @return true if the task is canceled, false otherwise
	 */
	public boolean isCancelled() {
		return bukkitTask != null ? bukkitTask.isCancelled() : scheduledTask.isCancelled();
	}

	/**
	 * Cancels the task. If the task is running, it will be canceled.
	 */
	public void cancel() {
		if (bukkitTask != null) {
			bukkitTask.cancel();
		} else {
			scheduledTask.cancel();
		}
	}
}

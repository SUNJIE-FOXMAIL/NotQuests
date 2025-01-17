/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.spigot.managers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import rocks.gravili.notquests.spigot.NotQuests;

import java.util.logging.Level;

public class LogManager {
    private final NotQuests main;
    private Audience consoleSender;
    private final String prefixText;

    public LogManager(final NotQuests main) {
        this.main = main;

        prefixText = "<DARK_GRAY>[<WHITE>NotQuests<DARK_GRAY>]<GRAY>: ";
    }

    public void lateInit() {
        consoleSender = main.adventure().sender(Bukkit.getConsoleSender());
    }


    private void log(final Level level, final String color, final String message) {
        log(level, LogCategory.DEFAULT, color, message);
    }

    private void log(final Level level, final LogCategory logCategory, final String color, final String message) {
        if (main.isAdventureEnabled() && consoleSender != null) {
            consoleSender.sendMessage(MiniMessage.miniMessage().parse(prefixText + color + message));
        } else {
            main.getMain().getLogger().log(level, ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.builder().build().serialize(MiniMessage.miniMessage().parse(color + message))));
        }


    }


    public void info(final LogCategory logCategory, final String message) {
        if (logCategory == LogCategory.DEFAULT) {
            log(Level.INFO, logCategory, "<GREEN>", message);
        } else if (logCategory == LogCategory.DATA) {
            log(Level.INFO, logCategory, "<BLUE>", message);
        } else if (logCategory == LogCategory.LANGUAGE) {
            log(Level.INFO, logCategory, "<DARK_PURPLE>", message);
        }
    }

    public void info(final String message) {
        info(LogCategory.DEFAULT, message);
    }

    public void warn(final LogCategory logCategory, final String message) {
        log(Level.WARNING, logCategory, "<YELLOW>", message);
    }

    public void warn(final String message) {
        warn(LogCategory.DEFAULT, message);
    }

    public void severe(final LogCategory logCategory, final String message) {
        log(Level.SEVERE, logCategory, "<RED>", message);
    }

    public void severe(final String message) {
        severe(LogCategory.DEFAULT, message);
    }

    public void debug(final LogCategory logCategory, final String message) {
        if (main.getConfiguration().debug) {
            log(Level.FINE, logCategory, "<GRAY>", message);
        }
    }

    public void debug(final String message) {
        debug(LogCategory.DEFAULT, message);
    }
}


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

package rocks.gravili.notquests.spigot.structs.triggers.types;

import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.triggers.Trigger;

public class BeginTrigger extends Trigger {


    public BeginTrigger(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addTriggerBuilder) {
        manager.command(addTriggerBuilder.literal("BEGIN")
                .flag(main.getCommandManager().applyOn)
                .flag(main.getCommandManager().triggerWorldString)
                .meta(CommandMeta.DESCRIPTION, "Triggers when a Quest begins or an Objective gets unlocked ('begins')")
                .handler((context) -> {

                    BeginTrigger beginTrigger = new BeginTrigger(main);

                    main.getTriggerManager().addTrigger(beginTrigger, context);
                }));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {

    }

    @Override
    public String getTriggerDescription() {
        return null;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {

    }


}
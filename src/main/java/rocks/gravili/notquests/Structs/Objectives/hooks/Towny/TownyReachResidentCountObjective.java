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

package rocks.gravili.notquests.Structs.Objectives.hooks.Towny;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.Objectives.Objective;

public class TownyReachResidentCountObjective extends Objective {

    private boolean countPreviousResidents = true;


    public TownyReachResidentCountObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.getIntegrationsManager().isTownyEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder.literal("TownyReachResidentCount")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Minimum amounts of residents"))
                .flag(
                        manager.flagBuilder("doNotCountPreviousResidents")
                                .withDescription(ArgumentDescription.of("Makes it so only additional residents from the time of unlocking this Objective will count (and previous/existing counts will not count, so it starts from zero)"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new TownyReachResidentCount Objective to a quest")
                .handler((context) -> {
                    int amount = context.get("amount");
                    final boolean countPreviousResidents = !context.flags().isPresent("doNotCountPreviousResidents");


                    TownyReachResidentCountObjective townyReachResidentCountObjective = new TownyReachResidentCountObjective(main);
                    townyReachResidentCountObjective.setProgressNeeded(amount);
                    townyReachResidentCountObjective.setCountPreviousResidents(countPreviousResidents);

                    main.getObjectiveManager().addObjective(townyReachResidentCountObjective, context);
                }));
    }

    public void setCountPreviousResidents(final boolean countPreviousResidents) {
        this.countPreviousResidents = countPreviousResidents;
    }

    public final long getAmountOfResidentsToReach() {
        return getProgressNeeded();
    }

    public final boolean isCountPreviousResidents() {
        return countPreviousResidents;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.townyReachResidentCount.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%AMOUNT%", "" + getAmountOfResidentsToReach());
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.countPreviousResidents", isCountPreviousResidents());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        countPreviousResidents = configuration.getBoolean(initialPath + ".specifics.countPreviousResidents");
    }


    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {
        activeObjective.getQuestPlayer().sendDebugMessage("TownyReachResidentCountObjective onObjectiveUnlock");
        if (!main.getIntegrationsManager().isTownyEnabled() || !isCountPreviousResidents()) {
            activeObjective.getQuestPlayer().sendDebugMessage("TownyReachResidentCountObjective onObjectiveUnlock cancel 1");
            return;
        }

        final Player player = activeObjective.getQuestPlayer().getPlayer();
        if(player == null){
            activeObjective.getQuestPlayer().sendDebugMessage("TownyReachResidentCountObjective onObjectiveUnlock cancel 2");
            return;
        }
        Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

        if(resident == null){
            activeObjective.getQuestPlayer().sendDebugMessage("TownyReachResidentCountObjective onObjectiveUnlock cancel 3");
            return;
        }

        Town town = resident.getTownOrNull();

        if(town == null){
            activeObjective.getQuestPlayer().sendDebugMessage("TownyReachResidentCountObjective onObjectiveUnlock cancel 4");
            return;
        }

        activeObjective.getQuestPlayer().sendDebugMessage("TownyReachResidentCountObjective addProgress: " + town.getNumResidents());

        activeObjective.addProgress(town.getNumResidents());
    }
}

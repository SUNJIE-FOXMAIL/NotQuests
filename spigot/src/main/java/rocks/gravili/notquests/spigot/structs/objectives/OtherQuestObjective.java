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

package rocks.gravili.notquests.spigot.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.arguments.QuestSelector;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;
import rocks.gravili.notquests.spigot.structs.Quest;

public class OtherQuestObjective extends Objective {
    private String otherQuestName = "";
    private boolean countPreviousCompletions = false;


    public OtherQuestObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("OtherQuest")
                .argument(QuestSelector.of("other quest name", main), ArgumentDescription.of("Name of the other Quest the player has to complete."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the Quest needs to be completed."))
                .flag(
                        manager.flagBuilder("countPreviouslyCompletedQuests")
                                .withDescription(ArgumentDescription.of("Makes it so quests completed before this OtherQuest objective becomes active will be counted towards the progress too."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new OtherQuest Objective to a quest")
                .handler((context) -> {
                    final Quest otherQuest = context.get("other quest name");
                    final int amount = context.get("amount");
                    final boolean countPreviouslyCompletedQuests = context.flags().isPresent("countPreviouslyCompletedQuests");

                    OtherQuestObjective otherQuestObjective = new OtherQuestObjective(main);

                    otherQuestObjective.setOtherQuestName(otherQuest.getQuestName());
                    otherQuestObjective.setCountPreviousCompletions(countPreviouslyCompletedQuests);
                    otherQuestObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(otherQuestObjective, context);
                }));
    }

    public void setOtherQuestName(final String otherQuestName) {
        this.otherQuestName = otherQuestName;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.otherQuest.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%OTHERQUESTNAME%", "" + getOtherQuest().getQuestName());
    }

    public void setCountPreviousCompletions(final boolean countPreviousCompletions) {
        this.countPreviousCompletions = countPreviousCompletions;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.otherQuestName", getOtherQuestName());
        configuration.set(initialPath + ".specifics.countPreviousCompletions", isCountPreviousCompletions());
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }

    public final long getAmountOfCompletionsNeeded() {
        return super.getProgressNeeded();
    }

    public final boolean isCountPreviousCompletions() {
        return countPreviousCompletions;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        otherQuestName = configuration.getString(initialPath + ".specifics.otherQuestName");
        countPreviousCompletions = configuration.getBoolean(initialPath + ".specifics.countPreviousCompletions");
    }
}

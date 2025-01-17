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

package rocks.gravili.notquests.spigot.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;

public class QuestPointsCondition extends Condition {

    private boolean deductQuestPoints = false;


    public QuestPointsCondition(NotQuests main) {
        super(main);
    }

    public void setDeductQuestPoints(final boolean deductQuestPoints){
        this.deductQuestPoints = deductQuestPoints;
    }


    public final long getQuestPointRequirement() {
        return getProgressNeeded();
    }


    public final boolean isDeductQuestPoints() {
        return deductQuestPoints;
    }

    @Override
    public String check(final QuestPlayer questPlayer, final boolean enforce) {
        final long questPointRequirementAmount = getQuestPointRequirement();
        final boolean deductQuestPoints = isDeductQuestPoints();

        if (questPlayer.getQuestPoints() < questPointRequirementAmount) {
            return "<YELLOW>You need <AQUA>" + (questPointRequirementAmount - questPlayer.getQuestPoints()) + "</AQUA> more quest points.";
        } else {
            if (enforce && deductQuestPoints && questPointRequirementAmount > 0) {
                questPlayer.removeQuestPoints(questPointRequirementAmount, true);
            }
            return "";
        }
    }

    @Override
    public void save(FileConfiguration configuration, final String initialPath) {
        configuration.set(initialPath + ".specifics.deductQuestPoints", isDeductQuestPoints());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.deductQuestPoints = configuration.getBoolean(initialPath + ".specifics.deductQuestPoints");

    }

    @Override
    public String getConditionDescription() {
        String description = "<GRAY>-- Quest points needed: " + getQuestPointRequirement() + "</GRAY>";
        if (isDeductQuestPoints()) {
            description += "\n<GRAY>--- <RED>Quest points WILL BE DEDUCTED!";
        } else {
            description += "\n<GRAY>--- Will quest points be deducted?: No";
        }
        return description;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        manager.command(builder.literal("QuestPoints")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of QuestPoints needed"))
                .flag(
                        manager.flagBuilder("deductQuestPoints")
                                .withDescription(ArgumentDescription.of("Makes it so the required quest points are deducted from the players balance if the Quest is accepted."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new QuestPoints Requirement to a quest")
                .handler((context) -> {

                    final int amount = context.get("amount");
                    final boolean deductQuestPoints = context.flags().isPresent("deductQuestPoints");

                    QuestPointsCondition questPointsCondition = new QuestPointsCondition(main);
                    questPointsCondition.setProgressNeeded(amount);
                    questPointsCondition.setDeductQuestPoints(deductQuestPoints);

                    main.getConditionsManager().addCondition(questPointsCondition, context);
                }));
    }
}

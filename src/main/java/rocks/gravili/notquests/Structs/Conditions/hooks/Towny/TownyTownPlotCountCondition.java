package rocks.gravili.notquests.Structs.Conditions.hooks.Towny;

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
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.Conditions.ConditionFor;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class TownyTownPlotCountCondition extends Condition {

    private int minTownPlotCount = 1;

    public TownyTownPlotCountCondition(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (!main.isTownyEnabled()) {
            return;
        }

        manager.command(builder.literal("TownyTownPlotCount")
                .argument(IntegerArgument.<CommandSender>newBuilder("min Plot Count").withMin(1), ArgumentDescription.of("Minimum Town plot count"))
                .meta(CommandMeta.DESCRIPTION, "Creates a new TownyTownPlotCount Condition")
                .handler((context) -> {
                    final int minPlotCount = context.get("min Plot Count");

                    TownyTownPlotCountCondition townyTownPlotCountCondition = new TownyTownPlotCountCondition(main);
                    townyTownPlotCountCondition.setMinTownPlotCount(minPlotCount);


                    main.getConditionsManager().addCondition(townyTownPlotCountCondition, context);
                }));
    }

    public final int getMinTownPlotCount() {
        return minTownPlotCount;
    }

    public void setMinTownPlotCount(final int minTownPlotCount) {
        this.minTownPlotCount = minTownPlotCount;
    }

    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {
        if (!main.isTownyEnabled()) {
            return "\n§eError: The server does not have Towny enabled. Please ask the Owner to install Towny for Towny stuff to work.";
        }

        final Player player = questPlayer.getPlayer();
        if (player != null) {
            Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUUID());
            if (resident != null && resident.getTownOrNull() != null && resident.hasTown()) {
                Town town = resident.getTownOrNull();
                if (town.getPlotGroups().size() >= getMinTownPlotCount()) {
                    return "";
                } else {
                    return "\n§eYour town needs to have at least §b" + getMinTownPlotCount() + "§e plot groups.";
                }
            } else {
                return "\n§eYou need to be in a town";
            }


        } else {
            return "\n§eError reading TownyTownPlotCount requirement...";
        }
    }

    @Override
    public String getConditionDescription() {
        return "§7-- Minimum town plots: " + getMinTownPlotCount() + "\n";
    }


    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.minTownPlotCount", getMinTownPlotCount());

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.minTownPlotCount = configuration.getInt(initialPath + ".specifics.minTownPlotCount");

    }
}
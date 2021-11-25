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

package rocks.gravili.notquests.Managers;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import rocks.gravili.notquests.Commands.CommandNotQuests;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Commands.newCMDs.AdminCommands;
import rocks.gravili.notquests.Commands.newCMDs.AdminEditCommands;
import rocks.gravili.notquests.Commands.old.CommandNotQuestsAdmin;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommandManager {
    private final NotQuests main;
    private final boolean useNewCommands = true;
    private PaperCommandManager<CommandSender> commandManager;
    private MinecraftHelp<CommandSender> minecraftHelp;
    private Command.Builder<CommandSender> adminCommandBuilder;
    private Command.Builder<CommandSender> adminEditAddObjectiveCommandBuilder;

    private final Commodore commodore;


    public CommandManager(final NotQuests main) {
        this.main = main;
        if (CommodoreProvider.isSupported()) {
            // get a commodore instance
            commodore = CommodoreProvider.getCommodore(main);
        } else {
            commodore = null;
        }
    }


    private void registerCommodoreCompletions(Commodore commodore, PluginCommand command) {
        if (CommodoreProvider.isSupported()) {

            /*LiteralCommandNode<?> timeCommand = LiteralArgumentBuilder.literal("notquestsadmin")
                    .then(LiteralArgumentBuilder.literal("create")
                            .then(LiteralArgumentBuilder.literal("day"))
                            .then(LiteralArgumentBuilder.literal("noon"))
                            .then(LiteralArgumentBuilder.literal("night"))
                            .then(LiteralArgumentBuilder.literal("midnight"))
                            .then(RequiredArgumentBuilder.argument("time", IntegerArgumentType.integer())))
                    .then(LiteralArgumentBuilder.literal("delete")
                            .then(RequiredArgumentBuilder.argument("time", IntegerArgumentType.integer())))
                    .then(LiteralArgumentBuilder.literal("query")
                            .then(LiteralArgumentBuilder.literal("daytime"))
                            .then(LiteralArgumentBuilder.literal("gametime"))
                            .then(LiteralArgumentBuilder.literal("day"))
                    ).build();

            commodore.register(command, timeCommand);*/
        }

    }

    public void preSetupCommands() {
        if (useNewCommands) {
            //Cloud command framework
            try {
                commandManager = new PaperCommandManager<>(
                        /* Owning plugin */ main,
                        /* Coordinator function */ CommandExecutionCoordinator.simpleCoordinator(),
                        /* Command Sender -> C */ Function.identity(),
                        /* C -> Command Sender */ Function.identity()
                );
            } catch (final Exception e) {
                main.getLogManager().severe("There was an error setting up the commands.");
                return;
            }


            adminCommandBuilder = commandManager.commandBuilder("qa2", ArgumentDescription.of("Admin commands for NotQuests"), "notquestsadmin2, nqa2")
                    .permission("notquests.admin");


            adminEditAddObjectiveCommandBuilder = adminCommandBuilder
                    .literal("edit")
                    .argument(StringArgument.<CommandSender>newBuilder("Quest Name").withSuggestionsProvider(
                            (context, lastString) -> {
                                final List<String> allArgs = context.getRawInput();
                                final Audience audience = main.adventure().sender(context.getSender());
                                main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Quest Name]", "[...]");
                                ArrayList<String> completions = new ArrayList<>();
                                for (Quest quest : main.getQuestManager().getAllQuests()) {
                                    completions.add(quest.getQuestName());
                                }
                                return completions;
                            }
                    ).single().build(), ArgumentDescription.of("Quest Name"))
                    .literal("objectives")
                    .literal("add");

            //asynchronous completions
            if (commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
            }

            //brigadier/commodore
            try {
                commandManager.registerBrigadier();
            } catch (final Exception e) {
                main.getLogger().warning("Failed to initialize Brigadier support: " + e.getMessage());
            }

            minecraftHelp = new MinecraftHelp<>(
                    "/qa2 help",
                    main.adventure()::sender,
                    commandManager
            );

            minecraftHelp.setHelpColors(MinecraftHelp.HelpColors.of(
                    NotQuestColors.main,
                    NamedTextColor.WHITE,
                    NotQuestColors.highlight,
                    NamedTextColor.GRAY,
                    NamedTextColor.DARK_GRAY
            ));
        }

    }

    public void setupCommands() {

        PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadmin");
        if (notQuestsAdminCommand != null) {
            final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
            notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
            notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);


            registerCommodoreCompletions(commodore, notQuestsAdminCommand);
        }
        //Register the notquests command & tab completer. This command will be used by Players
        final PluginCommand notQuestsCommand = main.getCommand("notquests");
        if (notQuestsCommand != null) {
            final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
            notQuestsCommand.setExecutor(commandNotQuests);
            notQuestsCommand.setTabCompleter(commandNotQuests);


        }


        if (!useNewCommands) {
            /*final PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadmin");
            if (notQuestsAdminCommand != null) {
                final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
                notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
                notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);
            }
            //Register the notquests command & tab completer. This command will be used by Players
            final PluginCommand notQuestsCommand = main.getCommand("notquests");
            if (notQuestsCommand != null) {
                final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
                notQuestsCommand.setExecutor(commandNotQuests);
                notQuestsCommand.setTabCompleter(commandNotQuests);
            }*/
        } else {





            constructCommands();
        }


    }


    public void constructCommands() {


       /* final Command.Builder<CommandSender> helpBuilder = commandManager.commandBuilder("notquestsadmin", "qa");
        commandManager.command(helpBuilder.meta(CommandMeta.DESCRIPTION, "fwefwe")
                .senderType(Player.class)
                .handler(commandContext -> {
                    minecraftHelp.queryCommands(commandContext.getOrDefault("", ""), commandContext.getSender());
                }));

        helpBuilder.literal("notquestsadmin",
                ArgumentDescription.of("Your Description"));*/


       /* commandManager.command(
                builder.literal("help", new String[0])
                .argument(com.magmaguy.shaded.cloud.arguments.standard.StringArgument.optional("query", com.magmaguy.shaded.cloud.arguments.standard.StringArgument.StringMode.GREEDY)).handler((context) -> {
                    this.minecraftHelp.queryCommands((String)context.getOrDefault("query", ""), context.getSender());
                })
        );*/


        //Help menu
        commandManager.command(adminCommandBuilder.meta(CommandMeta.DESCRIPTION, "Opens the help menu").handler((context) -> {
            minecraftHelp.queryCommands("qa2 *", context.getSender());
            final Audience audience = main.adventure().sender(context.getSender());
            final List<String> allArgs = context.getRawInput();
            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[What would you like to do?]", "[...]");

        }));
        commandManager.command(
                adminCommandBuilder.literal("help")
                        .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))

                        .handler(context -> {
                            minecraftHelp.queryCommands(context.getOrDefault("query", "qa2 *"), context.getSender());
                        })
        );


        MinecraftExceptionHandler<CommandSender> exceptionHandler = new MinecraftExceptionHandler<CommandSender>()
                .withArgumentParsingHandler()
                .withInvalidSenderHandler()
                .withInvalidSyntaxHandler()
                .withNoPermissionHandler()
                .withCommandExecutionHandler()
                .withDecorator(message -> {

                            return Component.text("NotQuests > ").color(NotQuestColors.main).append(Component.space()).append(message);
                        }
                )
                .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX, (sender, e) -> {
                    minecraftHelp.queryCommands(e.getMessage().split("syntax is: ")[1], sender);

                    return Component.text(e.getMessage(), NamedTextColor.RED);
                });

        exceptionHandler.apply(commandManager, main.adventure()::sender);

        new AdminCommands(main, commandManager, adminCommandBuilder);

        new AdminEditCommands(main, commandManager, adminCommandBuilder);


    }

    public final PaperCommandManager<CommandSender> getPaperCommandManager() {
        return commandManager;
    }

    public final Command.Builder<CommandSender> getAdminEditAddObjectiveCommandBuilder() {
        return adminEditAddObjectiveCommandBuilder;
    }


}
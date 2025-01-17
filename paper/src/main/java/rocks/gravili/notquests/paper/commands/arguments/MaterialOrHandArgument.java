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
//
// MIT License
//
// Copyright (c) 2021 Alexander Söderberg & Contributors
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package rocks.gravili.notquests.paper.commands.arguments;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.bukkit.BukkitCaptionKeys;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * cloud argument type that parses Bukkit {@link Material materials}
 *
 * @param <C> Command sender type
 */
public class MaterialOrHandArgument<C> extends CommandArgument<C, MaterialOrHand> {


    protected MaterialOrHandArgument(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main
    ) {
        super(required, name, new MaterialOrHandArgument.MaterialParser<>(main), defaultValue, MaterialOrHand.class, suggestionsProvider);
    }

    /**
     * Create a new builder
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> MaterialOrHandArgument.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
        return new MaterialOrHandArgument.Builder<>(name, main);
    }

    /**
     * Create a new required argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, MaterialOrHand> of(final @NonNull String name, final NotQuests main) {
        return MaterialOrHandArgument.<C>newBuilder(name, main).asRequired().build();
    }

    /**
     * Create a new optional argument
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, MaterialOrHand> optional(final @NonNull String name, final NotQuests main) {
        return MaterialOrHandArgument.<C>newBuilder(name, main).asOptional().build();
    }

    /**
     * Create a new optional argument with a default value
     *
     * @param name     Argument name
     * @param material Default value
     * @param <C>      Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, MaterialOrHand> optional(
            final @NonNull String name,
            final @NonNull MaterialOrHand material,
            final NotQuests main
    ) {
        return MaterialOrHandArgument.<C>newBuilder(name, main).asOptionalWithDefault(material.material.toLowerCase()).build();
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, MaterialOrHand> {
        private final NotQuests main;

        private Builder(final @NonNull String name, NotQuests main) {
            super(MaterialOrHand.class, name);
            this.main = main;
        }

        @Override
        public @NonNull CommandArgument<C, MaterialOrHand> build() {
            return new MaterialOrHandArgument<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    this.main
            );
        }

    }

    public static final class MaterialParser<C> implements ArgumentParser<C, MaterialOrHand> {

        private final NotQuests main;

        /**
         * Constructs a new MaterialParser.
         */
        public MaterialParser(
                NotQuests main
        ) {
            this.main = main;
        }

        @Override
        public @NonNull ArgumentParseResult<MaterialOrHand> parse(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(
                        MaterialOrHandArgument.MaterialParser.class,
                        commandContext
                ));
            }

            try {
                final MaterialOrHand materialOrHand = new MaterialOrHand();

                if (input.equalsIgnoreCase("hand")) {
                    materialOrHand.hand = true;
                    inputQueue.remove();
                    return ArgumentParseResult.success(materialOrHand);
                } else if (input.equalsIgnoreCase("any")) {
                    materialOrHand.material = "any";
                    inputQueue.remove();
                    return ArgumentParseResult.success(materialOrHand);
                } else {
                    materialOrHand.material = Material.valueOf(input.toUpperCase()).name();
                    inputQueue.remove();
                    return ArgumentParseResult.success(materialOrHand);
                }
            } catch (final IllegalArgumentException exception) {
                return ArgumentParseResult.failure(new MaterialOrHandArgument.MaterialParseException(input, commandContext));
            }
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(
                final @NonNull CommandContext<C> context,
                final @NonNull String input
        ) {
            final List<String> completions = new ArrayList<>();
            for (Material value : Material.values()) {
                completions.add(value.name().toLowerCase());
            }
            completions.add("hand");
            completions.add("any");


            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Item Name / 'hand' / 'any']", "[...]");

            return completions;
        }

    }


    public static final class MaterialParseException extends ParserException {

        private static final long serialVersionUID = 1615554107385965610L;
        private final String input;

        /**
         * Construct a new MaterialParseException
         *
         * @param input   Input
         * @param context Command context
         */
        public MaterialParseException(
                final @NonNull String input,
                final @NonNull CommandContext<?> context
        ) {
            super(
                    MaterialOrHandArgument.MaterialParser.class,
                    context,
                    BukkitCaptionKeys.ARGUMENT_PARSE_FAILURE_MATERIAL,
                    CaptionVariable.of("input", input)
            );
            this.input = input;
        }

        /**
         * Get the input
         *
         * @return Input
         */
        public @NonNull String getInput() {
            return this.input;
        }

    }

}

package com.elfmcys.yesstevemodel.command;

import com.elfmcys.yesstevemodel.command.subcommands.client.CacheCommand;
import com.elfmcys.yesstevemodel.util.YSMMessageFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;


public class OpenYSMClientCommand {

    public static void registerClientCommands(CommandDispatcher<net.minecraft.commands.CommandSourceStack> commandDispatcher) {
        LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> root = LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("openysm")
                .requires(source -> true);

        root.then(CacheCommand.register());

        commandDispatcher.register(root);
    }
}
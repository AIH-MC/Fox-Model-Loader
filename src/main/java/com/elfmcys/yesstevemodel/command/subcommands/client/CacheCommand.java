package com.elfmcys.yesstevemodel.command.subcommands.client;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.util.YSMMessageFormatter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class CacheCommand {

    public static LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> register() {
        return LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("cache")
                .then(LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("dump")
                        .executes(CacheCommand::dumpCache));
    }

    private static int dumpCache(CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }

        player.displayClientMessage(YSMMessageFormatter.withPrefix(Component.literal("开始解析并导出客户端缓存模型...")), false);

        ClientModelManager.exportAllCachedModels(null, exportResult -> {
            if (exportResult.getMessage() != null) {
                player.displayClientMessage(YSMMessageFormatter.withPrefix(exportResult.getMessage()), false);
            }
            if (exportResult.isSuccess()) {
                player.displayClientMessage(Component.translatable("commands.yes_steve_model.export.success", exportResult.getFilePath()), false);
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
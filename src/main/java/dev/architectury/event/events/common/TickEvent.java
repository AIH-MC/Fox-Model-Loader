package dev.architectury.event.events.common;

import dev.architectury.event.Event;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

/**
 * NeoForge-compatible subset of Architectury's server tick events.
 */
public class TickEvent {
    public static final Event<Consumer<MinecraftServer>> SERVER_POST = new Event<>();

    public static void fireServerPost(MinecraftServer server) {
        SERVER_POST.fire(handler -> handler.accept(server));
    }
}

package dev.architectury.event.events.common;

import dev.architectury.event.Event;
import dev.architectury.event.EventResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.function.BiFunction;

/**
 * MC 26.x: NeoForge-compatible EntityEvent.
 */
public class EntityEvent {
    public static final Event<BiFunction<Entity, Level, EventResult>> ADD = new Event<>();

    public static EventResult fireAdd(Entity entity, Level level) {
        return ADD.fireEventResult(handler -> handler.apply(entity, level));
    }
}

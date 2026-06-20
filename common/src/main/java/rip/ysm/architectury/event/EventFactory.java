package rip.ysm.architectury.event;

/**
 * Stub for rip.ysm.architectury.event.EventFactory.
 */
public class EventFactory {
    public static <T> Event<T> createEventResult() {
        return new Event<>();
    }
}

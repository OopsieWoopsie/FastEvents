package me.sheidy.fastevents;

import org.bukkit.event.Event;

@FunctionalInterface
public interface EventHandler {
    void handle(Event event);
}

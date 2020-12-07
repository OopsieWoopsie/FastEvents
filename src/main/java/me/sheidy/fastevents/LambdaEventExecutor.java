package me.sheidy.fastevents;

import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.spigotmc.CustomTimingsHandler;

final class LambdaEventExecutor implements EventExecutor {

    private final Class<?> eventClass;
    private final CustomTimingsHandler timings;
    private final EventHandler handler;

    public LambdaEventExecutor(Class<?> eventClass, CustomTimingsHandler timings, EventHandler handler) {
        this.eventClass = eventClass;
        this.timings = timings;
        this.handler = handler;
    }

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        try {
            if (eventClass.isAssignableFrom(event.getClass())) {
                boolean isAsync = event.isAsynchronous();

                if (!isAsync) {
                    timings.startTiming();
                }

                handler.handle(event);

                if (!isAsync) {
                    timings.stopTiming();
                }
            }
        } catch (Throwable e) {
            throw new EventException(e);
        }
    }
}

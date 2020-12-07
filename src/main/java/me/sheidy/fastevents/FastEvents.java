package me.sheidy.fastevents;

import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

public class FastEvents {

    private final Logger log;

    public FastEvents(Logger log) {
        this.log = log;
    }

    public synchronized Map<String, Integer> execute() {
        Map<String, Integer> pluginStats = new HashMap<>();

        for (HandlerList handler : HandlerList.getHandlerLists()) {
            for (RegisteredListener listener : handler.getRegisteredListeners()) {
                if (tryRebuild(listener)) {
                    String plugin = listener.getPlugin().getName();
                    pluginStats.put(plugin, pluginStats.getOrDefault(plugin, 0) + 1);
                }
            }
        }

        return pluginStats;
    }

    private boolean tryRebuild(RegisteredListener listener) {
        try {
            EventExecutor executor = getDefaultExecutor(listener);

            if (executor instanceof LambdaEventExecutor) {
                return false;
            }

            if (!isBukkitExecutor(executor)) {
                return false;
            }

            Class<?> executorClass = executor.getClass();

            Method method = Util.getFieldValue(executorClass, executor, "val$method");

            EventExecutor lambdaExecutor = new LambdaEventExecutor(
                    Util.getFieldValue(executorClass, executor, "val$eventClass"),
                    Util.getFieldValue(executorClass, executor, "val$timings"),
                    createLambda(listener.getListener(), method));

            return replaceExecutor(listener, lambdaExecutor);
        } catch (Exception e) {
            String plugin  = listener.getPlugin().getName();
            String clazz   = listener.getListener().getClass().getName();
            String message = e.getMessage();

            log.warning("Unable to optimize events for plugin " + plugin + " (" + clazz + ") : " + message);
        }
        return false;
    }

    private boolean isBukkitExecutor(EventExecutor executor) {
        try {
            Class<?> defClass = Class.forName(
                    "org.bukkit.plugin.java.JavaPluginLoader$1",
                    false, Bukkit.class.getClassLoader());

            return defClass.isAssignableFrom(executor.getClass());
        } catch (ClassNotFoundException e) {}
        return false;
    }

    private EventExecutor getDefaultExecutor(RegisteredListener listener) throws ReflectiveOperationException {
        return Util.getFieldValue(RegisteredListener.class, listener, "executor");
    }

    private boolean replaceExecutor(RegisteredListener listener, EventExecutor ex) throws ReflectiveOperationException {
        Field exField = RegisteredListener.class.getDeclaredField("executor");

        if (Modifier.isFinal(exField.getModifiers())) {
            Field mField = Field.class.getDeclaredField("modifiers");
            mField.setAccessible(true);
            mField.setInt(exField, exField.getModifiers() & ~Modifier.FINAL);
        }

        exField.setAccessible(true);
        exField.set(listener, ex);

        return exField.get(listener) == ex;
    }

    private EventHandler createLambda(Listener listenerIns, Method m) throws ReflectiveOperationException {
        try {
            Lookup caller   = Util.createPrivilegedLookup(m.getDeclaringClass());
            MethodHandle mh = caller.unreflect(m);

            MethodType mType = MethodType.methodType(Void.TYPE, m.getParameterTypes()[0]);
            MethodType iType = MethodType.methodType(Void.TYPE, Event.class);
            MethodType fType = MethodType.methodType(EventHandler.class, m.getDeclaringClass());

            MethodHandle target = LambdaMetafactory.metafactory(
                caller, "handle", fType,
                iType, mh, mType
            ).getTarget().bindTo(listenerIns);

            return (EventHandler) target.invokeExact();
        } catch (LambdaConversionException e) {
            throw new ReflectiveOperationException(e);
        } catch (ReflectiveOperationException e) {
            throw e; // because we catch Throwable below
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}

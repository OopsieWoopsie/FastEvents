package me.sheidy.fastevents;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class Util {

    public static Lookup createPrivilegedLookup(Class<?> clazz) throws ReflectiveOperationException {
        Constructor<Lookup> c;

        try {
            c = Lookup.class.getDeclaredConstructor(Class.class, int.class);
            c.setAccessible(true);
            return c.newInstance(clazz, 15);
        } catch (NoSuchMethodException e) {
            c = Lookup.class.getDeclaredConstructor(Class.class);
            c.setAccessible(true);
            return c.newInstance(clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Class<?> clazz, Object instance, String fieldName) throws ReflectiveOperationException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(instance);
    }
}

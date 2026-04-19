package xyz.immortius.chunkbychunk;

import net.minecraft.core.MappedRegistry;
import java.lang.reflect.Field;
import java.util.Map;

public class TestReflection {
    public static void printFields() {
        System.out.println("MappedRegistry fields:");
        for (Field f : MappedRegistry.class.getDeclaredFields()) {
            System.out.println(f.getType().getName() + " " + f.getName());
        }
    }
}

package xyz.immortius.chunkbychunk.common.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;

/**
 * Implementation for changing the dimension of entities by providing portal information
 */
public final class ChangeDimensionHelper {

    private static final ThreadLocal<DimensionTransition> DIMENSION_TRANSITION = new ThreadLocal<>();

    private ChangeDimensionHelper() {

    }

    public static DimensionTransition getDimensionTransition() {
        return DIMENSION_TRANSITION.get();
    }

    public static Entity changeDimension(Entity entity, ServerLevel level, DimensionTransition transition) {
        DIMENSION_TRANSITION.set(transition);
        Entity result = entity.changeDimension(transition);
        DIMENSION_TRANSITION.remove();
        return result;
    }
}
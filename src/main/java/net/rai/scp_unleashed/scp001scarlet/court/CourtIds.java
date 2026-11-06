package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

public final class CourtIds {
    private CourtIds() {
    }

    public static String id(Entity Subject) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(Subject.getType()).getPath();
    }
}

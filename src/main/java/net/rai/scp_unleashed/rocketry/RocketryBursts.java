package net.rai.scp_unleashed.rocketry;

import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;

public final class RocketryBursts {
    public static final byte Liftoff = 70;
    public static final byte Ignition = 71;
    public static final byte LandSoft = 72;
    public static final byte LandHard = 73;
    public static final byte Salvage = 74;
    private static BiConsumer<Entity, Byte> Handler = (Source, Id) -> {
    };

    private RocketryBursts() {
    }

    public static void setHandler(BiConsumer<Entity, Byte> Wanted) {
        Handler = Wanted;
    }

    public static boolean handles(byte Id) {
        return Id >= Liftoff && Id <= Salvage;
    }

    public static void play(Entity Source, byte Id) {
        Handler.accept(Source, Id);
    }
}

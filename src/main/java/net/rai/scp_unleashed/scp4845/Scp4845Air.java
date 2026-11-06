package net.rai.scp_unleashed.scp4845;

import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.space.SpaceAir;

public final class Scp4845Air {
    private Scp4845Air() {
    }

    public static void register() {
        SpaceAir.registerShelter(Scp4845Air::breathable);
    }

    public static boolean breathable(LivingEntity Breather) {
        Scp4845Site Site = Scp4845Restaurant.in(Breather.level());
        return Site != null && Site.breathable(Breather.level(), Breather.getEyePosition());
    }
}

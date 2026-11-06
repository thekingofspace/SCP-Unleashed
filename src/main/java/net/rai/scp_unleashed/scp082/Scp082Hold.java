package net.rai.scp_unleashed.scp082;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Scp082Hold {
    private Scp082Hold() {
    }

    public static void seize(Scp082Fernand Holder, LivingEntity Victim) {
        CompoundTag Mark = new CompoundTag();
        Mark.putUUID("Holder", Holder.getUUID());
        CompoundTag Earlier = Victim.getExistingDataOrNull(Scp082Registry.Held.get());
        if (Victim instanceof Mob Body) {
            Mark.putBoolean("NoAi", Earlier != null && Earlier.contains("NoAi") ? Earlier.getBoolean("NoAi") : Body.isNoAi());
            Body.getNavigation().stop();
            Body.setTarget(null);
            Body.setNoAi(true);
        }
        Victim.setData(Scp082Registry.Held.get(), Mark);
        Victim.setSprinting(false);
        Victim.stopUsingItem();
    }

    public static boolean isHeld(@Nullable Entity Found) {
        return Found instanceof LivingEntity Body && Body.getExistingDataOrNull(Scp082Registry.Held.get()) != null;
    }

    @Nullable
    public static UUID holder(LivingEntity Victim) {
        CompoundTag Mark = Victim.getExistingDataOrNull(Scp082Registry.Held.get());
        return Mark != null && Mark.hasUUID("Holder") ? Mark.getUUID("Holder") : null;
    }

    public static boolean heldBy(LivingEntity Victim, Scp082Fernand Holder) {
        return Holder.getUUID().equals(holder(Victim));
    }

    public static void lock(LivingEntity Victim) {
        if (Victim instanceof Mob Body && !Body.isNoAi()) {
            Body.getNavigation().stop();
            Body.setNoAi(true);
        }
    }

    public static void release(LivingEntity Victim) {
        CompoundTag Mark = Victim.getExistingDataOrNull(Scp082Registry.Held.get());
        if (Mark == null) {
            return;
        }
        if (Victim instanceof Mob Body) {
            Body.setNoAi(Mark.getBoolean("NoAi"));
        }
        Victim.removeData(Scp082Registry.Held.get());
    }
}

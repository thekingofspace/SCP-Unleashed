package net.rai.scp_unleashed.guns;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record GunHeat(float Amount, long Tick) {
    public static final StreamCodec<ByteBuf, GunHeat> HeatStreamCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GunHeat::Amount,
            ByteBufCodecs.VAR_LONG, GunHeat::Tick,
            GunHeat::new);

    public float at(GunStats Stats, long Now) {
        return Math.max(0.0F, Amount - Stats.getHeatCooling() * Math.max(0L, Now - Tick));
    }

    public static float level(ItemStack Stack, GunStats Stats, long Now) {
        GunHeat Stored = Stack.get(GunsRegistry.Heat.get());
        return Stored == null ? 0.0F : Stored.at(Stats, Now);
    }

    public static float share(ItemStack Stack, GunStats Stats, long Now) {
        return Stats.usesHeat() ? Math.min(1.0F, level(Stack, Stats, Now) / Stats.getHeatLimit()) : 0.0F;
    }

    @Nullable
    public static GunTimer vent(ItemStack Stack) {
        return Stack.get(GunsRegistry.Venting.get());
    }

    public static boolean venting(ItemStack Stack, long Now) {
        GunTimer Vent = vent(Stack);
        return Vent != null && !Vent.isDone(Now);
    }

    public static boolean addShot(ItemStack Stack, GunStats Stats, long Now) {
        if (!Stats.usesHeat()) {
            return false;
        }
        float Rising = level(Stack, Stats, Now) + Stats.getHeatPerShot();
        if (Rising >= Stats.getHeatLimit()) {
            Stack.remove(GunsRegistry.Heat.get());
            Stack.set(GunsRegistry.Venting.get(), new GunTimer(Now, Now + Stats.getVentTicks()));
            return true;
        }
        Stack.set(GunsRegistry.Heat.get(), new GunHeat(Rising, Now));
        return false;
    }

    public static boolean cooled(ItemStack Stack, long Now) {
        GunTimer Vent = vent(Stack);
        if (Vent == null || !Vent.isDone(Now)) {
            return false;
        }
        Stack.remove(GunsRegistry.Venting.get());
        return true;
    }
}

package net.rai.scp_unleashed.z0002.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.z0002.network.Z0002ChannelPayload;

import java.util.HashMap;
import java.util.Map;

public final class Z0002ClientChannel {
    public static final float RaiseTicks = 8.0f;
    private static final Map<Integer, long[]> Channels = new HashMap<>();

    private Z0002ClientChannel() {
    }

    public static void accept(Z0002ChannelPayload Payload) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            return;
        }
        if (Payload.Ticks() <= 0) {
            Channels.remove(Payload.EntityId());
            return;
        }
        long Now = Game.level.getGameTime();
        Channels.put(Payload.EntityId(), new long[]{Now, Now + Payload.Ticks()});
    }

    public static float raise(Entity Holder, float PartialTick) {
        long[] Found = Channels.get(Holder.getId());
        if (Found == null) {
            return 0.0f;
        }
        double Time = Holder.level().getGameTime() + PartialTick;
        if (Time > Found[1] + 2) {
            return 0.0f;
        }
        float Rise = Mth.clamp((float) (Time - Found[0]) / RaiseTicks, 0.0f, 1.0f);
        return Rise * Rise * (3.0f - 2.0f * Rise);
    }

    public static boolean channelling(Entity Holder) {
        return raise(Holder, 0.0f) > 0.0f;
    }

    public static void tick(Minecraft Game) {
        if (Game.level == null) {
            Channels.clear();
            return;
        }
        long Now = Game.level.getGameTime();
        Channels.values().removeIf(Span -> Now > Span[1] + 20);
    }

    public static void reset() {
        Channels.clear();
    }
}

package net.rai.scp_unleashed.scp330.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp330.Scp330Marks;
import net.rai.scp_unleashed.scp330.Scp330Registry;

import java.util.HashMap;
import java.util.Map;

public final class Scp330ClientState {
    private static final Map<Integer, Byte> Marks = new HashMap<>();

    private Scp330ClientState() {
    }

    public static void set(int EntityId, byte Flags) {
        if (Flags == 0) {
            Marks.remove(EntityId);
        } else {
            Marks.put(EntityId, Flags);
        }
    }

    private static byte flags(Entity Target) {
        Byte Found = Marks.get(Target.getId());
        return Found == null ? 0 : Found;
    }

    public static boolean isGreedy(Entity Target) {
        if ((flags(Target) & Scp330Marks.Greedy) != 0) {
            return true;
        }
        return Target == Minecraft.getInstance().player && Target instanceof LivingEntity Living && Living.hasEffect(Scp330Registry.Greed);
    }

    public static boolean isShielded(Entity Target) {
        return (flags(Target) & Scp330Marks.Shielded) != 0;
    }

    public static void clear() {
        Marks.clear();
    }
}

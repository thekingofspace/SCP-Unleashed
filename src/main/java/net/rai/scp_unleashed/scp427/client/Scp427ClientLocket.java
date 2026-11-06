package net.rai.scp_unleashed.scp427.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp427.Scp427LocketItem;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Scp427ClientLocket {
    public static final float OpenTicks = 8.0f;
    public static final float Frames = 12.0f;
    public static final float Span = 26.0f;
    private static final Map<Integer, float[]> Progress = new HashMap<>();

    private Scp427ClientLocket() {
    }

    public static boolean opening(LivingEntity Holder) {
        return Holder.isUsingItem() && Holder.getUsedItemHand() == InteractionHand.MAIN_HAND && Scp427LocketItem.is(Holder.getUseItem());
    }

    public static void tick(ClientLevel World) {
        for (Entity Candidate : World.entitiesForRendering()) {
            if (Candidate instanceof LivingEntity Holder && Scp427LocketItem.is(Holder.getMainHandItem())) {
                float[] Entry = Progress.computeIfAbsent(Holder.getId(), Id -> new float[2]);
                Entry[0] = Entry[1];
                Entry[1] = opening(Holder) ? Math.min(1.0f, Entry[1] + 1.0f / OpenTicks) : Math.max(0.0f, Entry[1] - 1.0f / OpenTicks);
            }
        }
        Iterator<Map.Entry<Integer, float[]>> Entries = Progress.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<Integer, float[]> Entry = Entries.next();
            Entity Found = World.getEntity(Entry.getKey());
            if (!(Found instanceof LivingEntity Holder) || !Scp427LocketItem.is(Holder.getMainHandItem())) {
                Entries.remove();
            }
        }
    }

    public static void reset() {
        Progress.clear();
    }

    public static float progress(LivingEntity Holder, float Partial) {
        float[] Entry = Progress.get(Holder.getId());
        return Entry == null ? 0.0f : Mth.lerp(Partial, Entry[0], Entry[1]);
    }

    public static float property(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Holder, int Seed) {
        if (Holder == null || Holder.getMainHandItem() != Stack) {
            return 0.0f;
        }
        float Partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float Open = progress(Holder, Partial);
        if (Holder.getMainArm() == HumanoidArm.LEFT) {
            return (Frames + 1.0f + Open * Frames) / Span;
        }
        return Open * Frames / Span;
    }
}

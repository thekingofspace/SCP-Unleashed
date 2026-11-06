package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp902.Scp902BoxItem;
import net.rai.scp_unleashed.scp902.Scp902Latch;
import net.rai.scp_unleashed.scp902.Scp902Lid;
import org.jetbrains.annotations.Nullable;

public class Scp902BoxItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final long Forgotten = 72000L;

    public Scp902BoxItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static float age(Scp902Latch Held, @Nullable ClientLevel World) {
        if (World == null) {
            return Forgotten;
        }
        long Ticks = World.getGameTime() - Held.Since();
        if (Ticks < 0L || Ticks > Forgotten) {
            return Forgotten;
        }
        return Ticks + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static float angle(ItemStack Stack) {
        Scp902Latch Held = Scp902BoxItem.latch(Stack);
        return Scp902Lid.angle(Held.Open(), Held.Open() ? 0.0f : Scp902Lid.OpenAngle, age(Held, Minecraft.getInstance().level));
    }

    public static float property(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Bearer, int Seed) {
        Scp902Latch Held = Scp902BoxItem.latch(Stack);
        if (Held.Open()) {
            return 1.0f;
        }
        return age(Held, World == null ? Minecraft.getInstance().level : World) < Scp902Lid.CloseTicks ? 1.0f : 0.0f;
    }

    @Override
    public void renderByItem(ItemStack Stack, ItemDisplayContext Context, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        Scp902Models.draw(Pose, Buffers, Light, Overlay, angle(Stack));
    }
}

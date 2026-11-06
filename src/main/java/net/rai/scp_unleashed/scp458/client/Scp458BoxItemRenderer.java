package net.rai.scp_unleashed.scp458.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp458.Scp458BoxItem;
import net.rai.scp_unleashed.scp458.Scp458Contents;
import net.rai.scp_unleashed.scp458.Scp458Lid;
import org.jetbrains.annotations.Nullable;

public class Scp458BoxItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final long Forgotten = 72000L;

    public Scp458BoxItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static float age(Scp458Contents Held, @Nullable ClientLevel World) {
        if (World == null) {
            return Forgotten;
        }
        long Ticks = World.getGameTime() - Held.Since();
        if (Ticks < 0L || Ticks > Forgotten) {
            return Forgotten;
        }
        return Ticks + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static float age(Scp458Contents Held, long Now, float Partial) {
        long Ticks = Now - Held.Since();
        if (Ticks < 0L || Ticks > Forgotten) {
            return Forgotten;
        }
        return Ticks + Partial;
    }

    public static float angle(ItemStack Stack, long Now, float Partial) {
        Scp458Contents Held = Scp458BoxItem.contents(Stack);
        return Scp458Lid.angle(Held.Open(), Held.Open() ? 0.0f : Scp458Lid.OpenAngle, age(Held, Now, Partial));
    }

    public static float angle(ItemStack Stack) {
        Scp458Contents Held = Scp458BoxItem.contents(Stack);
        float Age = age(Held, Minecraft.getInstance().level);
        return Scp458Lid.angle(Held.Open(), Held.Open() ? 0.0f : Scp458Lid.OpenAngle, Age);
    }

    public static float property(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Bearer, int Seed) {
        Scp458Contents Held = Scp458BoxItem.contents(Stack);
        if (Held.Open()) {
            return 1.0f;
        }
        return age(Held, World == null ? Minecraft.getInstance().level : World) < Scp458Lid.CloseTicks ? 1.0f : 0.0f;
    }

    @Override
    public void renderByItem(ItemStack Stack, ItemDisplayContext Context, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        Scp458Contents Held = Scp458BoxItem.contents(Stack);
        Scp458Models.draw(Pose, Buffers, Light, Overlay, angle(Stack), Held.Pizza().orElse(null), Held.Taken());
    }
}

package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GunHandRenderer {
    public static final float OffHandHideAim = 0.05F;

    private GunHandRenderer() {
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null) {
            return;
        }
        float Partial = Event.getPartialTick();
        if (Event.getHand() == InteractionHand.OFF_HAND) {
            GunKind Held = GunItem.kindOf(User.getMainHandItem());
            if (Held != null && (Held.getStats().isLongGun() || GunClientState.aim(Partial) > OffHandHideAim)) {
                Event.setCanceled(true);
            }
            return;
        }
        ItemStack Stack = Event.getItemStack();
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null) {
            return;
        }
        Event.setCanceled(true);
        if (GunClientState.hidesGun(Partial)) {
            return;
        }
        HumanoidArm Arm = User.getMainArm();
        boolean Right = Arm == HumanoidArm.RIGHT;
        BakedModel Model = GunMuzzle.modelFor(User, Stack);
        PoseStack Pose = Event.getPoseStack();
        Pose.pushPose();
        GunPose.apply(Pose, Model, Stack, Kind, Arm, Partial, GunClientState.equipProgress(User, Stack, Event.getEquipProgress(), Partial));
        Game.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(User, Stack,
                Right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !Right, Pose, Event.getMultiBufferSource(), Event.getPackedLight());
        float Flash = GunClientState.localFlash(Partial);
        if (Flash > 0.0F && GunView.of(Kind).FlashSize() > 0.0F) {
            Model.applyTransform(Right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, Pose, !Right);
            Pose.translate(-0.5F, -0.5F, -0.5F);
            GunEffects.renderHandFlash(Pose, Event.getMultiBufferSource(), Kind, Flash, GunPose.scaleOf(Model, Arm));
        }
        Pose.popPose();
    }
}

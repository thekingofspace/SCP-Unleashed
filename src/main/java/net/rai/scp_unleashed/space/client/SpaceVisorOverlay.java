package net.rai.scp_unleashed.space.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.rai.scp_unleashed.gear.client.GearVisorLayout;
import net.rai.scp_unleashed.space.SpaceRegistry;
import net.rai.scp_unleashed.space.SpaceSuitItem;
import net.rai.scp_unleashed.space.SpaceSuitPiece;

public final class SpaceVisorOverlay {
    public static final ResourceLocation LayerId = SpaceRegistry.location("space_visor_overlay");
    public static final GearVisorLayout.Spec Helmet = new GearVisorLayout.Spec(SpaceRegistry.location("textures/gui/space/helmet_overlay.png"), 56, 48,
            new int[]{20, 16, 20}, new boolean[]{false, true, false}, new int[]{16, 16, 16}, new boolean[]{false, true, false});
    private static float Shade = 1.0F;

    private SpaceVisorOverlay() {
    }

    public static boolean wearsHelmet(LocalPlayer User) {
        return User.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SpaceSuitItem Worn && Worn.getPiece() == SpaceSuitPiece.Helmet;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.options.hideGui || !Client.options.getCameraType().isFirstPerson() || User.isScoping() || Client.getCameraEntity() != User || !wearsHelmet(User)) {
            return;
        }
        Window Display = Client.getWindow();
        float Scale = (float) Display.getGuiScale();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float Light = shade(User, Delta);
        Graphics.setColor(Light, Light, Light, 1.0F);
        for (GearVisorLayout.Blit Step : GearVisorLayout.plan(Helmet, Display.getWidth(), Display.getHeight())) {
            PoseStack Pose = Graphics.pose();
            Pose.pushPose();
            Pose.translate(Step.X() / Scale, Step.Y() / Scale, 0.0F);
            Pose.scale(Step.Width() / Scale / Step.RegionWidth(), Step.Height() / Scale / Step.RegionHeight(), 1.0F);
            Graphics.blit(Helmet.Texture(), 0, 0, Step.RegionWidth(), Step.RegionHeight(), Step.U(), Step.V(), Step.RegionWidth(), Step.RegionHeight(), Helmet.TextureWidth(), Helmet.TextureHeight());
            Pose.popPose();
        }
        Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static float shade(LocalPlayer User, DeltaTracker Delta) {
        float Target = 1.0F;
        if (!User.hasEffect(MobEffects.NIGHT_VISION)) {
            BlockPos Eye = BlockPos.containing(User.getX(), User.getEyeY(), User.getZ());
            float Brightness = LightTexture.getBrightness(User.level().dimensionType(), User.level().getMaxLocalRawBrightness(Eye));
            Target = 0.35F + 0.65F * (float) Math.sqrt(Brightness);
        }
        Shade += (Target - Shade) * Math.min(1.0F, Delta.getRealtimeDeltaTicks() * 0.2F);
        return Shade;
    }
}

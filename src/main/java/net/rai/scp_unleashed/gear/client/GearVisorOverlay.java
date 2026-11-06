package net.rai.scp_unleashed.gear.client;

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
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gear.GearArmorItem;
import net.rai.scp_unleashed.gear.GearPiece;
import org.jetbrains.annotations.Nullable;

public final class GearVisorOverlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gear_visor_overlay");
    private static final GearVisorLayout.Spec GasMask = new GearVisorLayout.Spec(texture("gas_mask"), 89, 54,
            new int[]{18, 16, 21, 16, 18}, new boolean[]{false, true, false, true, false}, new int[]{18, 16, 20}, new boolean[]{false, true, false});
    private static final GearVisorLayout.Spec HazmatHood = new GearVisorLayout.Spec(texture("hazmat_hood"), 48, 48,
            new int[]{16, 16, 16}, new boolean[]{false, true, false}, new int[]{16, 16, 16}, new boolean[]{false, true, false});
    private static final GearVisorLayout.Spec ChaosMask = new GearVisorLayout.Spec(texture("chaos_insurgency_helmet"), 89, 54,
            new int[]{18, 16, 21, 16, 18}, new boolean[]{false, true, false, true, false}, new int[]{18, 16, 20}, new boolean[]{false, true, false});
    private static float Shade = 1.0F;

    private GearVisorOverlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.options.hideGui || !Client.options.getCameraType().isFirstPerson() || User.isScoping() || Client.getCameraEntity() != User) {
            return;
        }
        if (!(User.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GearArmorItem Worn)) {
            return;
        }
        GearVisorLayout.Spec Layout = layoutFor(Worn.getPiece());
        if (Layout == null) {
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
        for (GearVisorLayout.Blit Step : GearVisorLayout.plan(Layout, Display.getWidth(), Display.getHeight())) {
            draw(Graphics, Layout, Step, Scale);
        }
        Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    @Nullable
    public static GearVisorLayout.Spec layoutFor(GearPiece Piece) {
        return switch (Piece) {
            case GasMask -> GasMask;
            case HazmatHood -> HazmatHood;
            case ChaosInsurgencyHelmet -> ChaosMask;
            default -> null;
        };
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

    private static void draw(GuiGraphics Graphics, GearVisorLayout.Spec Layout, GearVisorLayout.Blit Step, float Scale) {
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.translate(Step.X() / Scale, Step.Y() / Scale, 0.0F);
        Pose.scale(Step.Width() / Scale / Step.RegionWidth(), Step.Height() / Scale / Step.RegionHeight(), 1.0F);
        Graphics.blit(Layout.Texture(), 0, 0, Step.RegionWidth(), Step.RegionHeight(), Step.U(), Step.V(), Step.RegionWidth(), Step.RegionHeight(), Layout.TextureWidth(), Layout.TextureHeight());
        Pose.popPose();
    }

    private static ResourceLocation texture(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/gear/" + Name + "_overlay.png");
    }
}

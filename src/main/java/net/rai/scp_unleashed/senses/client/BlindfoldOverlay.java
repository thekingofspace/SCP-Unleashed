package net.rai.scp_unleashed.senses.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.senses.SensesRegistry;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class BlindfoldOverlay {
    public static final ResourceLocation LayerId = SensesRegistry.location("blindfold_overlay");
    public static final ResourceLocation Cloth = SensesRegistry.location("textures/gui/senses/blindfold_cloth.png");
    public static final ResourceLocation LeakId = SensesRegistry.location("senses_blindfold_leak");
    public static final ResourceLocation FoldId = SensesRegistry.location("senses_blindfold_folds");
    public static final int ClothSize = 54;
    public static final int TexelRows = 128;
    public static final int AmbientColour = 0xFFE4C8;
    public static final int LeakColour = 0xFFEED6;
    public static final int GlimmerColour = 0xFFC27A;
    public static final float FadeSpeed = 0.25f;
    public static final float EdgeGlow = 0.4f;

    private static float Cover;
    private static float Shade = 0.5f;
    private static DynamicTexture Leak;
    private static int LeakWidth;
    private static int LeakHeight;
    private static DynamicTexture Folds;
    private static int FoldWidth;
    private static int FoldHeight;

    private BlindfoldOverlay() {
    }

    public static boolean active(Minecraft Client) {
        LocalPlayer User = Client.player;
        return User != null && !User.isSpectator() && Client.getCameraEntity() == User && Senses.isBlindfolded(User);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        boolean Wanted = active(Client);
        float Step = Math.min(1.0f, Delta.getRealtimeDeltaTicks() * FadeSpeed);
        Cover = Wanted ? Math.min(1.0f, Cover + Step) : Math.max(0.0f, Cover - Step);
        if (Cover <= 0.0f || Client.player == null) {
            return;
        }
        Window Display = Client.getWindow();
        int Scale = texel(Display.getHeight());
        int Wide = Mth.ceil(Display.getWidth() / (float) Scale) + 1;
        int Tall = Mth.ceil(Display.getHeight() / (float) Scale) + 1;
        float Light = shade(Client.player, Delta);
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        float Factor = Scale / (float) Display.getGuiScale();
        Pose.scale(Factor, Factor, 1.0f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.fill(0, 0, Wide, Tall, colour(AmbientColour, Light * 0.55f, Cover));
        Graphics.flush();
        Graphics.setColor(rgb(LeakColour, 16, Light), rgb(LeakColour, 8, Light), rgb(LeakColour, 0, Light), Cover);
        Graphics.blit(leak(Wide, Tall), 0, 0, 0.0f, 0.0f, Wide, Tall, Wide, Tall);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        glimmers(Graphics, Client, Scale, Wide, Tall);
        Graphics.flush();
        float Tint = 0.3f + 0.7f * Light;
        Graphics.setColor(Tint, Tint, Tint, Cover);
        Graphics.blit(Cloth, 0, 0, 0.0f, 0.0f, Wide, Tall, ClothSize, ClothSize);
        Graphics.setColor(rgb(LeakColour, 16, Light), rgb(LeakColour, 8, Light), rgb(LeakColour, 0, Light), Cover * EdgeGlow);
        Graphics.blit(LeakId, 0, 0, 0.0f, 0.0f, Wide, Tall, Wide, Tall);
        Graphics.setColor(0.0f, 0.0f, 0.0f, Cover);
        Graphics.blit(folds(Wide, Tall), 0, 0, 0.0f, 0.0f, Wide, Tall, Wide, Tall);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        Graphics.flush();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        Pose.popPose();
    }

    public static int texel(int ScreenHeight) {
        return Math.max(2, Math.round(ScreenHeight / (float) TexelRows));
    }

    private static void glimmers(GuiGraphics Graphics, Minecraft Client, int Scale, int Wide, int Tall) {
        List<BlindfoldGlimmers.Spot> Spots = BlindfoldGlimmers.spots();
        if (Spots.isEmpty()) {
            return;
        }
        Camera View = Client.gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        Quaternionf Inverse = new Quaternionf(View.rotation()).conjugate();
        Window Display = Client.getWindow();
        double TanHalf = Math.tan(Math.toRadians(BlindfoldGlimmers.fov()) / 2.0);
        double Aspect = Display.getWidth() / (double) Display.getHeight();
        for (BlindfoldGlimmers.Spot Found : Spots) {
            Vector3f Local = new Vector3f((float) (Found.Centre().x - Eye.x), (float) (Found.Centre().y - Eye.y), (float) (Found.Centre().z - Eye.z)).rotate(Inverse);
            if (Local.z > -0.2f) {
                continue;
            }
            double Depth = -Local.z;
            double NdcX = Local.x / (Depth * TanHalf * Aspect);
            double NdcY = Local.y / (Depth * TanHalf);
            if (Math.abs(NdcX) > 1.4 || Math.abs(NdcY) > 1.4) {
                continue;
            }
            int CentreX = (int) Math.round((NdcX + 1.0) / 2.0 * Display.getWidth() / Scale);
            int CentreY = (int) Math.round((1.0 - NdcY) / 2.0 * Display.getHeight() / Scale);
            double Distance = Math.sqrt(Local.lengthSquared());
            float Strength = Found.Emission() / 15.0f * (float) Mth.clamp(1.6 / Math.max(1.0, Distance * 0.45), 0.08, 1.0);
            int Radius = Mth.clamp(Math.round(2.0f + 9.0f * Strength), 2, 11);
            blob(Graphics, CentreX, CentreY, Radius, Strength * 0.35f * Cover, Wide, Tall);
        }
    }

    private static void blob(GuiGraphics Graphics, int CentreX, int CentreY, int Radius, float Strength, int Wide, int Tall) {
        if (CentreX + Radius < 0 || CentreY + Radius < 0 || CentreX - Radius > Wide || CentreY - Radius > Tall) {
            return;
        }
        int Rings = 3;
        for (int Ring = 0; Ring < Rings; Ring++) {
            int Reach = Math.max(1, Radius - Ring * Radius / Rings);
            int Colour = colour(GlimmerColour, 1.0f, Strength / Rings);
            for (int Row = -Reach; Row <= Reach; Row++) {
                int Half = (int) Math.floor(Math.sqrt(Reach * Reach - Row * Row + 0.5));
                Graphics.fill(CentreX - Half, CentreY + Row, CentreX + Half + 1, CentreY + Row + 1, Colour);
            }
        }
    }

    private static float shade(LocalPlayer User, DeltaTracker Delta) {
        float Target = 1.0f;
        if (!User.hasEffect(MobEffects.NIGHT_VISION)) {
            BlockPos Eye = BlockPos.containing(User.getX(), User.getEyeY(), User.getZ());
            Target = LightTexture.getBrightness(User.level().dimensionType(), User.level().getMaxLocalRawBrightness(Eye));
        }
        Shade += (Target - Shade) * Math.min(1.0f, Delta.getRealtimeDeltaTicks() * 0.1f);
        return Shade;
    }

    private static ResourceLocation leak(int Wide, int Tall) {
        if (Leak == null || Wide != LeakWidth || Tall != LeakHeight) {
            NativeImage Image = new NativeImage(Wide, Tall, true);
            for (int Y = 0; Y < Tall; Y++) {
                for (int X = 0; X < Wide; X++) {
                    int Alpha = Math.round(BlindfoldLeak.at(X, Y, Wide, Tall) * 255.0f);
                    Image.setPixelRGBA(X, Y, Alpha << 24 | 0xFFFFFF);
                }
            }
            if (Leak != null) {
                Minecraft.getInstance().getTextureManager().release(LeakId);
            }
            Leak = new DynamicTexture(Image);
            Minecraft.getInstance().getTextureManager().register(LeakId, Leak);
            LeakWidth = Wide;
            LeakHeight = Tall;
        }
        return LeakId;
    }

    private static ResourceLocation folds(int Wide, int Tall) {
        if (Folds == null || Wide != FoldWidth || Tall != FoldHeight) {
            NativeImage Image = new NativeImage(Wide, Tall, true);
            for (int Y = 0; Y < Tall; Y++) {
                for (int X = 0; X < Wide; X++) {
                    int Alpha = Math.round(BlindfoldLeak.shade(X, Y, Wide, Tall) * 255.0f);
                    Image.setPixelRGBA(X, Y, Alpha << 24 | 0xFFFFFF);
                }
            }
            if (Folds != null) {
                Minecraft.getInstance().getTextureManager().release(FoldId);
            }
            Folds = new DynamicTexture(Image);
            Minecraft.getInstance().getTextureManager().register(FoldId, Folds);
            FoldWidth = Wide;
            FoldHeight = Tall;
        }
        return FoldId;
    }

    private static int colour(int Rgb, float Level, float Alpha) {
        int Red = Math.round((Rgb >> 16 & 255) * Mth.clamp(Level, 0.0f, 1.0f));
        int Green = Math.round((Rgb >> 8 & 255) * Mth.clamp(Level, 0.0f, 1.0f));
        int Blue = Math.round((Rgb & 255) * Mth.clamp(Level, 0.0f, 1.0f));
        int Opacity = Math.round(255.0f * Mth.clamp(Alpha, 0.0f, 1.0f));
        return Opacity << 24 | Red << 16 | Green << 8 | Blue;
    }

    private static float rgb(int Rgb, int Shift, float Level) {
        return (Rgb >> Shift & 255) / 255.0f * Level;
    }
}

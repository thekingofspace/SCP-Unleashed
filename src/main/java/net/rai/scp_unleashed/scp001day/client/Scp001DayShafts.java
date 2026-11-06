package net.rai.scp_unleashed.scp001day.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.scp001day.Scp001DayClientConfig;
import net.rai.scp_unleashed.scp001day.Scp001DayWorld;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayRay;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.SunVector;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp001DayShafts {
    public static final ResourceLocation ShaftProgram = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001day/sun_shafts");
    public static final ResourceLocation VolumeProgram = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001day/sun_volume");
    public static final ResourceLocation CompositeProgram = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001day/sun_composite");
    public static final String Prefix = "message.scp_unleashed.scp001_day.sunlight.";
    public static final float Scale = 0.5f;
    public static final int Samples = 48;
    public static final float FogDistance = 40.0f;
    public static final float Density = 0.3f;
    public static final float Extinction = 0.012f;
    public static final float Anisotropy = 0.35f;
    public static final float SurfaceBoost = 2.2f;
    public static final float SurfaceLift = 0.1f;
    public static final float ShaftGain = 0.6f;
    public static final float GlowCurve = 1.2f;
    public static final float SunRed = 1.0f;
    public static final float SunGreen = 0.92f;
    public static final float SunBlue = 0.74f;
    public static final float ShadeRed = 0.74f;
    public static final float ShadeGreen = 0.8f;
    public static final float ShadeBlue = 0.92f;
    public static final float Golden = 0.618034f;
    public static final float LitThreshold = 0.5f;

    private static final Logger Log = LogUtils.getLogger();
    private static final Set<String> Logged = new HashSet<>();
    private static boolean Forced;
    private static AdvancedFbo Target;
    private static int Frame;
    private static String Reason = "outside";
    private static Object[] Details = new Object[0];
    private static boolean Bound = true;
    private static long Frames;
    private static boolean Measure;

    private Scp001DayShafts() {
    }

    public static void force(boolean Value) {
        Forced = Value;
    }

    public static boolean active(ClientLevel World) {
        return World != null && (Forced || Scp001DayWorld.inside(World)) && !ShaderCompat.skipPostEffects();
    }

    public static String reason() {
        return Reason;
    }

    public static long frames() {
        return Frames;
    }

    public static boolean bound() {
        return Bound;
    }

    public static Component status() {
        return Component.translatable(Prefix + "reason." + Reason, Details);
    }

    public static void measureNextFrame() {
        Measure = true;
    }

    private static void note(String Key, Object... Values) {
        Reason = Key;
        Details = Values;
        if (Logged.add(Key)) {
            Log.info("SCP-001 sunlight renderer: {} {} (grid {}% built, {}% of grid columns loaded, volume {}% built, programs bound {}, frames drawn {})", Key,
                    Arrays.toString(Values), Scp001DaySunGrid.progress(), Scp001DaySunGrid.loaded(), Scp001DaySunVolume.progress(), Bound, Frames);
        }
    }

    public static String offReason() {
        if (ShaderCompat.forced()) {
            return "forced_fallback";
        }
        if (ShaderCompat.shaderPackInUse()) {
            return "shader_pack";
        }
        if (!ScpClientConfig.postEffects()) {
            return "post_effects_off";
        }
        if (!Scp001DayClientConfig.sunShafts()) {
            return "shafts_off";
        }
        return null;
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || !(Forced || Scp001DayWorld.inside(World))) {
            Reason = "outside";
            Details = new Object[0];
            return;
        }
        String Off = offReason();
        if (Off != null) {
            if (holding()) {
                release();
            }
            stop(Off);
            return;
        }
        render(Client, World, Event.getCamera());
    }

    public static boolean holding() {
        return Target != null || Scp001DaySunGrid.holding() || Scp001DaySunVolume.holding() || Scp001DayFarBoxes.texture() != 0;
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload Event) {
        if (Event.getLevel().isClientSide()) {
            RenderSystem.recordRenderCall(Scp001DayShafts::release);
        }
    }

    public static boolean pass(ShaderProgram Program, Runnable Draw) {
        Program.bind();
        Program.bindSamplers(0);
        boolean Ok = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM) == Program.getProgram();
        if (Ok) {
            Draw.run();
        }
        ShaderProgram.unbind();
        return Ok;
    }

    private static void stop(String Key, Object... Values) {
        note(Key, Values);
        if (Measure) {
            Measure = false;
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable(Prefix + "unmeasured", status()));
        }
    }

    private static void render(Minecraft Client, ClientLevel World, Camera View) {
        SunVector Sun = Scp001DaySun.direction(World.getDayTime());
        if (!Scp001DaySun.lethal(Sun)) {
            stop("night");
            return;
        }
        Vec3 Eye = View.getPosition();
        Scp001DaySunGrid.update(World, Eye, Sun);
        if (!Scp001DaySunGrid.ready()) {
            stop("grid_building", Scp001DaySunGrid.progress());
            return;
        }
        ShaderProgram Volume = VeilRenderSystem.setShader(VolumeProgram);
        ShaderProgram Shafts = VeilRenderSystem.setShader(ShaftProgram);
        ShaderProgram Composite = VeilRenderSystem.setShader(CompositeProgram);
        if (Volume == null || Shafts == null || Composite == null) {
            stop("program_missing", (Volume == null ? VolumeProgram : Shafts == null ? ShaftProgram : CompositeProgram).toString());
            return;
        }
        RenderTarget Main = Client.getMainRenderTarget();
        int Width = Math.max(1, (int) (Main.width * Scale));
        int Height = Math.max(1, (int) (Main.height * Scale));
        if (Target == null || Target.getWidth() != Width || Target.getHeight() != Height) {
            if (Target != null) {
                Target.free();
            }
            Target = AdvancedFbo.withSize(Width, Height).setFormat(FramebufferAttachmentDefinition.Format.RGBA16F).setFilter(true, false).addColorTextureBuffer().build(true);
        }
        boolean Far = Scp001DayWorld.inside(World);
        Scp001DayFarBoxes.update(Eye.x, Eye.z, Scp001DaySunGrid.originX(), Scp001DaySunGrid.originY(), Scp001DaySunGrid.originZ());
        int MaxY = World.getMaxBuildHeight();
        Consumer<ShaderProgram> Bind = Program -> bindTrace(Program, Sun, MaxY, Far);
        Bound = Scp001DaySunVolume.update(Eye, Scp001DaySun.epoch(World.getDayTime()), Scp001DaySunGrid.version(), Volume, Bind, Scp001DaySunGrid.originX(),
                Scp001DaySunGrid.originY(), Scp001DaySunGrid.originZ());
        Main.bindWrite(true);
        if (!Bound) {
            stop("unbound", VolumeProgram.toString());
            return;
        }
        if (!Scp001DaySunVolume.ready()) {
            stop("volume_building", Scp001DaySunVolume.progress());
            return;
        }
        Bind.accept(Shafts);
        Shafts.setTexture("DepthSampler", GL11C.GL_TEXTURE_2D, Main.getDepthTextureId());
        Shafts.setTexture("SunVolume", GL12C.GL_TEXTURE_3D, Scp001DaySunVolume.texture());
        Shafts.getUniformSafe("SunVolumeOrigin").setVector((float) (Scp001DaySunVolume.originX() - Scp001DaySunGrid.originX()), (float) (Scp001DaySunVolume.originY() - Scp001DaySunGrid.originY()),
                (float) (Scp001DaySunVolume.originZ() - Scp001DaySunGrid.originZ()));
        Shafts.getUniformSafe("SunVolumeExtent").setVector(Scp001DaySunVolume.extentX(), Scp001DaySunVolume.extentY(), Scp001DaySunVolume.extentZ());
        Shafts.getUniformSafe("SunTime").setFloat((World.getGameTime() % 24000L) / 20.0f);
        Shafts.getUniformSafe("SunSamples").setInt(Samples);
        Shafts.getUniformSafe("SunFogDistance").setFloat(FogDistance);
        Shafts.getUniformSafe("SunDensity").setFloat(Density);
        Shafts.getUniformSafe("SunExtinction").setFloat(Extinction);
        Shafts.getUniformSafe("SunAnisotropy").setFloat(Anisotropy);
        Shafts.getUniformSafe("CameraLocal").setVector((float) (Eye.x - Scp001DaySunGrid.originX()), (float) (Eye.y - Scp001DaySunGrid.originY()), (float) (Eye.z - Scp001DaySunGrid.originZ()));
        Frame = (Frame + 1) & 1023;
        Shafts.getUniformSafe("FrameJitter").setFloat((Frame * Golden) % 1.0f);
        Composite.setTexture("ShaftSampler", GL11C.GL_TEXTURE_2D, Target.getColorTextureAttachment(0).getId());
        Composite.getUniformSafe("SunColour").setVector(SunRed, SunGreen, SunBlue);
        Composite.getUniformSafe("ShadeTint").setVector(ShadeRed, ShadeGreen, ShadeBlue);
        Composite.getUniformSafe("SurfaceBoost").setFloat(SurfaceBoost);
        Composite.getUniformSafe("SurfaceLift").setFloat(SurfaceLift);
        Composite.getUniformSafe("ShaftGain").setFloat(ShaftGain);
        Composite.getUniformSafe("GlowCurve").setFloat(GlowCurve);
        Composite.getUniformSafe("Strength").setFloat((float) Scp001DaySun.intensity(Sun));
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        Target.bind(true);
        Target.clear(0.0f, 0.0f, 0.0f, 0.0f, GL11C.GL_COLOR_BUFFER_BIT);
        Bound = pass(Shafts, VeilRenderSystem::drawScreenQuad);
        String Measured = Bound && Measure ? measure() : null;
        Main.bindWrite(true);
        if (Bound) {
            Bound = pass(Composite, () -> {
                RenderSystem.enableBlend();
                Composite.getUniformSafe("Mode").setInt(2);
                RenderSystem.blendFuncSeparate(GL11C.GL_DST_COLOR, GL11C.GL_ZERO, GL11C.GL_ZERO, GL11C.GL_ONE);
                VeilRenderSystem.drawScreenQuad();
                Composite.getUniformSafe("Mode").setInt(0);
                RenderSystem.blendFuncSeparate(GL11C.GL_DST_COLOR, GL11C.GL_ONE, GL11C.GL_ZERO, GL11C.GL_ONE);
                VeilRenderSystem.drawScreenQuad();
                Composite.getUniformSafe("Mode").setInt(1);
                RenderSystem.blendFuncSeparate(GL11C.GL_ONE, GL11C.GL_ONE, GL11C.GL_ZERO, GL11C.GL_ONE);
                VeilRenderSystem.drawScreenQuad();
                GL14C.glBlendEquation(GL14C.GL_FUNC_ADD);
            });
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        if (!Bound) {
            stop("unbound", (Measured == null ? ShaftProgram : CompositeProgram).toString());
            return;
        }
        Frames++;
        note("running");
        if (Measured != null) {
            Client.gui.getChat().addMessage(Component.translatable(Prefix + "measure", Measured));
        }
    }

    private static String measure() {
        Measure = false;
        int Width = Target.getWidth();
        int Height = Target.getHeight();
        FloatBuffer Pixels = MemoryUtil.memAllocFloat(Width * Height * 4);
        try {
            Scp001DayGl.packed(() -> GL11C.glReadPixels(0, 0, Width, Height, GL11C.GL_RGBA, GL11C.GL_FLOAT, Pixels));
            int Surfaces = 0;
            int Lit = 0;
            float Glow = 0.0f;
            for (int Index = 0; Index < Width * Height; Index++) {
                Glow = Math.max(Glow, Pixels.get(Index * 4));
                if (Pixels.get(Index * 4 + 3) > 0.5f) {
                    Surfaces++;
                    if (Pixels.get(Index * 4 + 1) > LitThreshold) {
                        Lit++;
                    }
                }
            }
            int Share = Surfaces == 0 ? 0 : Math.round(100.0f * Lit / Surfaces);
            return String.format(Locale.ROOT, "%d%% / %.2f / %dx%d", Share, Glow, Width, Height);
        } finally {
            MemoryUtil.memFree(Pixels);
        }
    }

    public static void bindTrace(ShaderProgram Program, SunVector Sun, int MaxY, boolean Far) {
        Program.setTexture("SunGrid", GL12C.GL_TEXTURE_3D, Scp001DaySunGrid.gridTexture());
        Program.setTexture("SunCoarse", GL12C.GL_TEXTURE_3D, Scp001DaySunGrid.coarseTexture());
        Program.setTexture("SunFarBoxes", GL11C.GL_TEXTURE_2D, Scp001DayFarBoxes.texture());
        Program.getUniformSafe("SunGridSize").setInt(Scp001DaySunGrid.Span);
        Program.getUniformSafe("SunDirection").setVector((float) Sun.X(), (float) Sun.Y(), (float) Sun.Z());
        Program.getUniformSafe("SunSkyLocal").setFloat((float) (MaxY - Scp001DaySunGrid.originY()));
        Program.getUniformSafe("SunReach").setFloat((float) Scp001DayRay.NearReach);
        Program.getUniformSafe("SunFarEnabled").setInt(Far ? 1 : 0);
        Program.getUniformSafe("SunFarSpan").setInt(Scp001DayFarBoxes.Span);
        Program.getUniformSafe("SunFarFirst").setInts(Scp001DayFarBoxes.firstX(), Scp001DayFarBoxes.firstZ());
        Program.getUniformSafe("SunFarShift").setVector(Scp001DayFarBoxes.shiftX(Scp001DaySunGrid.originX()), Scp001DayFarBoxes.shiftZ(Scp001DaySunGrid.originZ()));
        Program.getUniformSafe("SunFarTop").setFloat((float) (Scp001DayPlan.farTop() - Scp001DaySunGrid.originY()));
    }

    private static void release() {
        Scp001DaySunVolume.release();
        Scp001DayFarBoxes.release();
        Scp001DaySunGrid.release();
        if (Target != null) {
            Target.free();
            Target = null;
        }
    }
}

package net.rai.scp_unleashed.rift.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RiftDistortion {
    public static final ResourceLocation Pipeline = VeilPost.id("rift_distortion");
    public static final double ScanReach = 16.0;

    private static float Strength;
    private static float StrengthOld;
    private static float Flash;
    private static float FlashOld;
    private static Vec3 Focus = Vec3.ZERO;
    private static float FocusHalf = 1.0f;
    private static int FocusColour = 0xFFFFFF;
    private static ClientLevel LastWorld;

    private RiftDistortion() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World != LastWorld) {
            reset();
            LastWorld = World;
        }
        if (World == null || Client.isPaused()) {
            return;
        }
        StrengthOld = Strength;
        FlashOld = Flash;
        Vec3 Eye = Client.gameRenderer.getMainCamera().getPosition();
        float Best = 0.0f;
        for (RiftEntity Rift : World.getEntitiesOfClass(RiftEntity.class, new AABB(Eye, Eye).inflate(ScanReach), Candidate -> !Candidate.isRemoved())) {
            int Tier = Rift.getRarity().getTier();
            Vec3 Center = Rift.getCenter(1.0f);
            float Near = 1.0f - (float) Center.distanceTo(Eye) / (6.0f + 1.2f * Tier);
            if (Near <= 0.0f) {
                continue;
            }
            float Influence = Near * Near * (3.0f - 2.0f * Near) * (0.4f + 0.15f * Tier) * Rift.getOpenness(1.0f)
                    * (1.0f + 0.6f * Rift.getShownExtraction(1.0f) + 0.6f * Rift.getCollapse(1.0f));
            if (Influence > Best && Client.player != null && hidden(World, Eye, Center, Rift.getRiftHeight() * 0.5, Client.player)) {
                Influence *= 0.35f;
            }
            if (Influence > Best) {
                Best = Influence;
                Focus = Center;
                FocusHalf = Rift.getRiftHeight() * 0.5f;
                FocusColour = Rift.getRiftColour();
            }
        }
        float Target = Mth.clamp(Best, 0.0f, 1.0f) * Client.options.screenEffectScale().get().floatValue();
        Strength += (Target - Strength) * 0.15f;
        if (Target <= 0.0f && Strength < 0.003f) {
            Strength = 0.0f;
        }
        Flash *= 0.78f;
        if (Flash < 0.003f) {
            Flash = 0.0f;
        }
    }

    private static boolean hidden(ClientLevel World, Vec3 Eye, Vec3 Center, double Half, Entity Viewer) {
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Center, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer));
        return Hit.getType() == HitResult.Type.BLOCK && Hit.getLocation().distanceToSqr(Center) > Half * Half;
    }

    public static void flash(RiftEntity Rift) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Rift.level() != Client.level || Client.options.hideLightningFlash().get()) {
            return;
        }
        int Tier = Rift.getRarity().getTier();
        Vec3 Center = Rift.getCenter(1.0f);
        double Distance = Center.distanceTo(Client.gameRenderer.getMainCamera().getPosition());
        float Power = (float) Mth.clamp(1.0 - Distance / (14.0 + 2.0 * Tier), 0.0, 1.0) * (0.5f + 0.1f * Tier);
        if (Power > Flash) {
            Flash = Power;
            Focus = Center;
            FocusHalf = Rift.getRiftHeight() * 0.5f;
            FocusColour = Rift.getRiftColour();
        }
    }

    public static double beat(double Seconds) {
        double Phase = Seconds * 0.8 - Math.floor(Seconds * 0.8);
        double First = Math.exp(-Math.pow((Phase - 0.08) / 0.06, 2.0));
        double Second = Math.exp(-Math.pow((Phase - 0.3) / 0.07, 2.0)) * 0.6;
        return First + Second;
    }

    private static double seconds(ClientLevel World, double PartialTick) {
        return (World.getGameTime() % 24000L + PartialTick) / 20.0;
    }

    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Event.usedConfiguredFov() || Client.level == null) {
            return;
        }
        float Shown = Mth.lerp((float) Event.getPartialTick(), StrengthOld, Strength);
        if (Shown <= 0.001f) {
            return;
        }
        double Scale = Client.options.fovEffectScale().get();
        Event.setFOV(Event.getFOV() * (1.0 - 0.04 * Shown * beat(seconds(Client.level, Event.getPartialTick())) * Scale));
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !VeilPost.available()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float Shown = Mth.lerp(Partial, StrengthOld, Strength);
        float Burst = Mth.lerp(Partial, FlashOld, Flash);
        if (Client.level == null || Shown < 0.003f && Burst < 0.003f) {
            return;
        }
        Camera View = Event.getCamera();
        Vec3 Eye = View.getPosition();
        Matrix4f Projection = Event.getProjectionMatrix();
        Vector4f Clip = new Vector4f((float) (Focus.x - Eye.x), (float) (Focus.y - Eye.y), (float) (Focus.z - Eye.z), 1.0f);
        Event.getModelViewMatrix().transform(Clip);
        Projection.transform(Clip);
        float Distance = (float) Focus.distanceTo(Eye);
        float Facing = Mth.clamp(Clip.w / Math.max(Distance, 0.1f) * 3.0f, 0.0f, 1.0f);
        float CenterX = 0.5f;
        float CenterY = 0.5f;
        float Radius = 0.1f;
        float Depth = 0.0f;
        if (Clip.w > 0.05f) {
            CenterX = Mth.clamp(Clip.x / Clip.w * 0.5f + 0.5f, -3.0f, 4.0f);
            CenterY = Mth.clamp(Clip.y / Clip.w * 0.5f + 0.5f, -3.0f, 4.0f);
            Radius = Mth.clamp(Projection.m11() * FocusHalf / Clip.w * 0.5f, 0.01f, 3.0f);
            Depth = Clip.w;
        }
        double Seconds = seconds(Client.level, Partial);
        float AtX = CenterX;
        float AtY = CenterY;
        float Size = Radius;
        float Behind = Depth;
        VeilPost.run(Pipeline, Values -> Values.set("Strength", Shown)
                .set("CenterX", AtX)
                .set("CenterY", AtY)
                .set("Radius", Size)
                .set("Seconds", (float) Seconds)
                .set("Beat", (float) beat(Seconds))
                .set("Flash", Burst)
                .set("TintR", ((FocusColour >> 16) & 255) / 255.0f)
                .set("TintG", ((FocusColour >> 8) & 255) / 255.0f)
                .set("TintB", (FocusColour & 255) / 255.0f)
                .set("Facing", Facing)
                .set("FocusDepth", Behind)
                .set("FocusHalf", FocusHalf)
                .set("ProjA", Projection.m22())
                .set("ProjB", Projection.m32()));
    }

    public static void reset() {
        Strength = 0.0f;
        StrengthOld = 0.0f;
        Flash = 0.0f;
        FlashOld = 0.0f;
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        reset();
        LastWorld = null;
    }
}

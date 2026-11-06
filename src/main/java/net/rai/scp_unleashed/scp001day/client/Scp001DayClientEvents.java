package net.rai.scp_unleashed.scp001day.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.scp001day.Scp001DaySunlight;
import net.rai.scp_unleashed.scp001day.item.Scp001DayWrapItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.Scp001DayWorld;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayLevelField;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayRay;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.SunVector;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayExposure;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayFarField;
import net.rai.scp_unleashed.scp001day.world.Scp001DayFar;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp001DayClientEvents {
    public static final int MoteTries = 6;
    public static final double MoteRadius = 9.0;
    public static final float RollSwing = 3.0f;
    public static final float RollGrowth = 9.0f;
    public static final float LookSwing = 2.0f;
    public static final float FovSwing = 0.08f;
    private static final double[] Points = new double[Scp001DayExposure.Samples];
    private static boolean DrawingArm;

    private Scp001DayClientEvents() {
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp001DayRegistry.location("scp001_day"), new Scp001DayEffects());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(Scp001DayRegistry.Tin.get(), Scp001DayRegistry.location("opened"),
                (Stack, World, Holder, Seed) -> Holder != null && Holder.isUsingItem() && Holder.getUseItem() == Stack ? 1.0f : 0.0f));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp001DayRegistry.RiftType.get(), Scp001DayRiftRenderer::new);
        Event.registerEntityRenderer(Scp001DayRegistry.CrawlerType.get(), Context -> new Scp001DayFleshRenderer(Context, Scp001DayForm.Crawler));
        Event.registerEntityRenderer(Scp001DayRegistry.ClingerType.get(), Context -> new Scp001DayFleshRenderer(Context, Scp001DayForm.Clinger));
        Event.registerEntityRenderer(Scp001DayRegistry.LurkerType.get(), Context -> new Scp001DayFleshRenderer(Context, Scp001DayForm.Lurker));
        Event.registerEntityRenderer(Scp001DayRegistry.MassType.get(), Context -> new Scp001DayFleshRenderer(Context, Scp001DayForm.Mass));
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp001DayOverlay.LayerId, Scp001DayOverlay::render);
    }

    @SubscribeEvent
    public static void registerShells(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(Scp001DayTurningLayer.Wide, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(Scp001DayTurningLayer.Inflate), false), 64, 64));
        Event.registerLayerDefinition(Scp001DayTurningLayer.Slim, () -> LayerDefinition.create(PlayerModel.createMesh(new CubeDeformation(Scp001DayTurningLayer.Inflate), true), 64, 64));
    }

    @SubscribeEvent
    public static void addShells(EntityRenderersEvent.AddLayers Event) {
        for (PlayerSkin.Model Kind : Event.getSkins()) {
            if (Event.getSkin(Kind) instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new Scp001DayTurningLayer(Renderer, Event.getEntityModels(), Kind == PlayerSkin.Model.SLIM));
            }
        }
    }

    @SubscribeEvent
    public static void renderArm(RenderArmEvent Event) {
        if (DrawingArm) {
            return;
        }
        AbstractClientPlayer Player = Event.getPlayer();
        float Turn = Scp001DayClientState.turning(Player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (Turn < 0.0f || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(Player) instanceof PlayerRenderer Renderer)) {
            return;
        }
        Event.setCanceled(true);
        DrawingArm = true;
        try {
            if (Event.getArm() == HumanoidArm.RIGHT) {
                Renderer.renderRightHand(Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight(), Player);
            } else {
                Renderer.renderLeftHand(Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight(), Player);
            }
        } finally {
            DrawingArm = false;
        }
        Scp001DayTurningLayer.arm(Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight(), Player, Event.getArm(), Turn);
    }

    @SubscribeEvent
    public static void sway(ViewportEvent.ComputeCameraAngles Event) {
        Minecraft Client = Minecraft.getInstance();
        float Turn = Scp001DayClientState.turning(Client.player, (float) Event.getPartialTick());
        if (Turn < 0.0f || Client.level == null) {
            return;
        }
        double Time = Client.level.getGameTime() + Event.getPartialTick();
        Event.setRoll(Event.getRoll() + (float) Math.sin(Time * 0.13) * (RollSwing + RollGrowth * Turn) * Turn);
        Event.setPitch(Event.getPitch() + (float) Math.sin(Time * 0.071) * LookSwing * Turn);
        Event.setYaw(Event.getYaw() + (float) Math.sin(Time * 0.053) * LookSwing * Turn);
    }

    @SubscribeEvent
    public static void pulse(ViewportEvent.ComputeFov Event) {
        Minecraft Client = Minecraft.getInstance();
        float Turn = Scp001DayClientState.turning(Client.player, (float) Event.getPartialTick());
        if (Turn < 0.0f || Client.level == null) {
            return;
        }
        double Time = Client.level.getGameTime() + Event.getPartialTick();
        Event.setFOV(Event.getFOV() * (1.0 + FovSwing * Turn * Math.sin(Time * 0.21)));
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (Event.getLevel().isClientSide() && Event.getLevel() instanceof ClientLevel World && Scp001DayWorld.inside(World)) {
            Scp001DaySunGrid.invalidate(Event.getChunk().getPos().x, Event.getChunk().getPos().z);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Client.player == null) {
            Scp001DayClientState.reset();
            return;
        }
        if (!Scp001DayWorld.inside(World)) {
            Scp001DayClientState.leave();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        if (World.getGameTime() % Scp001DaySunlight.CheckTicks == 0L) {
            SunVector Sun = Scp001DaySun.direction(World.getDayTime());
            boolean Lethal = Scp001DaySun.lethal(Sun);
            Scp001DayFarField Far = Scp001DayWorld.strict(World) ? Scp001DayFar.City : Scp001DayFarField.Open;
            int Lit = 0;
            float Exposure = 0.0f;
            if (Lethal) {
                Scp001DayExposure.samples(World, Far, Client.player, Sun, Points);
                double Strength = Scp001DaySun.intensity(Sun);
                double Total = 0.0;
                for (int Index = 0; Index < Points.length; Index++) {
                    if (Scp001DayWrapItem.shields(Client.player, Scp001DaySunlight.Cover[Index])) {
                        continue;
                    }
                    Total += Points[Index];
                    if (Points[Index] * Strength >= Scp001DaySunlight.LitPoint) {
                        Lit++;
                    }
                }
                Exposure = (float) (Strength * Total / Points.length);
            }
            Scp001DayClientState.local(Exposure, Lit, Lethal);
        }
        motes(World, Client.player.getEyePosition(), World.random);
    }

    public static void motes(ClientLevel World, Vec3 Eye, RandomSource Random) {
        SunVector Sun = Scp001DaySun.direction(World.getDayTime());
        if (!Scp001DaySun.lethal(Sun)) {
            return;
        }
        Scp001DayLevelField Cells = new Scp001DayLevelField(World);
        for (int Try = 0; Try < MoteTries; Try++) {
            double X = Eye.x + (Random.nextDouble() * 2.0 - 1.0) * MoteRadius;
            double Y = Eye.y + (Random.nextDouble() * 2.0 - 1.2) * MoteRadius * 0.6;
            double Z = Eye.z + (Random.nextDouble() * 2.0 - 1.0) * MoteRadius;
            double Lit = Scp001DayRay.transmit(Cells, Scp001DayFarField.Open, X, Y, Z, Sun, World.getMaxBuildHeight());
            if (Lit > 0.2) {
                World.addParticle(ParticleTypes.WHITE_ASH, X, Y, Z, (Random.nextDouble() - 0.5) * 0.004, -0.002, (Random.nextDouble() - 0.5) * 0.004);
            }
        }
    }
}

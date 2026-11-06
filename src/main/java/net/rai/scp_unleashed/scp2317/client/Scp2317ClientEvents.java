package net.rai.scp_unleashed.scp2317.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp2317.Scp2317Plan;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import net.rai.scp_unleashed.scp2317.Scp2317World;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp2317ClientEvents {
    public static final float FogReach = 150.0f;
    public static final float FogStartShare = 0.2f;
    public static final int DustEvery = 3;
    public static final double DustReach = 14.0;

    private Scp2317ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp2317Registry.DoorEntity.get(), Scp2317DoorRenderer::new);
        Event.registerBlockEntityRenderer(Scp2317Registry.KingEntity.get(), Scp2317KingRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(Scp2317DoorRenderer.LeftLeaf);
        Event.register(Scp2317DoorRenderer.RightLeaf);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp2317Registry.location("scp2317_desert"), new Scp2317DesertEffects());
    }

    public static boolean inDesert(Minecraft Client) {
        return Client.level != null && Client.level.dimension() == Scp2317World.Key;
    }

    public static boolean above(Minecraft Client) {
        return Client.player != null && Client.player.getY() > Scp2317Plan.Ground - 8;
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!inDesert(Client) || !above(Client)) {
            return;
        }
        float Mix = 0.6f;
        Event.setRed(Event.getRed() * (1.0f - Mix) + (float) Scp2317DesertEffects.Haze.x * Mix);
        Event.setGreen(Event.getGreen() * (1.0f - Mix) + (float) Scp2317DesertEffects.Haze.y * Mix);
        Event.setBlue(Event.getBlue() * (1.0f - Mix) + (float) Scp2317DesertEffects.Haze.z * Mix);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!inDesert(Client) || !above(Client) || Client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float Far = Math.min(Event.getFarPlaneDistance(), FogReach);
        Event.setNearPlaneDistance(Far * FogStartShare);
        Event.setFarPlaneDistance(Far);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.level == null || Client.isPaused() || !inDesert(Client) || User.tickCount % DustEvery != 0) {
            return;
        }
        if (User.getY() > Scp2317Plan.CaveY + Scp2317Plan.CaveHalf || User.getY() < Scp2317Plan.CaveFloor) {
            return;
        }
        RandomSource Random = Client.level.random;
        double X = User.getX() + (Random.nextDouble() - 0.5) * DustReach * 2.0;
        double Z = User.getZ() + (Random.nextDouble() - 0.5) * DustReach * 2.0;
        double Y = User.getY() + 6.0 + Random.nextDouble() * 8.0;
        Client.level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), X, Y, Z, 0.0, 0.0, 0.0);
    }
}

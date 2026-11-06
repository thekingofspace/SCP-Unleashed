package net.rai.scp_unleashed.scp107.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp107.Scp107RainKind;
import net.rai.scp_unleashed.scp107.Scp107Registry;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp107ClientEvents {
    public static final float FogShare = 0.35f;
    public static final float OilFogShare = 0.7f;
    private static final float[] LavaFog = {0.55f, 0.22f, 0.12f};
    private static final float[] CausticFog = {0.45f, 0.5f, 0.2f};
    private static final float[] MilkFog = {0.86f, 0.85f, 0.8f};
    private static final float[] OilFog = {0.13f, 0.1f, 0.07f};

    private Scp107ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp107Registry.ShellEntity.get(), Scp107ShellRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp107Registry.Ember.get(), Scp107Particles.EmberProvider::new);
        Event.registerSpriteSet(Scp107Registry.CausticSplash.get(), Scp107Particles.SplashProvider::new);
        Event.registerSpriteSet(Scp107Registry.MilkSplash.get(), Scp107Particles.MilkProvider::new);
        Event.registerSpriteSet(Scp107Registry.OilSplash.get(), Scp107Particles.OilProvider::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new Scp107SkyEffects());
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp107ClientRain.tick();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp107ClientRain.reset();
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null || Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        Optional<Scp107RainKind> Kind = Scp107ClientRain.shownKind(Game.level);
        if (Kind.isEmpty()) {
            return;
        }
        float Open = Game.level.getBrightness(LightLayer.SKY, BlockPos.containing(Event.getCamera().getPosition())) / 15.0f;
        float Share = Mth.clamp(Scp107ClientRain.display() * (Kind.get() == Scp107RainKind.Oil ? OilFogShare : FogShare) * Open, 0.0f, 1.0f);
        float[] Tint = switch (Kind.get()) {
            case Lava -> LavaFog;
            case Milk -> MilkFog;
            case Oil -> OilFog;
            default -> CausticFog;
        };
        Event.setRed(Mth.lerp(Share, Event.getRed(), Tint[0]));
        Event.setGreen(Mth.lerp(Share, Event.getGreen(), Tint[1]));
        Event.setBlue(Mth.lerp(Share, Event.getBlue(), Tint[2]));
    }
}

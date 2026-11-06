package net.rai.scp_unleashed.scp244.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp244.Scp244Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp244Mist {
    public static final float Rise = 0.1f;
    public static final float Fall = 0.06f;
    public static final float Onset = 0.3f;
    public static final float NearestFar = 6.0f;
    public static final float FarSpan = 12.0f;
    public static final float[] Tint = {0.76f, 0.79f, 0.83f};
    public static final float DarkFloor = 0.16f;
    public static final int FlakeReach = 7;
    public static final int CrackleGap = 70;
    public static final int CrackleSpread = 90;
    private static float Share;
    private static float OldShare;
    private static float Density;
    private static int CrackleTimer = CrackleGap;

    private Scp244Mist() {
    }

    public static float share(float Partial) {
        return Mth.lerp(Partial, OldShare, Share);
    }

    public static float densityAt(BlockPos Pos) {
        float Best = 0.0f;
        for (Scp244FogEffect Fog : Scp244FogEffect.active()) {
            Best = Math.max(Best, Fog.densityAt(Pos));
        }
        return Best;
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        OldShare = Share;
        if (World == null) {
            Share = 0.0f;
            Density = 0.0f;
            Scp244FogEffect.active().clear();
            return;
        }
        Scp244FogEffect.prune(World);
        if (Scp244FogEffect.active().isEmpty()) {
            Density = 0.0f;
        } else {
            Vec3 Eye = Game.gameRenderer.getMainCamera().getPosition();
            Density = densityAt(BlockPos.containing(Eye));
        }
        float Goal = Density;
        Share = Goal > Share ? Math.min(Goal, Share + Rise * Math.max(0.2f, Goal - Share) + 0.005f) : Math.max(Goal, Share - Fall * Math.max(0.2f, Share - Goal) - 0.004f);
        if (Share > 0.04f && !Game.isPaused()) {
            frost(Game, World);
        }
    }

    private static void frost(Minecraft Game, ClientLevel World) {
        RandomSource Random = World.random;
        Vec3 Eye = Game.gameRenderer.getMainCamera().getPosition();
        ParticleStatus Status = Game.options.particles().get();
        int Tries = Status == ParticleStatus.ALL ? 6 : Status == ParticleStatus.DECREASED ? 3 : 1;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Try = 0; Try < Tries; Try++) {
            double X = Eye.x + (Random.nextDouble() * 2.0 - 1.0) * FlakeReach;
            double Y = Eye.y + (Random.nextDouble() * 2.0 - 1.2) * FlakeReach * 0.5;
            double Z = Eye.z + (Random.nextDouble() * 2.0 - 1.0) * FlakeReach;
            Cursor.set(X, Y, Z);
            float Here = densityAt(Cursor);
            if (Here < 0.12f || Random.nextFloat() > Here) {
                continue;
            }
            World.addParticle(ParticleTypes.SNOWFLAKE, X, Y, Z, (Random.nextDouble() - 0.5) * 0.02, -0.01 - Random.nextDouble() * 0.02, (Random.nextDouble() - 0.5) * 0.02);
        }
        if (--CrackleTimer > 0) {
            return;
        }
        CrackleTimer = CrackleGap + Random.nextInt(CrackleSpread);
        double X = Eye.x + (Random.nextDouble() * 2.0 - 1.0) * 4.0;
        double Y = Eye.y - 1.0 + Random.nextDouble() * 2.0;
        double Z = Eye.z + (Random.nextDouble() * 2.0 - 1.0) * 4.0;
        float Volume = 0.25f + 0.35f * Share;
        World.playLocalSound(X, Y, Z, Scp244Registry.Frost.get(), SoundSource.AMBIENT, Volume, 0.9f + Random.nextFloat() * 0.25f, false);
        World.playLocalSound(X, Y, Z, Scp244Registry.FrostGlint.get(), SoundSource.AMBIENT, Volume * 0.6f, 0.9f + Random.nextFloat() * 0.25f, false);
    }

    private static float blend(float Value) {
        return Mth.clamp(Value / Onset, 0.0f, 1.0f);
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog Event) {
        if (Event.getType() != FogType.NONE) {
            return;
        }
        float Value = share((float) Event.getPartialTick());
        if (Value < 0.01f) {
            return;
        }
        float Mix = blend(Value);
        float Far = Mth.lerp(Mix, Event.getFarPlaneDistance(), NearestFar + (1.0f - Value) * FarSpan);
        if (Far >= Event.getFarPlaneDistance()) {
            return;
        }
        Event.setFarPlaneDistance(Far);
        Event.setNearPlaneDistance(Mth.lerp(Mix, Math.min(Event.getNearPlaneDistance(), Far), 0.0f));
        Event.setFogShape(FogShape.SPHERE);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Camera View = Event.getCamera();
        if (View.getFluidInCamera() != FogType.NONE) {
            return;
        }
        float Value = share((float) Event.getPartialTick());
        if (Value < 0.01f) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        BlockPos Pos = BlockPos.containing(View.getPosition());
        float Lamp = World.getBrightness(LightLayer.BLOCK, Pos) / 15.0f;
        float Sky = World.getBrightness(LightLayer.SKY, Pos) / 15.0f * World.getSkyDarken((float) Event.getPartialTick());
        float Lit = Math.max(Lamp, Sky);
        float Shade = DarkFloor + (1.0f - DarkFloor) * Lit;
        float Mix = blend(Value);
        Event.setRed(Mth.lerp(Mix, Event.getRed(), Tint[0] * Shade));
        Event.setGreen(Mth.lerp(Mix, Event.getGreen(), Tint[1] * Shade));
        Event.setBlue(Mth.lerp(Mix, Event.getBlue(), Tint[2] * Shade));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Share = 0.0f;
        OldShare = 0.0f;
        Density = 0.0f;
        Scp244FogEffect.active().clear();
    }
}

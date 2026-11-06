package net.rai.scp_unleashed.flamethrower.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.flamethrower.FlamerRegistry;
import net.rai.scp_unleashed.flamethrower.FlamerSounds;
import net.rai.scp_unleashed.flamethrower.FlamerStream;
import net.rai.scp_unleashed.flamethrower.FlamethrowerItem;
import net.rai.scp_unleashed.guns.GunFiring;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.client.GunClientSounds;
import net.rai.scp_unleashed.guns.client.GunClientState;
import net.rai.scp_unleashed.guns.client.GunMuzzle;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class FlamerClient {
    public static final ResourceLocation StateProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flamethrower_state");
    public static final float LocalWindow = 2.5F;
    public static final float RemoteWindow = 4.5F;
    public static final int NearFlames = 13;
    public static final int FarFlames = 5;
    public static final double ParticleReach = 64.0;
    public static final double DetailReach = 28.0;
    public static final double SoundReach = 48.0;
    public static final double FlameSpeed = 1.3;
    public static final float Spread = 6.5F;
    public static final float RampTicks = 4.0F;
    public static final int DouseEvery = 10;
    public static final float PilotVolume = 0.22F;
    public static final float RoarVolume = 1.0F;
    public static final float CrackleVolume = 0.55F;
    private static final Map<Integer, Long> Starts = new HashMap<>();
    private static final Map<Integer, FlamerLoopSound[]> Loops = new HashMap<>();
    private static final Map<Integer, Long> NextBurst = new HashMap<>();
    private static long EmittedTick = Long.MIN_VALUE;
    private static long DouseTick = Long.MIN_VALUE / 2;

    private FlamerClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(FlamerRegistry.Flamethrower.get(), StateProperty, (Stack, World, Holder, Seed) -> state(Stack, Holder)));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(FlamerRegistry.Flame.get(), FlamerFlameParticle.FlameProvider::new);
        Event.registerSpriteSet(FlamerRegistry.Ember.get(), FlamerFlameParticle.EmberProvider::new);
    }

    public static float state(ItemStack Stack, @Nullable LivingEntity Holder) {
        if (GunItem.getAmmo(Stack) <= 0) {
            return 0.0F;
        }
        if (Holder != null && Holder.getMainHandItem().getItem() == Stack.getItem() && firing(Holder, 0.0F)) {
            return 1.0F;
        }
        return 0.5F;
    }

    public static boolean holding(Entity Holder) {
        return Holder instanceof LivingEntity Living && FlamethrowerItem.isFlamethrower(Living.getMainHandItem());
    }

    public static boolean lit(Entity Holder) {
        return Holder instanceof LivingEntity Living && FlamethrowerItem.isFlamethrower(Living.getMainHandItem()) && GunItem.getAmmo(Living.getMainHandItem()) > 0
                && !Living.isUnderWater();
    }

    public static boolean firing(Entity Holder, float Partial) {
        Minecraft Game = Minecraft.getInstance();
        float Window = Game.player != null && Game.player.getId() == Holder.getId() ? LocalWindow : RemoteWindow;
        return holding(Holder) && GunClientState.shotAge(Holder.getId(), Partial) <= Window;
    }

    public static float ramp(Entity Holder, float Partial) {
        Long Start = Starts.get(Holder.getId());
        if (Start == null) {
            return FlamerStream.FirstPulse;
        }
        float Elapsed = GunClientState.ticks() - Start + Partial;
        return Math.min(1.0F, FlamerStream.FirstPulse + (1.0F - FlamerStream.FirstPulse) * Elapsed / RampTicks);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        LocalPlayer Local = Game.player;
        if (World == null || Local == null) {
            clear();
            return;
        }
        long Now = GunClientState.ticks();
        Set<Integer> Seen = new HashSet<>();
        for (Entity Holder : candidates(World, Local)) {
            if (!holding(Holder) || Holder.distanceToSqr(Local) > SoundReach * SoundReach) {
                continue;
            }
            Seen.add(Holder.getId());
            boolean Firing = firing(Holder, 0.0F);
            if (Firing) {
                Starts.putIfAbsent(Holder.getId(), Now);
            } else {
                Starts.remove(Holder.getId());
            }
            loops(Game, Holder, Firing);
            if (Firing) {
                bursts(Game, Holder, Now);
            }
        }
        Starts.keySet().retainAll(Seen);
        NextBurst.keySet().retainAll(Seen);
        Iterator<Map.Entry<Integer, FlamerLoopSound[]>> Entries = Loops.entrySet().iterator();
        while (Entries.hasNext()) {
            boolean Done = true;
            for (FlamerLoopSound Loop : Entries.next().getValue()) {
                Done &= Loop == null || Loop.finished();
            }
            if (Done) {
                Entries.remove();
            }
        }
        douse(Game, Local, Now);
    }

    private static Set<Entity> candidates(ClientLevel World, LocalPlayer Local) {
        Set<Entity> Found = new HashSet<>(World.players());
        for (int Id : GunClientState.shooters()) {
            Entity Shooter = World.getEntity(Id);
            if (Shooter != null) {
                Found.add(Shooter);
            }
        }
        return Found;
    }

    private static void loops(Minecraft Game, Entity Holder, boolean Firing) {
        FlamerLoopSound[] Playing = Loops.computeIfAbsent(Holder.getId(), Id -> new FlamerLoopSound[4]);
        if (lit(Holder)) {
            Playing[0] = keep(Game, Playing[0], () -> new FlamerLoopSound(FlamerRegistry.Pilot.get(), Holder, PilotVolume, 1.0F, 0.1F, 0.2F, FlamerClient::lit));
        }
        if (Firing) {
            Playing[1] = keep(Game, Playing[1], () -> new FlamerLoopSound(FlamerRegistry.Roar.get(), Holder, RoarVolume, 1.0F, 0.34F, 0.25F, Body -> firing(Body, 0.0F)));
            Playing[2] = keep(Game, Playing[2], () -> new FlamerLoopSound(FlamerRegistry.Roar.get(), Holder, RoarVolume * 0.7F, 1.27F, 0.34F, 0.25F, Body -> firing(Body, 0.0F)));
            Playing[3] = keep(Game, Playing[3], () -> new FlamerLoopSound(FlamerRegistry.Crackle.get(), Holder, CrackleVolume, 1.0F, 0.25F, 0.2F, Body -> firing(Body, 0.0F)));
        }
    }

    private static FlamerLoopSound keep(Minecraft Game, @Nullable FlamerLoopSound Current, Supplier<FlamerLoopSound> Maker) {
        if (Current != null && !Current.finished() && Game.getSoundManager().isActive(Current)) {
            return Current;
        }
        FlamerLoopSound Made = Maker.get();
        Game.getSoundManager().play(Made);
        return Made;
    }

    private static void bursts(Minecraft Game, Entity Holder, long Now) {
        Long Due = NextBurst.get(Holder.getId());
        if (Due != null && Now < Due) {
            return;
        }
        RandomSource Random = Holder.level().getRandom();
        NextBurst.put(Holder.getId(), Now + 5 + Random.nextInt(6));
        Vec3 Spot = Holder.getEyePosition().add(Holder.getLookAngle().scale(2.5));
        Holder.level().playLocalSound(Spot.x, Spot.y, Spot.z, FlamerRegistry.Burst.get(), Holder.getSoundSource(), 0.55F + Random.nextFloat() * 0.25F,
                0.85F + Random.nextFloat() * 0.3F, false);
    }

    private static void douse(Minecraft Game, LocalPlayer Local, long Now) {
        if (Game.screen != null || !FlamethrowerItem.isFlamethrower(Local.getMainHandItem()) || !Game.options.keyAttack.isDown() || !FlamerStream.submerged(Local)) {
            return;
        }
        if (Now - DouseTick < DouseEvery) {
            return;
        }
        DouseTick = Now;
        GunClientSounds.play(FlamerSounds.douse(), 0.8F, GunSounds.PitchJitter);
        Vec3 Nozzle = FlamerStream.nozzle(Local);
        RandomSource Random = Local.getRandom();
        for (int Index = 0; Index < 6; Index++) {
            Local.level().addParticle(ParticleTypes.BUBBLE, Nozzle.x, Nozzle.y, Nozzle.z, (Random.nextDouble() - 0.5) * 0.1, 0.05 + Random.nextDouble() * 0.05,
                    (Random.nextDouble() - 0.5) * 0.1);
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        LocalPlayer Local = Game.player;
        long Now = GunClientState.ticks();
        if (World == null || Local == null || Game.isPaused() || Now == EmittedTick) {
            return;
        }
        EmittedTick = Now;
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera View = Event.getCamera();
        for (Entity Holder : candidates(World, Local)) {
            double Distance = Holder.distanceToSqr(View.getPosition());
            if (Distance > ParticleReach * ParticleReach || !(Holder instanceof LivingEntity Living) || !firing(Living, Partial) || Living.isUnderWater()) {
                continue;
            }
            emit(Game, World, Local, Living, View, Event.getProjectionMatrix(), Partial, Distance <= DetailReach * DetailReach);
        }
    }

    private static void emit(Minecraft Game, ClientLevel World, LocalPlayer Local, LivingEntity Holder, Camera View, Matrix4f Projection, float Partial, boolean Near) {
        ItemStack Stack = Holder.getMainHandItem();
        boolean FirstPerson = Holder == Local && Game.options.getCameraType().isFirstPerson();
        Vec3 Muzzle = FirstPerson ? GunMuzzle.firstPersonWorld(Local, Stack, GunKind.Flamethrower, View, Projection, Partial)
                : GunMuzzle.thirdPersonWorld(Holder, Stack, GunKind.Flamethrower, Partial);
        Vec3 Eye = Holder.getEyePosition(Partial);
        Vec3 Look = Holder.getViewVector(Partial);
        boolean Rain = World.isRainingAt(BlockPos.containing(Muzzle).above());
        float Spin = ramp(Holder, Partial) * (Rain ? FlamerStream.RainShare : 1.0F);
        double Range = GunKind.Flamethrower.getStats().getRange() * Spin;
        Vec3 Heading = Eye.add(Look.scale(Range)).subtract(Muzzle).normalize();
        Vec3 Drift = Holder.getDeltaMovement();
        RandomSource Random = World.getRandom();
        int Count = Near ? NearFlames : FarFlames;
        for (int Index = 0; Index < Count; Index++) {
            boolean Lazy = Index % 5 == 4;
            double Speed = FlameSpeed * Spin * (Lazy ? 0.25 + Random.nextDouble() * 0.3 : 0.85 + Random.nextDouble() * 0.3);
            Vec3 Velocity = GunFiring.scatter(Heading, Spread, Random).scale(Speed).add(Drift);
            Vec3 Start = Muzzle.add(Velocity.scale(Random.nextDouble() * 0.9));
            World.addParticle(FlamerRegistry.Flame.get(), Start.x, Start.y, Start.z, Velocity.x, Velocity.y, Velocity.z);
        }
        if (Random.nextFloat() < (Near ? 0.7F : 0.3F)) {
            Vec3 Velocity = GunFiring.scatter(Heading, Spread * 2.0F, Random).scale(FlameSpeed * Spin * 0.6).add(0.0, 0.08, 0.0);
            World.addParticle(FlamerRegistry.Ember.get(), Muzzle.x, Muzzle.y, Muzzle.z, Velocity.x, Velocity.y, Velocity.z);
        }
        if (Near && Random.nextFloat() < 0.35F) {
            Vec3 Smoke = Muzzle.add(Heading.scale(Range * (0.55 + Random.nextDouble() * 0.4)));
            World.addParticle(ParticleTypes.LARGE_SMOKE, Smoke.x, Smoke.y + 0.3, Smoke.z, (Random.nextDouble() - 0.5) * 0.03, 0.04, (Random.nextDouble() - 0.5) * 0.03);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        clear();
    }

    private static void clear() {
        Starts.clear();
        NextBurst.clear();
        for (FlamerLoopSound[] Playing : Loops.values()) {
            for (FlamerLoopSound Loop : Playing) {
                if (Loop != null) {
                    Minecraft.getInstance().getSoundManager().stop(Loop);
                }
            }
        }
        Loops.clear();
    }
}

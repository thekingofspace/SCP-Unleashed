package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp016.client.Scp016ClientHooks;
import net.rai.scp_unleashed.scp016.network.Scp016StatePayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp016Events {
    public static final int SpawnCap = 18;
    public static final int RoomSearch = 3;
    public static final DustParticleOptions SplitDust = new DustParticleOptions(new Vector3f(0.62f, 0.04f, 0.1f), 1.6f);

    private Scp016Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp016Registry.SkitterType.get(), Scp016Skitter.createAttributes().build());
        Event.put(Scp016Registry.BruteType.get(), Scp016Brute.createAttributes().build());
        Event.put(Scp016Registry.SlingerType.get(), Scp016Slinger.createAttributes().build());
        Event.put(Scp016Registry.DuelistType.get(), Scp016Duelist.createAttributes().build());
        Event.put(Scp016Registry.GluttonType.get(), Scp016Glutton.createAttributes().build());
        Event.put(Scp016Registry.SwimmerType.get(), Scp016Swimmer.createAttributes().build());
        Event.put(Scp016Registry.FlyerType.get(), Scp016Flyer.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp016StatePayload.PayloadType, Scp016StatePayload.PayloadCodec, (Payload, Context) -> Scp016ClientHooks.state(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp016Registry.SkitterEgg.get());
            Event.accept(Scp016Registry.BruteEgg.get());
            Event.accept(Scp016Registry.SlingerEgg.get());
            Event.accept(Scp016Registry.DuelistEgg.get());
            Event.accept(Scp016Registry.GluttonEgg.get());
            Event.accept(Scp016Registry.SwimmerEgg.get());
            Event.accept(Scp016Registry.FlyerEgg.get());
        }
    }

    @SubscribeEvent
    public static void tickAfflicted(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (Ticked.level().isClientSide) {
            return;
        }
        visit(Ticked);
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof LivingEntity Subject && Scp016Affliction.isAfflicted(Subject)) {
            Scp016Affliction.tick(Subject);
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (Event.isCanceled()) {
            if (Fallen instanceof ServerPlayer User && ControlManager.hasPendingDeath(User)) {
                Scp016Cause.remember(Event.getSource(), Scp016Cause.classify(Event.getSource(), User));
            }
            return;
        }
        if (!(Fallen.level() instanceof ServerLevel World) || !(Fallen instanceof ServerPlayer || Fallen instanceof AbstractVillager || ControlBodies.isPersonLike(Fallen))) {
            return;
        }
        boolean Carrying = Scp016Affliction.isAfflicted(Fallen);
        if (!Carrying && !(Event.getSource().getEntity() instanceof Scp016Spawn)) {
            return;
        }
        Scp016Cause Cause = Scp016Cause.recall(Event.getSource(), Fallen);
        Scp016Affliction.forget(Fallen);
        split(World, Fallen, Cause);
    }

    public static boolean split(ServerLevel World, LivingEntity Fallen) {
        return split(World, Fallen, Scp016Cause.Other);
    }

    public static boolean split(ServerLevel World, LivingEntity Fallen, Scp016Cause Cause) {
        return split(World, Fallen.position(), Fallen.getBbWidth(), Fallen.getBbHeight(), Cause, Scp016Host.of(Fallen, World.getRandom())) != null;
    }

    @Nullable
    public static Scp016Spawn split(ServerLevel World, Vec3 At, float Width, float Height, Scp016Cause Cause, int Host) {
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.SplitLayers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch);
        }
        double Middle = At.y + Height * 0.5;
        World.sendParticles(Scp016Registry.Fleck.get(), At.x, Middle, At.z, 40, 0.4, 0.6, 0.4, 0.12);
        World.sendParticles(SplitDust, At.x, Middle, At.z, 30, 0.45, 0.6, 0.45, 0.0);
        if (countSpawn(World) >= SpawnCap) {
            return null;
        }
        return raise(World, formFor(Cause, World.getRandom()), At, Host, true);
    }

    @Nullable
    public static Scp016Spawn raise(ServerLevel World, EntityType<? extends Scp016Spawn> Type, Vec3 At, int Host, boolean Emerging) {
        Scp016Spawn Risen = Type.create(World);
        if (Risen == null) {
            return null;
        }
        Vec3 Spot = room(World, Risen, At);
        float Angle = World.getRandom().nextFloat() * 360.0f;
        Risen.moveTo(Spot.x, Spot.y, Spot.z, Angle, 0.0f);
        Risen.setYHeadRot(Angle);
        Risen.setYBodyRot(Angle);
        Risen.setHost(Host);
        Risen.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        if (Emerging) {
            Risen.beginEmerging();
        }
        return World.addFreshEntity(Risen) ? Risen : null;
    }

    public static EntityType<? extends Scp016Spawn> formFor(Scp016Cause Cause, RandomSource Random) {
        return switch (Cause) {
            case Slain -> Scp016Registry.DuelistType.get();
            case Starved -> Scp016Registry.GluttonType.get();
            case Drowned -> Scp016Registry.SwimmerType.get();
            case Fell -> Scp016Registry.FlyerType.get();
            case Other -> {
                List<EntityType<? extends Scp016Spawn>> Pool = defaultPool();
                yield Pool.get(Random.nextInt(Pool.size()));
            }
        };
    }

    public static List<EntityType<? extends Scp016Spawn>> defaultPool() {
        return List.of(Scp016Registry.BruteType.get(), Scp016Registry.SkitterType.get(), Scp016Registry.SlingerType.get());
    }

    private static Vec3 room(ServerLevel World, Scp016Spawn Risen, Vec3 At) {
        if (fits(World, Risen, At)) {
            return At;
        }
        for (int Reach = 1; Reach <= RoomSearch; Reach++) {
            for (int Dy = 0; Dy <= Reach; Dy++) {
                for (int Dx = -Reach; Dx <= Reach; Dx++) {
                    for (int Dz = -Reach; Dz <= Reach; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Reach && Dy != Reach) {
                            continue;
                        }
                        Vec3 Candidate = new Vec3(Math.floor(At.x + Dx) + 0.5, Math.floor(At.y) + Dy, Math.floor(At.z + Dz) + 0.5);
                        if (fits(World, Risen, Candidate) && !World.getBlockState(BlockPos.containing(Candidate).below()).getCollisionShape(World, BlockPos.containing(Candidate).below()).isEmpty()) {
                            return Candidate;
                        }
                    }
                }
            }
        }
        return At;
    }

    private static boolean fits(ServerLevel World, Scp016Spawn Risen, Vec3 Spot) {
        AABB Box = Risen.getType().getDimensions().makeBoundingBox(Spot);
        return World.noCollision(Risen, Box);
    }

    public static void collapse(Scp016Spawn Instance) {
        if (!(Instance.level() instanceof ServerLevel World)) {
            return;
        }
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.CollapseLayers) {
            World.playSound(null, Instance.getX(), Instance.getY(), Instance.getZ(), Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch);
        }
        double Middle = Instance.getY() + Instance.getBbHeight() * 0.5;
        World.sendParticles(Scp016Registry.Fleck.get(), Instance.getX(), Middle, Instance.getZ(), 30, Instance.getBbWidth() * 0.4, Instance.getBbHeight() * 0.35,
                Instance.getBbWidth() * 0.4, 0.05);
        World.sendParticles(SplitDust, Instance.getX(), Middle, Instance.getZ(), 24, Instance.getBbWidth() * 0.4, Instance.getBbHeight() * 0.35, Instance.getBbWidth() * 0.4, 0.0);
        Instance.discard();
    }

    public static int countSpawn(ServerLevel World) {
        return World.getEntities(EntityTypeTest.forClass(Scp016Spawn.class), Found -> Found.isAlive()).size();
    }
}

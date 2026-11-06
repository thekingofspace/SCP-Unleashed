package net.rai.scp_unleashed.scp093;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp093.client.Scp093ClientHooks;
import net.rai.scp_unleashed.scp093.network.Scp093FlashPayload;
import net.rai.scp_unleashed.scp093.network.Scp093VisionPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp093Events {
    private Scp093Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp093FlashPayload.PayloadType, Scp093FlashPayload.PayloadCodec, (Payload, Context) -> Scp093ClientHooks.flash(Payload));
        Registrar.playToClient(Scp093VisionPayload.PayloadType, Scp093VisionPayload.PayloadCodec, (Payload, Context) -> Scp093ClientHooks.vision(Payload));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp093Registry.Torso.get(), Scp093Torso.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp093Registry.Disc.get());
            Event.accept(Scp093Registry.Tears.get());
            Event.accept(Scp093Registry.TorsoEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp093Registry.MirrorItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp093Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp093Transit.tick(Event.getServer());
        Scp093Spawning.tick(Event.getServer());
        Scp093Visions.tickAll(Event.getServer());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        Player User = Event.getEntity();
        if (User.level().isClientSide || !User.hasData(Scp093Registry.Doses)) {
            return;
        }
        Scp093Tears.tickClaim(User);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (!(Dead.level() instanceof ServerLevel World)) {
            return;
        }
        Scp093Visions.forget(Dead);
        if (Event.getSource().is(Scp093Registry.TearsDamage) && !(Dead instanceof Scp093Torso)) {
            Scp093Torso.riseFrom(World, Dead.position(), Dead.getYRot());
            Scp093Tears.play(World, Dead, Scp093Registry.ClaimLayers, 1.0f, 0.6f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void feed(PlayerInteractEvent.EntityInteract Event) {
        ItemStack Stack = Event.getItemStack();
        if (!Scp093TearsItem.is(Stack) || !(Event.getTarget() instanceof LivingEntity Target) || !Target.isAlive()) {
            return;
        }
        boolean Person = !(Target instanceof Player) && ControlBodies.isPersonLike(Target);
        Player User = Event.getEntity();
        if (Event.getLevel().isClientSide) {
            if (Person) {
                Event.setCanceled(true);
                Event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        boolean Controlled = Target instanceof ServerPlayer Body && Body != User && ControlSessions.isControlling(Body);
        if (!Person && !Controlled) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.SUCCESS);
        ServerLevel World = (ServerLevel) Event.getLevel();
        Vec3 Mouth = Target.getEyePosition().add(Target.getLookAngle().scale(0.25)).add(0.0, -0.15, 0.0);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Stack.copyWithCount(1)), Mouth.x, Mouth.y, Mouth.z, 8, 0.08, 0.08, 0.08, 0.04);
        Scp093Tears.play(World, Target, Scp093Registry.DrinkLayers, 0.8f, 1.05f);
        Scp093Tears.drink(Target);
        Stack.consume(1, User);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !(Event.getEntity() instanceof LivingEntity Living) || Living instanceof Player) {
            return;
        }
        if (Living.hasData(Scp093Registry.Doses) && Scp093Visions.afflicted(Living)) {
            Scp093Visions.afflict(World, Living);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (!(Event.getLevel().getLevel() instanceof Level World) || !Scp093Wastes.inside(World) || Event.getEntity() instanceof Scp093Torso) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        if (Kind == MobSpawnType.NATURAL || Kind == MobSpawnType.CHUNK_GENERATION || Kind == MobSpawnType.STRUCTURE || Kind == MobSpawnType.PATROL
                || Kind == MobSpawnType.REINFORCEMENT || Kind == MobSpawnType.JOCKEY) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp093Wastes.inside(World)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        if (Event.getState().getBlock() instanceof Scp093ReturnMirrorBlock && (Event.getPlayer() == null || !Event.getPlayer().isCreative())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp093Tears.sync(User, Scp093Tears.read(User));
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp093Transit.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        if (!Scp093Wastes.inside(User)) {
            Scp093World.get(User.server).clearOrigin(User.getUUID());
        }
        Scp093Tears.sync(User, Scp093Tears.read(User));
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp093Tears.sync(User, Scp093Tears.read(User));
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp093Transit.clear();
        Scp093Visions.clear();
    }
}

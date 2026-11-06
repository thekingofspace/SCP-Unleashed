package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.scp303.network.Scp303UnlockPayload;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp303Events {
    public static final double HeldReach = 4.0;

    private Scp303Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp303Registry.Doorman.get(), Scp303Doorman.createAttributes().build());
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp303Registry.DoormanEgg.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp303UnlockPayload.PayloadType, Scp303UnlockPayload.PayloadCodec,
                (Payload, Context) -> Context.enqueueWork(() -> Scp303Unlock.setClientUnlocked(Payload.Unlocked())));
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (!(Event.getEntity() instanceof Scp303Doorman Doorman) || !(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Scp303State State = Scp303State.get(World.getServer());
        if (State.isActive() && !Doorman.getUUID().equals(State.current()) && Event.loadedFromDisk()) {
            Event.setCanceled(true);
            return;
        }
        if (!State.isCurrent(Doorman.getUUID()) || State.isAway()) {
            State.present(Doorman.getUUID());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp303Keeper.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && User.tickCount % Scp303Unlock.SweepInterval == 0) {
            Scp303Unlock.sweep(User);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp303Unlock.sweep(User);
            Scp303Unlock.sync(User);
        }
    }

    public static boolean binds(Level World, Player User, ItemStack Stack, BlockPos Lower) {
        if (!Stack.is(ModItems.EnergumDust.get()) || !Scp303Arrival.realm(World) || Scp303Doors.isOpen(World, Lower)) {
            return false;
        }
        if (User instanceof ServerPlayer Server) {
            return Scp303Unlock.canBind(Server);
        }
        return World.isClientSide && Scp303Unlock.clientUnlocked();
    }

    public static Optional<Scp303Doorman> holder(Level World, BlockPos Lower) {
        for (Scp303Doorman Doorman : Scp303Doorman.near(World, Lower, HeldReach)) {
            if (Doorman.isAlive() && Doorman.heldDoor().map(Lower::equals).orElse(false)) {
                return Optional.of(Doorman);
            }
        }
        return Optional.empty();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useDoor(PlayerInteractEvent.RightClickBlock Event) {
        Level World = Event.getLevel();
        Optional<BlockPos> Found = Scp303Doors.lowerOf(World, Event.getPos());
        if (Found.isEmpty()) {
            return;
        }
        BlockPos Lower = Found.get();
        Player User = Event.getEntity();
        Optional<Scp303Doorman> Holder = holder(World, Lower);
        if (Holder.isPresent()) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.SUCCESS);
            if (World instanceof ServerLevel Server) {
                Holder.get().rattle(Server, Lower);
            }
            return;
        }
        if (binds(World, User, Event.getItemStack(), Lower)) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.SUCCESS);
            if (World instanceof ServerLevel Server && User instanceof ServerPlayer Binder) {
                bind(Server, Binder, Event.getItemStack(), Lower);
            }
            return;
        }
        if (World instanceof ServerLevel Server) {
            touched(Server, Lower, User);
        }
    }

    public static boolean bind(ServerLevel World, ServerPlayer Binder, ItemStack Stack, BlockPos Lower) {
        Scp303State State = Scp303State.get(World.getServer());
        if (!State.isActive() || State.isBoundDoor(World, Lower)) {
            return false;
        }
        BlockState DoorState = World.getBlockState(Lower);
        int Cell = -Scp303Doors.sideOf(Lower, DoorState, Binder.position());
        State.bind(World.dimension(), Lower, Cell);
        if (!Binder.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        Scp303Doorman Present = Scp303Arrival.loaded(World);
        if (Present != null) {
            Present.bindTo(World);
        }
        Vec3 Handle = Scp303Doors.handle(Lower, DoorState);
        World.sendParticles(Scp303Keeper.Sign, Handle.x, Handle.y, Handle.z, 6, 0.08, 0.25, 0.08, 0.0);
        World.sendParticles(ParticleTypes.WHITE_ASH, Handle.x, Handle.y, Handle.z, 4, 0.1, 0.3, 0.1, 0.0);
        return true;
    }

    public static void touched(ServerLevel World, BlockPos Lower, Player User) {
        Scp303State State = Scp303State.get(World.getServer());
        if (!State.isBoundDoor(World, Lower)) {
            return;
        }
        State.unbind();
        Scp303Doorman Present = Scp303Arrival.loaded(World);
        if (Present != null) {
            Present.freed(World, User);
        } else {
            State.favour(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void broken(BlockEvent.BreakEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !(Event.getState().getBlock() instanceof DoorBlock)) {
            return;
        }
        BlockPos Lower = Event.getState().getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? Event.getPos().below() : Event.getPos();
        holder(World, Lower).ifPresent(Doorman -> Doorman.doorBroken(World, Lower));
        touched(World, Lower, Event.getPlayer());
    }
}

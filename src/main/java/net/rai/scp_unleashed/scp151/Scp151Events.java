package net.rai.scp_unleashed.scp151;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp151.client.Scp151ClientHooks;
import net.rai.scp_unleashed.scp151.network.Scp151BreathPayload;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp151Events {
    public static final String NoStorageKey = "message.scp_unleashed.scp151.no_storage";

    private Scp151Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp151BreathPayload.PayloadType, Scp151BreathPayload.PayloadCodec, (Payload, Context) -> Scp151ClientHooks.breath(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void breathe(LivingBreatheEvent Event) {
        Scp151Breath.breathe(Event);
    }

    @SubscribeEvent
    public static void drown(LivingDrownEvent Event) {
        Scp151Breath.drown(Event);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp151Breath.sync(User);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp151Breath.resend(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void storeAttempt(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        ItemStack Held = Event.getItemStack();
        if (!User.isSecondaryUseActive() || !Held.is(Scp151Registry.PaintingItem.get())) {
            return;
        }
        Level World = Event.getLevel();
        BlockState State = World.getBlockState(Event.getPos());
        if (!(State.getBlock() instanceof StorageContainerBlock) || Event.getHitVec().getDirection() != State.getValue(StorageContainerBlock.Facing)) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.sidedSuccess(World.isClientSide));
        if (World.isClientSide) {
            return;
        }
        User.displayClientMessage(Component.translatable(NoStorageKey).withStyle(ChatFormatting.GRAY), true);
        StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, Event.getPos());
        if (Locker != null) {
            Locker.refuse();
        }
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp151Registry.PaintingItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp151Commands.register(Event.getDispatcher());
    }
}

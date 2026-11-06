package net.rai.scp_unleashed.warhead;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.warhead.client.WarheadClientHooks;
import net.rai.scp_unleashed.warhead.network.WarheadAlertPayload;
import net.rai.scp_unleashed.warhead.network.WarheadAnnouncePayload;
import net.rai.scp_unleashed.warhead.network.WarheadBlastPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class WarheadEvents {
    private WarheadEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(WarheadAlertPayload.PayloadType, WarheadAlertPayload.PayloadCodec, (Payload, Context) -> WarheadClientHooks.alert(Payload));
        Registrar.playToClient(WarheadAnnouncePayload.PayloadType, WarheadAnnouncePayload.PayloadCodec, (Payload, Context) -> WarheadClientHooks.announce(Payload));
        Registrar.playToClient(WarheadBlastPayload.PayloadType, WarheadBlastPayload.PayloadCodec, (Payload, Context) -> WarheadClientHooks.blast(Payload));
    }

    @SubscribeEvent
    public static void tickLevel(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            WarheadNetwork.get(World).tick(World);
        }
    }

    @SubscribeEvent
    public static void guardBreaking(BlockEvent.BreakEvent Event) {
        BlockState State = Event.getState();
        boolean Guarded = false;
        if (State.getBlock() instanceof AlphaWarheadBlock) {
            Guarded = State.getValue(AlphaWarheadBlock.Status).isActive();
            if (!Guarded && Event.getLevel() instanceof ServerLevel World) {
                Guarded = WarheadNetwork.get(World).sequenceTargeting(AlphaWarheadBlock.masterOf(Event.getPos(), State)).isPresent();
            }
        } else if (State.getBlock() instanceof WarheadPanelBlock) {
            WarheadPanelBlockEntity Panel = WarheadPanelBlock.masterEntity(Event.getLevel(), Event.getPos(), State);
            Guarded = Panel != null && Panel.isEmitting();
        }
        if (Guarded && !Event.getPlayer().isCreative()) {
            Event.setCanceled(true);
            Event.getPlayer().displayClientMessage(Component.translatable("message.scp_unleashed.warhead.unbreakable").withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(WarheadRegistry.AlphaWarheadItem.get());
            Event.accept(WarheadRegistry.PanelItem.get());
        }
    }
}

package net.rai.scp_unleashed.scp465;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp465.client.Scp465ClientHooks;
import net.rai.scp_unleashed.scp465.network.Scp465LetterPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp465Events {
    private Scp465Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp465Registry.Guest.get(), Scp465Guest.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp465LetterPayload.PayloadType, Scp465LetterPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> Scp465ClientHooks.letter(Payload)));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp465Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp465Registry.BoxItem.get());
            for (Scp465Decor Kind : Scp465Decor.values()) {
                Event.accept(Scp465Registry.decorItem(Kind));
            }
        } else if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp465Registry.Letter.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            Event.accept(Scp465Registry.Punch.get());
            Event.accept(Scp465Registry.Cupcake.get());
            Event.accept(Scp465Registry.Chips.get());
            Event.accept(Scp465Registry.Chocolate.get());
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp465Celebration.tick(World);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void breaking(BlockEvent.BreakEvent Event) {
        BlockState State = Event.getState();
        if (Scp465BoxBlock.isOpen(State) && (Event.getPlayer() == null || !Event.getPlayer().isCreative())) {
            Event.setCanceled(true);
        }
    }
}

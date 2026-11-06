package net.rai.scp_unleashed.scp085;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp085.network.Scp085ResidentPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp085Events {
    public static final int ValidateTicks = 100;

    private Scp085Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp085ResidentPayload.PayloadType, Scp085ResidentPayload.PayloadCodec, (Payload, Context) -> Scp085Resident.show(Payload.Resident()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(new ItemStack(Scp085Registry.HomeSheetItem.get()));
            for (Scp085Scene Scene : Scp085Scene.Drawings) {
                Event.accept(Scp085DrawingBlock.stackOf(Scene, 1));
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        if (Server.getTickCount() % ValidateTicks == 0) {
            Scp085Figure.get(Server).validate(Server);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            PacketDistributor.sendToPlayer(User, new Scp085ResidentPayload(Scp085Figure.get(User.server).resident()));
        }
    }
}

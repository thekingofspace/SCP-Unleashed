package net.rai.scp_unleashed.archive;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class UniqueGifts {
    public static final String KeptKey = "tooltip.scp_unleashed.unique_gift.kept";
    public static final int CheckInterval = 20;

    private static volatile boolean ClientReusable = true;
    @Nullable
    private static Boolean LastSent;

    private UniqueGifts() {
    }

    public static boolean keeps(@Nullable Player User) {
        if (User != null && User.getServer() != null && !User.level().isClientSide) {
            return ArchiveRules.uniqueItemsReusable(User.getServer());
        }
        return ClientReusable;
    }

    public static Component keptLine() {
        return Component.translatable(KeptKey).withStyle(ChatFormatting.GOLD);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToClient(RulePayload.PayloadType, RulePayload.PayloadCodec, (Payload, Context) -> ClientReusable = Payload.Reusable());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Joined) {
            PacketDistributor.sendToPlayer(Joined, new RulePayload(ArchiveRules.uniqueItemsReusable(Joined.server)));
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        if (Server.getTickCount() % CheckInterval != 0) {
            return;
        }
        boolean Reusable = ArchiveRules.uniqueItemsReusable(Server);
        if (LastSent == null || LastSent != Reusable) {
            LastSent = Reusable;
            PacketDistributor.sendToAllPlayers(new RulePayload(Reusable));
        }
    }

    public record RulePayload(boolean Reusable) implements CustomPacketPayload {
        public static final Type<RulePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_unique_gifts"));
        public static final StreamCodec<ByteBuf, RulePayload> PayloadCodec = ByteBufCodecs.BOOL.map(RulePayload::new, RulePayload::Reusable);

        @Override
        public Type<RulePayload> type() {
            return PayloadType;
        }
    }
}

package net.rai.scp_unleashed.speaker;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;

public record SoundConsolePayload(BlockPos Pos, ResourceLocation Sound, float Volume, float Pitch, boolean Loop, Command Order) implements CustomPacketPayload {
    public static final Type<SoundConsolePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "speaker_console"));
    public static final StreamCodec<ByteBuf, SoundConsolePayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SoundConsolePayload::Pos,
            ResourceLocation.STREAM_CODEC, SoundConsolePayload::Sound,
            ByteBufCodecs.FLOAT, SoundConsolePayload::Volume,
            ByteBufCodecs.FLOAT, SoundConsolePayload::Pitch,
            ByteBufCodecs.BOOL, SoundConsolePayload::Loop,
            Command.CommandCodec, SoundConsolePayload::Order,
            SoundConsolePayload::new);

    @Override
    public Type<SoundConsolePayload> type() {
        return PayloadType;
    }

    public static void handle(SoundConsolePayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer User) || !(User.level() instanceof ServerLevel World) || !World.isLoaded(Payload.Pos())
                || !User.canInteractWithBlock(Payload.Pos(), 1.0) || !(World.getBlockEntity(Payload.Pos()) instanceof SoundConsoleBlockEntity Console)) {
            return;
        }
        Console.applySettings(User, Payload.Sound(), Payload.Volume(), Payload.Pitch(), Payload.Loop());
        boolean Sent = switch (Payload.Order()) {
            case Send -> Console.transmit(World);
            case Stop -> Console.stopLoop(World);
            case Apply -> true;
        };
        if (!Sent) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.sound_console.no_power"), true);
        }
    }

    public enum Command {
        Apply,
        Send,
        Stop;

        private static final Command[] Orders = values();
        public static final StreamCodec<ByteBuf, Command> CommandCodec = ByteBufCodecs.idMapper(Index -> Orders[Math.floorMod(Index, Orders.length)], Command::ordinal);
    }
}

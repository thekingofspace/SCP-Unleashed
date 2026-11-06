package net.rai.scp_unleashed.scp143.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp143.Scp143PetalPath;

import java.util.List;

public record Scp143PetalPayload(List<Scp143PetalPath.Spawn> Petals) implements CustomPacketPayload {
    public static final int MaxPetals = 128;
    public static final Type<Scp143PetalPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp143_petals"));
    public static final StreamCodec<ByteBuf, Scp143PetalPayload> PayloadCodec = StreamCodec.composite(
            Scp143PetalPath.Spawn.SpawnCodec.apply(ByteBufCodecs.list(MaxPetals)), Scp143PetalPayload::Petals,
            Scp143PetalPayload::new);

    @Override
    public Type<Scp143PetalPayload> type() {
        return PayloadType;
    }
}

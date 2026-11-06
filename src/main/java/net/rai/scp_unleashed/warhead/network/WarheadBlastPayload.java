package net.rai.scp_unleashed.warhead.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;

public record WarheadBlastPayload(double X, double Y, double Z) implements CustomPacketPayload {
    public static final Type<WarheadBlastPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "warhead_blast"));
    public static final StreamCodec<ByteBuf, WarheadBlastPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, WarheadBlastPayload::X,
            ByteBufCodecs.DOUBLE, WarheadBlastPayload::Y,
            ByteBufCodecs.DOUBLE, WarheadBlastPayload::Z,
            WarheadBlastPayload::new);

    public WarheadBlastPayload(Vec3 Centre) {
        this(Centre.x, Centre.y, Centre.z);
    }

    public Vec3 centre() {
        return new Vec3(X, Y, Z);
    }

    @Override
    public Type<WarheadBlastPayload> type() {
        return PayloadType;
    }
}

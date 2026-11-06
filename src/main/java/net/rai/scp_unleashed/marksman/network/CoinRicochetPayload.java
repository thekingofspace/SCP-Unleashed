package net.rai.scp_unleashed.marksman.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.network.GunShotPayload;

import java.util.List;

public record CoinRicochetPayload(int ShooterId, List<Vec3> Path, int Outcome, int Face) implements CustomPacketPayload {
    public static final int MaxPoints = 8;
    public static final int Stray = 0;
    public static final int StrayBlock = 1;
    public static final int Wound = 2;
    public static final int Kill = 3;
    public static final int Shrugged = 4;
    public static final Type<CoinRicochetPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "marksman_ricochet"));
    public static final StreamCodec<ByteBuf, CoinRicochetPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CoinRicochetPayload::ShooterId,
            GunShotPayload.PointCodec.apply(ByteBufCodecs.list(MaxPoints)), CoinRicochetPayload::Path,
            ByteBufCodecs.VAR_INT, CoinRicochetPayload::Outcome,
            ByteBufCodecs.VAR_INT, CoinRicochetPayload::Face,
            CoinRicochetPayload::new);

    @Override
    public Type<CoinRicochetPayload> type() {
        return PayloadType;
    }

    public boolean struck() {
        return Outcome == Wound || Outcome == Kill;
    }

    public int coins() {
        return Path.size() - 1;
    }
}

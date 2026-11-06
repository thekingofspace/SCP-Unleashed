package net.rai.scp_unleashed.guns.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunReloading;

public record GunReloadPayload() implements CustomPacketPayload {
    public static final GunReloadPayload Instance = new GunReloadPayload();
    public static final Type<GunReloadPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_reload"));
    public static final StreamCodec<ByteBuf, GunReloadPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<GunReloadPayload> type() {
        return PayloadType;
    }

    public static void handle(GunReloadPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User) {
            GunReloading.start(User);
        }
    }
}

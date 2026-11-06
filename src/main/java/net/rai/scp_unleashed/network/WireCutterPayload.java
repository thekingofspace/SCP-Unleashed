package net.rai.scp_unleashed.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.item.WireCutterItem;
import net.rai.scp_unleashed.wire.WireCutting;

public record WireCutterPayload(BlockPos Owner, BlockPos Target, boolean MainHand) implements CustomPacketPayload {
    public static final Type<WireCutterPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "wire_cutter_snip"));
    public static final StreamCodec<ByteBuf, WireCutterPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WireCutterPayload::Owner,
            BlockPos.STREAM_CODEC, WireCutterPayload::Target,
            ByteBufCodecs.BOOL, WireCutterPayload::MainHand,
            WireCutterPayload::new);

    @Override
    public Type<WireCutterPayload> type() {
        return PayloadType;
    }

    public static void handle(WireCutterPayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer User) || !(User.level() instanceof ServerLevel World) || User.isSpectator()) {
            return;
        }
        InteractionHand Hand = Payload.MainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack Stack = User.getItemInHand(Hand);
        if (!(Stack.getItem() instanceof WireCutterItem) || !User.mayBuild()) {
            return;
        }
        if (WireCutting.cut(World, User, Payload.Owner(), Payload.Target())) {
            Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
        }
    }
}

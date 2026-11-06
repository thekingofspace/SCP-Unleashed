package net.rai.scp_unleashed.marksman.ability;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;

public record ToolAbilityPayload() implements CustomPacketPayload {
    public static final ToolAbilityPayload Instance = new ToolAbilityPayload();
    public static final Type<ToolAbilityPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "marksman_tool_ability"));
    public static final StreamCodec<ByteBuf, ToolAbilityPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<ToolAbilityPayload> type() {
        return PayloadType;
    }

    public static void handle(ToolAbilityPayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer User) || User.isSpectator() || !User.isAlive()) {
            return;
        }
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = User.getItemInHand(Hand);
            if (Stack.getItem() instanceof ToolAbilityItem Ability && Ability.acceptsAbility(Stack, Hand)) {
                Ability.useAbility(User, Stack, Hand);
                return;
            }
        }
    }
}

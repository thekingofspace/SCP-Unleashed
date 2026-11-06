package net.rai.scp_unleashed.marksman.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface ToolAbilityItem {
    String AbilityKey = "key.scp_unleashed.tool_ability";

    void useAbility(ServerPlayer User, ItemStack Stack, InteractionHand Hand);

    default boolean acceptsAbility(ItemStack Stack, InteractionHand Hand) {
        return true;
    }
}

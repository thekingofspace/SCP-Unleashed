package net.rai.scp_unleashed.scp217.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityItem;
import net.rai.scp_unleashed.marksman.client.ToolAbilityKeys;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp217.network.Scp217PulseRequestPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp217PulseKey {
    private Scp217PulseKey() {
    }

    @SubscribeEvent
    public static void key(InputEvent.Key Event) {
        if (Event.getAction() == InputConstants.PRESS && ToolAbilityKeys.AbilityKey.isActiveAndMatches(InputConstants.getKey(Event.getKey(), Event.getScanCode()))) {
            press();
        }
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre Event) {
        if (Event.getAction() == InputConstants.PRESS && ToolAbilityKeys.AbilityKey.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(Event.getButton()))) {
            press();
        }
    }

    private static void press() {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null || Game.screen != null || User.isSpectator() || holdsTool(User)) {
            return;
        }
        Scp217ClientState.Entry Found = Scp217ClientState.get(User.getId());
        if (Found == null || !Found.perfect() || Found.stage() < Scp217Clockwork.PulseStage) {
            return;
        }
        PacketDistributor.sendToServer(Scp217PulseRequestPayload.Instance);
    }

    private static boolean holdsTool(LocalPlayer User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = User.getItemInHand(Hand);
            if (Stack.getItem() instanceof ToolAbilityItem Ability && Ability.acceptsAbility(Stack, Hand)) {
                return true;
            }
        }
        return false;
    }
}

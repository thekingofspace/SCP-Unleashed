package net.rai.scp_unleashed.marksman.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.client.GunsClientEvents;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityItem;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityPayload;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ToolAbilityKeys {
    public static final KeyMapping AbilityKey = new KeyMapping(ToolAbilityItem.AbilityKey, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            GunsClientEvents.KeyCategory);

    private ToolAbilityKeys() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent Event) {
        Event.register(AbilityKey);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        boolean Pressed = false;
        while (AbilityKey.consumeClick()) {
            Pressed = true;
        }
        if (Pressed && User != null && Game.screen == null && !User.isSpectator() && holdsAbility(User)) {
            PacketDistributor.sendToServer(ToolAbilityPayload.Instance);
        }
    }

    private static boolean holdsAbility(LocalPlayer User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Stack = User.getItemInHand(Hand);
            if (Stack.getItem() instanceof ToolAbilityItem Ability && Ability.acceptsAbility(Stack, Hand)) {
                return true;
            }
        }
        return false;
    }
}

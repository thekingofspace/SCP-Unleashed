package net.rai.scp_unleashed.antimemetic.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class AntiMemeticClientEvents {
    public static final int WrapWidth = 150;

    private AntiMemeticClientEvents() {
    }

    @SubscribeEvent
    public static void effectTooltip(GatherEffectScreenTooltipsEvent Event) {
        if (!Event.getEffectInstance().is(AntiMemeticRegistry.Shielded)) {
            return;
        }
        Component Description = Component.translatable(Event.getEffectInstance().getDescriptionId() + ".description");
        for (FormattedText Line : Minecraft.getInstance().font.getSplitter().splitLines(Description, WrapWidth, Style.EMPTY)) {
            Event.getTooltip().add(Component.literal(Line.getString()).withStyle(ChatFormatting.GRAY));
        }
    }
}

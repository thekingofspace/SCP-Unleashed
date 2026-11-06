package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class HudOrder {
    private HudOrder() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void liftLateHud(RenderGuiEvent.Post Event) {
        Event.getGuiGraphics().flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }
}

package net.rai.scp_unleashed.terminal.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.terminal.TerminalGate;
import net.rai.scp_unleashed.terminal.TerminalSettings;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class TerminalScreens {
    private TerminalScreens() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void opening(ScreenEvent.Opening Event) {
        Screen Next = Event.getNewScreen();
        TitleArchive.opening(Next);
        if (Next == null) {
            return;
        }
        TerminalGate.Decision Choice = TerminalGate.opening(Next.getClass().getName(), Next instanceof TerminalTitleScreen, TerminalSettings.menu());
        if (Choice == TerminalGate.Decision.Terminal) {
            Event.setNewScreen(new TerminalTitleScreen());
        } else if (Choice == TerminalGate.Decision.Vanilla) {
            Event.setNewScreen(new TitleScreen());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void render(ScreenEvent.Render.Pre Event) {
        Screen Shown = Event.getScreen();
        TerminalGate.Kind Which = TerminalGate.kindOf(Shown.getClass().getName());
        if (Which == TerminalGate.Kind.None || Which == TerminalGate.Kind.Title || !TerminalSettings.applies(Which)) {
            return;
        }
        boolean Painted = Which == TerminalGate.Kind.Worlds
                ? TerminalWorldStyle.draw(Shown, Event.getGuiGraphics(), Event.getMouseX(), Event.getMouseY(), Event.getPartialTick())
                : TerminalLoading.draw(Shown, Which, Event.getGuiGraphics(), Event.getMouseX(), Event.getMouseY(), Event.getPartialTick());
        if (Painted) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void key(ScreenEvent.KeyPressed.Pre Event) {
        TerminalClient.keyboard(Event.getKeyCode());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        TerminalLoading.tick();
    }
}

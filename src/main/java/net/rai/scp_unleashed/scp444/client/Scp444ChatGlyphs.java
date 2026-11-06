package net.rai.scp_unleashed.scp444.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.rai.scp_unleashed.scp444.Scp444Pages;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiFunction;

public final class Scp444ChatGlyphs {
    private static final Logger Log = LogUtils.getLogger();
    @Nullable
    private static Field FormatterField;
    private static boolean Resolved;

    private Scp444ChatGlyphs() {
    }

    @Nullable
    private static Field formatterField() {
        if (!Resolved) {
            Resolved = true;
            try {
                FormatterField = ObfuscationReflectionHelper.findField(EditBox.class, "formatter");
            } catch (RuntimeException Problem) {
                Log.warn("SCP-444 chat glyphs are unavailable: the chat box formatter could not be found", Problem);
            }
        }
        return FormatterField;
    }

    public static void attach(ChatScreen Chat, List<GuiEventListener> Listeners) {
        Field Formatter = formatterField();
        if (Formatter == null) {
            return;
        }
        for (GuiEventListener Listener : Listeners) {
            if (Listener instanceof EditBox Box) {
                wrap(Formatter, Box);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void wrap(Field Formatter, EditBox Box) {
        try {
            Object Current = Formatter.get(Box);
            if (Current instanceof Wrapped || !(Current instanceof BiFunction<?, ?, ?>)) {
                return;
            }
            Box.setFormatter(new Wrapped(Box, (BiFunction<String, Integer, FormattedCharSequence>) Current));
        } catch (IllegalAccessException | RuntimeException Problem) {
            Log.warn("SCP-444 chat glyphs could not wrap the chat box", Problem);
        }
    }

    private record Wrapped(EditBox Box, BiFunction<String, Integer, FormattedCharSequence> Original) implements BiFunction<String, Integer, FormattedCharSequence> {
        @Override
        public FormattedCharSequence apply(String Text, Integer Offset) {
            if (!Scp444ClientState.isSilenced() || Box.getValue().startsWith("/")) {
                return Original.apply(Text, Offset);
            }
            return Scp444Script.sequence(Scp444Script.transliterate(Text), Scp444Pages.MainFont);
        }
    }
}

package net.rai.scp_unleashed.scp444.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp444.Scp444Pages;

public final class Scp444Script {
    public static final ResourceLocation MainFont = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_script");
    public static final ResourceLocation SmallFont = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_script_small");
    public static final Style MainStyle = Style.EMPTY.withFont(MainFont);
    public static final Style SmallStyle = Style.EMPTY.withFont(SmallFont);

    private Scp444Script() {
    }

    public static Style style(int Face) {
        return Face == Scp444Pages.SmallFont ? SmallStyle : MainStyle;
    }

    public static FormattedCharSequence sequence(String Text, int Face) {
        return FormattedCharSequence.forward(Text, style(Face));
    }

    public static int width(Font Measure, String Text, int Face) {
        return Measure.width(FormattedText.of(Text, style(Face)));
    }

    public static int glyphFor(int Letter) {
        int Mixed = Letter * 0x9E3779B1 + 0x7F4A7C15;
        Mixed ^= Mixed >>> 15;
        Mixed *= 0x2C1B3C6D;
        Mixed ^= Mixed >>> 12;
        return Scp444Pages.MainBase + Math.floorMod(Mixed, Scp444Pages.MainGlyphs);
    }

    public static String transliterate(String Text) {
        StringBuilder Built = new StringBuilder(Text.length());
        for (int Index = 0; Index < Text.length(); Index++) {
            char Letter = Text.charAt(Index);
            if (Letter <= ' ') {
                Built.append(Letter);
            } else {
                Built.append((char) glyphFor(Letter));
            }
        }
        return Built.toString();
    }
}

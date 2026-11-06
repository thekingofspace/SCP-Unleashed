package net.rai.scp_unleashed.scp140.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp140Script {
    public static final ResourceLocation InkFont = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp140_script");
    public static final ResourceLocation BleedFont = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp140_script_bleed");
    public static final Style InkStyle = Style.EMPTY.withFont(InkFont);
    public static final Style BleedStyle = Style.EMPTY.withFont(BleedFont);
    public static final int FirstCode = 32;
    public static final int LastCode = 126;
    private static final String[] Singles = new String[LastCode + 1];

    private final int[] Advances = new int[LastCode + 1];

    public Scp140Script(Font Measure) {
        for (int Code = FirstCode; Code <= LastCode; Code++) {
            Advances[Code] = Measure.width(FormattedText.of(single((char) Code), InkStyle));
        }
    }

    public static String single(char Character) {
        int Code = Character;
        if (Code < 0 || Code > LastCode) {
            return String.valueOf(Character);
        }
        String Found = Singles[Code];
        if (Found == null) {
            Found = String.valueOf(Character);
            Singles[Code] = Found;
        }
        return Found;
    }

    public static String clean(String Text) {
        StringBuilder Built = new StringBuilder(Text.length());
        for (int Index = 0; Index < Text.length(); Index++) {
            char Character = Text.charAt(Index);
            if (Character == '’' || Character == '‘') {
                Built.append('\'');
            } else if (Character == '“' || Character == '”') {
                Built.append('"');
            } else if (Character == '—' || Character == '–') {
                Built.append('-');
            } else if (Character >= FirstCode && Character <= LastCode) {
                Built.append(Character);
            } else if (Character == '\n') {
                Built.append(' ');
            }
        }
        return Built.toString();
    }

    public int advance(char Character) {
        int Code = Character;
        return Code >= FirstCode && Code <= LastCode ? Advances[Code] : 0;
    }

    public int width(String Text) {
        int Total = 0;
        for (int Index = 0; Index < Text.length(); Index++) {
            Total += advance(Text.charAt(Index));
        }
        return Total;
    }

    public static FormattedCharSequence ink(char Character) {
        return FormattedCharSequence.forward(single(Character), InkStyle);
    }

    public static FormattedCharSequence bleed(char Character) {
        return FormattedCharSequence.forward(single(Character), BleedStyle);
    }
}

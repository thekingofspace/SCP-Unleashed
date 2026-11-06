package net.rai.scp_unleashed.discord;

import net.minecraft.network.chat.Component;

public final class DiscordText {
    public static final int TitleLimit = 256;
    public static final int DescriptionLimit = 4096;
    public static final int FieldNameLimit = 256;
    public static final int FieldValueLimit = 1024;
    public static final int FooterLimit = 2048;
    public static final int AuthorLimit = 256;
    public static final int FieldCount = 25;
    public static final int TotalLimit = 6000;
    public static final int UsernameLimit = 80;
    public static final String Ellipsis = "\u2026";
    private static final String Inline = "\\*_~`|[]<";
    private static final String LineStart = "#->+";

    private DiscordText() {
    }

    public static String escape(String Raw) {
        if (Raw == null || Raw.isEmpty()) {
            return "";
        }
        StringBuilder Out = new StringBuilder(Raw.length() + 8);
        boolean Fresh = true;
        for (int Index = 0; Index < Raw.length(); Index++) {
            char Next = Raw.charAt(Index);
            if (Next == '\r') {
                continue;
            }
            if (Next == '\n') {
                Out.append(Next);
                Fresh = true;
                continue;
            }
            if (Next == '@') {
                Out.append("@\u200B");
                Fresh = false;
                continue;
            }
            if (Fresh && Next == ' ') {
                Out.append(Next);
                continue;
            }
            if (Fresh && Character.isDigit(Next)) {
                int End = Index;
                while (End < Raw.length() && Character.isDigit(Raw.charAt(End))) {
                    End++;
                }
                Out.append(Raw, Index, End);
                if (End < Raw.length() && Raw.charAt(End) == '.') {
                    Out.append("\\.");
                    End++;
                }
                Index = End - 1;
                Fresh = false;
                continue;
            }
            if (Inline.indexOf(Next) >= 0 || Fresh && LineStart.indexOf(Next) >= 0) {
                Out.append('\\');
            }
            Out.append(Next);
            Fresh = false;
        }
        return Out.toString();
    }

    public static String flat(String Raw) {
        if (Raw == null) {
            return "";
        }
        return Raw.replace('\r', ' ').replace('\n', ' ').replace("@", "@\u200B").strip();
    }

    public static String plain(Component Text) {
        return escape(Text.getString());
    }

    public static String clip(String Raw, int Limit) {
        if (Raw == null) {
            return "";
        }
        if (Raw.length() <= Limit) {
            return Raw;
        }
        int Cut = Math.max(0, Limit - Ellipsis.length());
        if (Cut > 0 && Character.isHighSurrogate(Raw.charAt(Cut - 1))) {
            Cut--;
        }
        int Slashes = 0;
        for (int Index = Cut - 1; Index >= 0 && Raw.charAt(Index) == '\\'; Index--) {
            Slashes++;
        }
        if (Slashes % 2 == 1) {
            Cut--;
        }
        return Raw.substring(0, Math.max(0, Cut)) + Ellipsis;
    }

    public static String translate(String Key, Object... Args) {
        return Component.translatable(Key, Args).getString();
    }
}

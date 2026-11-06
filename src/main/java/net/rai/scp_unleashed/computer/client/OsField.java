package net.rai.scp_unleashed.computer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.IntPredicate;

public final class OsField {
    public static final IntPredicate AnyText = Letter -> true;
    public static final IntPredicate Digits = Letter -> Letter >= '0' && Letter <= '9';

    private final int Max;
    private final boolean Upper;
    private final boolean Multiline;
    private final IntPredicate Filter;
    private String Value = "";
    private int Cursor;
    private int Shift;

    public OsField(int Max, boolean Upper, IntPredicate Filter) {
        this(Max, Upper, Filter, false);
    }

    public OsField(int Max, boolean Upper, IntPredicate Filter, boolean Multiline) {
        this.Max = Max;
        this.Upper = Upper;
        this.Filter = Filter;
        this.Multiline = Multiline;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String NewValue) {
        Value = clean(NewValue == null ? "" : NewValue);
        Cursor = Value.length();
        Shift = 0;
    }

    public int getCursor() {
        return Cursor;
    }

    public boolean isMultiline() {
        return Multiline;
    }

    public int number(int Fallback) {
        try {
            return Value.isEmpty() ? Fallback : Integer.parseInt(Value);
        } catch (NumberFormatException Problem) {
            return Fallback;
        }
    }

    private String clean(String Raw) {
        StringBuilder Built = new StringBuilder();
        for (int Index = 0; Index < Raw.length() && Built.length() < Max; Index++) {
            char Letter = Raw.charAt(Index);
            if (Letter == '\n' && Multiline) {
                Built.append(Letter);
            } else if (StringUtil.isAllowedChatCharacter(Letter) && Filter.test(Letter)) {
                Built.append(Upper ? Character.toUpperCase(Letter) : Letter);
            }
        }
        return Built.toString();
    }

    private void insert(String Text) {
        String Cleaned = clean(Text);
        int Room = Max - Value.length();
        if (Room <= 0 || Cleaned.isEmpty()) {
            return;
        }
        if (Cleaned.length() > Room) {
            Cleaned = Cleaned.substring(0, Room);
        }
        Value = Value.substring(0, Cursor) + Cleaned + Value.substring(Cursor);
        Cursor += Cleaned.length();
    }

    public boolean charTyped(char Letter) {
        if (!StringUtil.isAllowedChatCharacter(Letter) || !Filter.test(Letter)) {
            return false;
        }
        insert(String.valueOf(Letter));
        return true;
    }

    public boolean keyPressed(int Key) {
        switch (Key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (Cursor > 0) {
                    Value = Value.substring(0, Cursor - 1) + Value.substring(Cursor);
                    Cursor--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (Cursor < Value.length()) {
                    Value = Value.substring(0, Cursor) + Value.substring(Cursor + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                Cursor = Math.max(0, Cursor - 1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                Cursor = Math.min(Value.length(), Cursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                Cursor = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                Cursor = Value.length();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (Multiline && !Screen.hasShiftDown()) {
                    insert("\n");
                    return true;
                }
                return false;
            }
            default -> {
                if (Key == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                    insert(Minecraft.getInstance().keyboardHandler.getClipboard().replace("\r", ""));
                    return true;
                }
                if (Key == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(Value);
                    return true;
                }
                return false;
            }
        }
    }

    public String visible(Font Text, int Width) {
        if (Cursor < Shift) {
            Shift = Cursor;
        }
        while (Shift < Cursor && Text.width(Value.substring(Shift, Cursor)) > Width - 2) {
            Shift++;
        }
        Shift = Math.max(0, Math.min(Shift, Value.length()));
        return Text.plainSubstrByWidth(Value.substring(Shift), Width);
    }

    public int caretX(Font Text) {
        int From = Math.min(Shift, Cursor);
        return Text.width(Value.substring(From, Cursor));
    }

    public String upperHint(String Hint) {
        return Upper ? Hint.toUpperCase(Locale.ROOT) : Hint;
    }
}

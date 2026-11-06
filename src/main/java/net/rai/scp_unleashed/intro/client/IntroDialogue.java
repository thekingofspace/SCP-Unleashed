package net.rai.scp_unleashed.intro.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class IntroDialogue {
    public static final float CharsPerTick = 1.1f;
    public static final int SentencePause = 12;
    public static final int CommaPause = 5;
    public static final float Drift = 0.07f;
    public static final float Fall = 0.9f;
    public static final float Rise = 1.08f;
    public static final int PageGap = 6;
    public static final int AutoBase = 70;
    public static final float AutoPerChar = 0.45f;
    public static final int TextWidth = 268;
    public static final int TextRows = 5;
    private final Component Plate;
    private final List<Component> Pages;
    private final IntroVoice Voice;
    private final RandomSource Random = RandomSource.create();
    private final List<String> Lines = new ArrayList<>();
    private int Page = -1;
    private float Shown;
    private int Pause;
    private int TotalChars;
    private int Letters;
    private int Gap = 3;
    private int Mouth;
    private boolean Opened;
    private boolean Closed;
    private int Idle;
    private int Age;
    private boolean Finished;
    private boolean Auto = true;

    public IntroDialogue(Component Plate, List<Component> Pages, IntroVoice Voice) {
        this.Plate = Plate;
        this.Pages = Pages;
        this.Voice = Voice;
    }

    public Component plate() {
        return Plate;
    }

    public int page() {
        return Page;
    }

    public int pages() {
        return Pages.size();
    }

    public int age() {
        return Age;
    }

    public boolean finished() {
        return Finished;
    }

    public boolean lastPage() {
        return Page >= Pages.size() - 1;
    }

    public void setAuto(boolean On) {
        Auto = On;
    }

    public List<String> lines() {
        return Lines;
    }

    public boolean pageDone() {
        return Page >= 0 && Shown >= TotalChars;
    }

    public boolean speaking() {
        return Page >= 0 && !pageDone() && Pause <= 0;
    }

    public boolean mouthOpen() {
        return Mouth > 0;
    }

    public int visibleChars(float Partial) {
        float Extra = Pause > 0 || pageDone() ? 0.0f : CharsPerTick * Partial;
        return Math.min(TotalChars, (int) Math.floor(Shown + Extra));
    }

    private void layout(Font Shape) {
        Lines.clear();
        String Text = Pages.get(Page).getString();
        for (FormattedText Line : Shape.getSplitter().splitLines(Text, TextWidth, Style.EMPTY)) {
            Lines.add(Line.getString());
        }
        while (Lines.size() > TextRows) {
            Lines.removeLast();
        }
        TotalChars = 0;
        for (String Line : Lines) {
            TotalChars += Line.length();
        }
    }

    private char charAt(int Index) {
        int Remaining = Index;
        for (String Line : Lines) {
            if (Remaining < Line.length()) {
                return Line.charAt(Remaining);
            }
            Remaining -= Line.length();
        }
        return ' ';
    }

    public void begin() {
        if (Page < 0 && !Pages.isEmpty()) {
            Page = 0;
            Shown = 0.0f;
            Pause = PageGap;
            layout(Minecraft.getInstance().font);
        }
    }

    private static boolean letter(char Value) {
        return Character.isLetterOrDigit(Value) || Value == '\u2588';
    }

    private static boolean stop(char Value) {
        return Value == '.' || Value == '!' || Value == '?';
    }

    private int wordLeft(int Index) {
        int Left = 0;
        for (int Scan = Index + 1; Scan < TotalChars && letter(charAt(Scan)); Scan++) {
            Left++;
        }
        return Left;
    }

    private char ending(int Index) {
        int Scan = Index + 1;
        while (Scan < TotalChars && (letter(charAt(Scan)) || charAt(Scan) == '\'')) {
            Scan++;
        }
        char Next = Scan < TotalChars ? charAt(Scan) : '.';
        return stop(Next) ? Next : ' ';
    }

    private float share(int Index) {
        int Start = Index;
        while (Start > 0 && !stop(charAt(Start - 1))) {
            Start--;
        }
        int End = Index;
        while (End < TotalChars - 1 && !stop(charAt(End))) {
            End++;
        }
        return End <= Start ? 0.0f : (Index - Start) / (float) (End - Start);
    }

    private void speak(int Index, int Length) {
        char Mark = ending(Index);
        float Pitch = 1.02f - Drift * share(Index);
        float Loudness = 1.0f;
        int Hold = Length <= 3 ? 2 : 3;
        if (Mark == '?') {
            Pitch *= Rise;
            Hold = 4;
        } else if (Mark != ' ') {
            Pitch *= Fall;
            Loudness = 0.85f;
            Hold = 4;
        }
        if (charAt(Index) == '\u2588') {
            Pitch *= 0.88f;
            Hold = 2;
        }
        Voice.syllable(Pitch, Loudness, Hold);
        Mouth = Hold + 1;
    }

    public void tick() {
        Age++;
        if (Mouth > 0) {
            Mouth--;
        }
        if (Page < 0 || Finished) {
            return;
        }
        if (pageDone()) {
            if (!Closed) {
                Closed = true;
                Voice.click();
            }
            Idle++;
            if (Auto && Idle >= AutoBase + (int) (TotalChars * AutoPerChar)) {
                advance();
            }
            return;
        }
        if (Pause > 0) {
            Pause--;
            return;
        }
        if (!Opened) {
            Opened = true;
            Voice.click();
        }
        float Before = Shown;
        Shown = Math.min(TotalChars, Shown + CharsPerTick);
        for (int Index = (int) Before; Index < (int) Shown; Index++) {
            char Letter = charAt(Index);
            if (letter(Letter)) {
                boolean Starts = Index == 0 || !letter(charAt(Index - 1));
                int Left = wordLeft(Index);
                if (Starts) {
                    Letters = 0;
                    Gap = 3 + Random.nextInt(2);
                    speak(Index, Left + 1);
                } else if (++Letters >= Gap && Left >= 2) {
                    Letters = 0;
                    Gap = 3 + Random.nextInt(2);
                    speak(Index, Left + 1);
                }
            }
            if (stop(Letter)) {
                Pause = SentencePause;
                Shown = Index + 1;
                break;
            }
            if (Letter == ',' || Letter == ';' || Letter == ':') {
                Pause = CommaPause;
                Shown = Index + 1;
                break;
            }
        }
    }

    public boolean advance() {
        if (Page < 0 || Finished) {
            return false;
        }
        Idle = 0;
        if (!pageDone()) {
            Shown = TotalChars;
            Pause = 0;
            return false;
        }
        if (Page < Pages.size() - 1) {
            Page++;
            Shown = 0.0f;
            Pause = PageGap;
            Letters = 0;
            Opened = false;
            Closed = false;
            layout(Minecraft.getInstance().font);
            return true;
        }
        Finished = true;
        return true;
    }
}

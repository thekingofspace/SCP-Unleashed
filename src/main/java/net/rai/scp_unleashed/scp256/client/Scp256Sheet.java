package net.rai.scp_unleashed.scp256.client;

import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp256.Scp256Schedule;
import net.rai.scp_unleashed.scp256.Scp256TypewriterBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class Scp256Sheet {
    public static final float Travel = 5.0f;
    public static final float TextScale = 0.1f;
    public static final float StepTicks = 1.5f;
    public static final float StrikeTicks = 2.5f;
    public static final float PressTicks = 2.0f;
    public static final float StrikeAngle = 10.0f;
    public static final float RestAngle = -40.0f;
    public static final int KeyCount = 14;
    public static final String Layout = "qwertyuiopasdfghjklzxcvbnm";

    private Scp256Sheet() {
    }

    public record Row(String Text, float Slot) {
    }

    public record Frame(float Offset, List<Row> Rows, int Key, float KeyDepth, float SpaceDepth, float Typebar, boolean Striking) {
        public static final Frame Idle = new Frame(0.0f, List.of(), -1, 0.0f, 0.0f, RestAngle, false);
    }

    public static float ease(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static int keyFor(char Typed) {
        char Lower = Character.toLowerCase(Typed);
        int Index = Layout.indexOf(Lower);
        return Math.floorMod(Index >= 0 ? Index * 5 + 3 : Lower * 7, KeyCount);
    }

    private static float lineOffset(Font Text, Scp256Schedule Plan, int Stroke) {
        if (Stroke < 0) {
            return -Travel;
        }
        String Line = Plan.getRows().get(Plan.rowOf(Stroke));
        return -Travel + Text.width(Line.substring(0, Plan.columnOf(Stroke) + 1)) * TextScale;
    }

    public static float offset(Font Text, Scp256Schedule Plan, double Elapsed) {
        int Typed = Plan.typedAt(Elapsed);
        if (Typed == 0) {
            return Mth.lerp(ease((float) (Elapsed / Scp256Schedule.LeadIn)), 0.0f, -Travel);
        }
        int Last = Typed - 1;
        float End = lineOffset(Text, Plan, Last);
        if (Typed == Plan.strokes()) {
            float Back = (float) ((Elapsed - Plan.finalReturnAt()) / Scp256Schedule.ReturnTicks);
            if (Back > 0.0f) {
                return Mth.lerp(ease(Back), End, 0.0f);
            }
        }
        int Row = Plan.rowOf(Last);
        if (Row < Plan.returns() && Elapsed >= Plan.returnAt(Row) && Plan.columnOf(Last) == Plan.getRows().get(Row).length() - 1) {
            float Back = (float) ((Elapsed - Plan.returnAt(Row)) / Scp256Schedule.ReturnTicks);
            return Mth.lerp(ease(Back), End, -Travel);
        }
        float Before = Plan.columnOf(Last) == 0 ? -Travel : lineOffset(Text, Plan, Last - 1);
        float Step = (float) ((Elapsed - Plan.timeOf(Last)) / StepTicks);
        return Mth.lerp(ease(Step), Before, End);
    }

    public static float scroll(Scp256Schedule Plan, double Elapsed) {
        float Done = 0.0f;
        for (int Index = 0; Index < Plan.returns(); Index++) {
            Done += ease((float) ((Elapsed - Plan.returnAt(Index)) / Scp256Schedule.ReturnTicks));
        }
        if (Plan.strokes() > 0) {
            Done += ease((float) ((Elapsed - Plan.finalReturnAt()) / Scp256Schedule.ReturnTicks));
        }
        return Done;
    }

    public static List<Row> rows(Scp256Schedule Plan, int Typed, float Scroll) {
        List<Row> Result = new ArrayList<>();
        if (Typed <= 0) {
            return Result;
        }
        int Last = Typed - 1;
        int CurrentRow = Plan.rowOf(Last);
        for (int Index = 0; Index <= CurrentRow; Index++) {
            String Line = Plan.getRows().get(Index);
            String Shown = Index < CurrentRow ? Line : Line.substring(0, Plan.columnOf(Last) + 1);
            Result.add(new Row(Shown, Scroll - Index));
        }
        return Result;
    }

    public static Frame frame(Font Text, Scp256TypewriterBlockEntity Typewriter, double Elapsed) {
        Scp256Schedule Plan = Typewriter.schedule();
        if (Plan == null) {
            return Frame.Idle;
        }
        if (Elapsed >= Typewriter.getDuration()) {
            return new Frame(0.0f, rows(Plan, Plan.strokes(), scroll(Plan, Typewriter.getDuration())), -1, 0.0f, 0.0f, RestAngle, false);
        }
        int Typed = Plan.typedAt(Elapsed);
        float Offset = offset(Text, Plan, Elapsed);
        List<Row> Shown = rows(Plan, Typed, scroll(Plan, Elapsed));
        int Key = -1;
        float KeyDepth = 0.0f;
        float SpaceDepth = 0.0f;
        float Typebar = RestAngle;
        boolean Striking = false;
        if (Typed > 0) {
            int Last = Typed - 1;
            float Since = (float) (Elapsed - Plan.timeOf(Last));
            char Struck = Plan.charOf(Last);
            float Press = 1.0f - Mth.clamp(Since / PressTicks, 0.0f, 1.0f);
            if (Struck == ' ') {
                SpaceDepth = Press;
            } else {
                Key = keyFor(Struck);
                KeyDepth = Press;
                if (Since < StrikeTicks) {
                    Striking = true;
                    Typebar = Mth.lerp(ease(Since / StrikeTicks), StrikeAngle, RestAngle);
                }
            }
        }
        return new Frame(Offset, Shown, Key, KeyDepth, SpaceDepth, Typebar, Striking);
    }
}

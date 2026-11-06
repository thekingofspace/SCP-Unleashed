package net.rai.scp_unleashed.scp420j;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.control.ControlSessions;

import java.util.ArrayList;
import java.util.List;

public final class Scp420jChatter {
    public static final String LineKey = "chat.scp_unleashed.scp420j.line.";
    public static final int LineCount = 30;
    public static final int MinLines = 2;
    public static final int MaxLines = 4;
    public static final int FirstEarliest = 40;
    public static final int FirstLatest = 60;
    public static final int MinGap = 60;
    public static final int LastLine = Scp420jFloat.TrackTicks - 25;

    private Scp420jChatter() {
    }

    public record Plan(List<Integer> Lines, List<Integer> Times) {
    }

    public static Plan plan(int Session) {
        RandomSource Dice = RandomSource.create(Session);
        int Count = MinLines + Dice.nextInt(MaxLines - MinLines + 1);
        List<Integer> Pool = new ArrayList<>();
        for (int Index = 0; Index < LineCount; Index++) {
            Pool.add(Index);
        }
        for (int Index = Pool.size() - 1; Index > 0; Index--) {
            int Other = Dice.nextInt(Index + 1);
            Integer Held = Pool.get(Index);
            Pool.set(Index, Pool.get(Other));
            Pool.set(Other, Held);
        }
        int First = FirstEarliest + Dice.nextInt(FirstLatest - FirstEarliest + 1);
        int Slot = (LastLine - First) / (Count - 1);
        List<Integer> Times = new ArrayList<>();
        Times.add(First);
        for (int Index = 1; Index < Count; Index++) {
            Times.add(First + Index * Slot - Dice.nextInt(Math.max(1, Slot - MinGap + 1)));
        }
        return new Plan(List.copyOf(Pool.subList(0, Count)), List.copyOf(Times));
    }

    public static int due(int Session, int Said, int Elapsed) {
        if (Said >= MaxLines) {
            return -1;
        }
        Plan Schedule = plan(Session);
        if (Said < Schedule.Lines().size() && Elapsed >= Schedule.Times().get(Said)) {
            return Schedule.Lines().get(Said);
        }
        return -1;
    }

    public static Component speakerName(ServerPlayer User) {
        if (ControlSessions.isControlling(User)) {
            String Name = ControlSessions.session(User).Identity().getString("ControlName");
            if (!Name.isBlank()) {
                return Component.literal(Name);
            }
        }
        return User.getDisplayName();
    }

    public static Component line(ServerPlayer User, int Line) {
        return Component.translatable("chat.type.text", speakerName(User), Component.translatable(LineKey + Line));
    }

    public static void say(ServerPlayer User, int Line) {
        if (User.server == null) {
            return;
        }
        User.server.getPlayerList().broadcastSystemMessage(line(User, Line), false);
    }
}

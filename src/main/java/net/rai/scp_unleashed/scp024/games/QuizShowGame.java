package net.rai.scp_unleashed.scp024.games;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class QuizShowGame implements Scp024Game {
    public static final int Pool = 16;
    public static final int Needed = 3;
    public static final int AnswerTicks = 300;
    public static final int NextDelay = 40;
    public static final int FallGrace = 50;
    public static final String[] Letters = {"A", "B", "C"};
    public static final ChatFormatting[] Colours = {ChatFormatting.RED, ChatFormatting.YELLOW, ChatFormatting.BLUE};
    private final List<Integer> Order = new ArrayList<>();
    private final List<Entity> Board = new ArrayList<>();
    private int Correct;
    private int Asked;
    private int Right;
    private int Deadline;
    private int Next = -1;
    private int Dropped = -1;
    private int Answer = -1;

    @Override
    public String id() {
        return "quiz_show";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.quizShow(Seed);
    }

    @Override
    public int seconds() {
        return 0;
    }

    @Override
    public double killDepth() {
        return -3.0;
    }

    @Override
    public void start(Scp024Session Session) {
        for (int Index = 0; Index < Pool; Index++) {
            Order.add(Index);
        }
        Collections.shuffle(Order, new Random(Session.Seed));
        ask(Session);
    }

    private void clearBoard() {
        for (Entity Line : Board) {
            Line.discard();
        }
        Board.clear();
    }

    private void line(Scp024Session Session, Component Text, double Y, float Scale, int Width) {
        int[] Mark = Session.mark("board");
        Vec3 Spot = new Vec3(Session.Origin.getX() + Mark[0] + 0.5, Session.Origin.getY() + Y, Session.Origin.getZ() + 0.05);
        Entity Display = Scp024Hazards.display(Session.World, "minecraft:text_display", Spot, Scp024Hazards.textDisplay(Session.World, Text, Scale, Width, 0x00000000, 0.0f));
        if (Display != null) {
            Session.spawn(Display);
            Board.add(Display);
        }
    }

    private void ask(Scp024Session Session) {
        clearBoard();
        int Question = Order.get(Asked % Order.size());
        Asked++;
        List<String> Keys = new ArrayList<>(List.of("a", "b", "c"));
        Collections.shuffle(Keys, new Random(Session.Seed + Asked * 31L));
        Correct = Keys.indexOf("a");
        String Base = "quiz.scp_unleashed.scp024." + Question;
        line(Session, Component.translatable(Base + ".q").withStyle(ChatFormatting.WHITE), 5.6, 0.75f, 200);
        for (int Slot = 0; Slot < 3; Slot++) {
            Component Text = Component.literal(Letters[Slot] + ": ").withStyle(Colours[Slot], ChatFormatting.BOLD)
                    .append(Component.translatable(Base + "." + Keys.get(Slot)).withStyle(ChatFormatting.WHITE));
            line(Session, Text, 4.65 - Slot * 0.6, 0.7f, 240);
        }
        Deadline = Session.ticks() + AnswerTicks;
        Answer = -1;
        Session.cue(Scp024Cue.Tick);
    }

    @Override
    public boolean use(Scp024Session Session, BlockPos Local) {
        List<int[]> Buttons = Session.marks("answer");
        for (int Slot = 0; Slot < Buttons.size(); Slot++) {
            int[] Mark = Buttons.get(Slot);
            if (Local.getX() == Mark[0] && Local.getY() == Mark[1] && Local.getZ() == Mark[2] && Answer < 0 && Next < 0 && Dropped < 0) {
                Answer = Slot;
            }
        }
        return false;
    }

    private void drop(Scp024Session Session) {
        int[] Door = Session.mark("trapdoor");
        BlockState State = Session.state(Door[0], Door[1], Door[2]);
        if (State.hasProperty(TrapDoorBlock.OPEN)) {
            Session.set(Door[0], Door[1], Door[2], State.setValue(TrapDoorBlock.OPEN, true));
        }
        Session.sound(Session.centre(Door), Scp024Registry.Trapdoor.get(), 1.0f, 0.7f);
        Session.cue(Scp024Cue.Buzzer);
        Dropped = Session.ticks();
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Dropped >= 0) {
            return Session.ticks() - Dropped > FallGrace ? Scp024Outcome.Lose : Scp024Outcome.Continue;
        }
        if (Next >= 0) {
            if (Session.ticks() >= Next) {
                Next = -1;
                ask(Session);
            }
            return Scp024Outcome.Continue;
        }
        if (Answer >= 0) {
            if (Answer == Correct) {
                Right++;
                if (Right >= Needed) {
                    clearBoard();
                    return Scp024Outcome.Win;
                }
                Session.cheer("correct");
                Next = Session.ticks() + NextDelay;
            } else {
                drop(Session);
            }
            return Scp024Outcome.Continue;
        }
        int Left = Deadline - Session.ticks();
        if (Left <= 0) {
            if (User != null) {
                Session.taunt("timeout");
            }
            drop(Session);
            return Scp024Outcome.Continue;
        }
        if (Left % 20 == 0 && Left <= 100) {
            Session.cue(Scp024Cue.Tick);
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.quiz", Right, Needed, (Left + 19) / 20));
        return Scp024Outcome.Continue;
    }

    @Override
    public void cleanup(Scp024Session Session) {
        clearBoard();
    }
}

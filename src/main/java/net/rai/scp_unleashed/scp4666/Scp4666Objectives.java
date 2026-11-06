package net.rai.scp_unleashed.scp4666;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp4666.block.Scp4666StoveBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp4666Objectives {
    public static final int Done = 0;
    public static final int Pending = 1;
    public static final int Urgent = 2;
    public static final int MaxLines = 12;

    public enum Task {
        Freezing, Gear, Hearth, Firewood, Boiler, Windows, Sleep, Bread, Gift, Calendar
    }

    public record Line(Task Kind, int Status, int Value, int Step) {
        public int pack() {
            return Kind.ordinal() & 15 | (Status & 3) << 4 | (Step & 3) << 6 | (Math.max(0, Math.min(255, Value)) & 255) << 8;
        }

        public static Line unpack(int Packed) {
            Task[] Kinds = Task.values();
            int Index = Packed & 15;
            return new Line(Kinds[Math.min(Index, Kinds.length - 1)], Packed >> 4 & 3, Packed >> 8 & 255, Packed >> 6 & 3);
        }
    }

    public record Facts(int Night, int Phase, boolean Spectator, int Fire, int Boiler, int Cracked, float Chill, int Firewood, boolean Scuttle, int Boards,
                        boolean GiftUsed, boolean GiftCarried, boolean Offering, boolean Oats, int Bread, int Oven, boolean CalendarOpened, boolean Sleeping,
                        boolean Bedless, boolean Outside, boolean Coat) {
    }

    private Scp4666Objectives() {
    }

    public static boolean hidden(Facts Known) {
        return Known.Spectator() || Known.Phase() == Scp4666State.PhaseEnding || Known.Phase() == Scp4666State.PhaseOver;
    }

    public static List<Line> compute(Facts Known) {
        List<Line> Found = new ArrayList<>();
        if (hidden(Known)) {
            return Found;
        }
        if (Known.Chill() >= Scp4666Cold.ChillSlow) {
            Found.add(new Line(Task.Freezing, Urgent, Math.round(Known.Chill()), 0));
        }
        if (Known.Outside() && !Known.Coat()) {
            Found.add(new Line(Task.Gear, Pending, 0, 0));
        }
        int Fire = Known.Fire();
        Found.add(new Line(Task.Hearth, Fire <= 1 ? Urgent : Fire == 2 ? Pending : Done, Fire, 0));
        if (Fire <= 2 && Known.Firewood() == 0) {
            Found.add(new Line(Task.Firewood, Fire <= 1 ? Urgent : Pending, 0, 0));
        }
        int Heat = Known.Boiler();
        Found.add(new Line(Task.Boiler, Heat <= 1 ? Pending : Done, Heat, Heat <= 1 && Known.Scuttle() ? 1 : 0));
        if (Known.Cracked() > 0) {
            Found.add(new Line(Task.Windows, Pending, Known.Cracked(), Known.Boards() >= Scp4666Chores.BoardsPerWindow ? 1 : 0));
        }
        boolean Late = Known.Phase() == Scp4666State.PhaseMidnight || Known.Phase() == Scp4666State.PhaseWoken;
        if (Late && !Known.Sleeping() && !Known.Bedless()) {
            Found.add(new Line(Task.Sleep, Pending, Known.Night() >= Scp4666State.FinalNight ? 1 : 0, 0));
        }
        if (Known.Night() >= Scp4666State.FoodStops) {
            Found.add(bread(Known));
        }
        if (Known.Offering()) {
            Found.add(new Line(Task.Gift, Done, 0, 0));
        } else if (Known.GiftCarried()) {
            Found.add(new Line(Task.Gift, Pending, 0, 1));
        } else if (!Known.GiftUsed()) {
            Found.add(new Line(Task.Gift, Pending, 0, 0));
        }
        Found.add(new Line(Task.Calendar, Known.CalendarOpened() ? Done : Pending, 0, 0));
        Found.sort(Comparator.comparingInt((Line Each) -> -Each.Status()).thenComparingInt(Each -> Each.Kind().ordinal()));
        return Found.size() > MaxLines ? new ArrayList<>(Found.subList(0, MaxLines)) : Found;
    }

    private static Line bread(Facts Known) {
        if (Known.Oven() == 2) {
            return new Line(Task.Bread, Pending, 0, 3);
        }
        if (Known.Oven() == 1) {
            return new Line(Task.Bread, Pending, 0, 2);
        }
        if (Known.Bread() > 0) {
            return new Line(Task.Bread, Done, Known.Bread(), 0);
        }
        return new Line(Task.Bread, Pending, 0, Known.Oats() ? 1 : 0);
    }

    public static Facts facts(ServerLevel World, Scp4666Map Layout, Scp4666State State, ServerPlayer User) {
        BlockState Stove = World.getBlockState(Layout.Stove);
        int Oven = Stove.getBlock() instanceof Scp4666StoveBlock ? Stove.getValue(Scp4666StoveBlock.Oven) : 0;
        return new Facts(State.night(), State.phase(), User.isSpectator(), Scp4666Heat.fireLevel(State), Scp4666Heat.boilerLevel(State), State.cracked().size(),
                Scp4666Cold.chill(User), Scp4666Chores.carried(User, Scp4666Registry.Firewood.get()), Scp4666Chores.carried(User, Scp4666Registry.Scuttle.get()) > 0,
                Scp4666Chores.carried(User, Scp4666Registry.Boards.get()), State.daily("gift:" + User.getUUID()), Scp4666Chores.carried(User, Scp4666Registry.Gift.get()) > 0,
                State.offering(), Scp4666Chores.carried(User, Scp4666Registry.Oats.get()) > 0, Scp4666Chores.carried(User, Scp4666Registry.OatBread.get()), Oven,
                State.daily("calendar:" + User.getUUID()), User.isSleeping(), State.isCold(User.getUUID()), !Layout.insideCabin(User.position()),
                User.getItemBySlot(EquipmentSlot.CHEST).is(Scp4666Registry.WinterCoat.get()));
    }

    public static List<Integer> packed(List<Line> Lines) {
        List<Integer> Packed = new ArrayList<>();
        for (Line Each : Lines) {
            Packed.add(Each.pack());
        }
        return Packed;
    }

    public static List<Integer> forPlayer(ServerLevel World, Scp4666Map Layout, Scp4666State State, ServerPlayer User) {
        if (!State.started()) {
            return List.of();
        }
        return packed(compute(facts(World, Layout, State, User)));
    }
}

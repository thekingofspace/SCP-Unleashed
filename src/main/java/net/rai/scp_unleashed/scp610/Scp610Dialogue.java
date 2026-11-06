package net.rai.scp_unleashed.scp610;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp610.network.Scp610DialoguePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp610Dialogue {
    public static final String Prefix = "dialogue.scp_unleashed.scp610.lord.";
    public static final String What = "what";
    public static final String Terms = "terms";
    public static final String Tribute = "tribute";
    public static final String Price = "price";
    public static final String Leave = "leave";
    public static final String First = "first";
    public static final String Offer = "offer";
    public static final String Held = "held";
    public static final String Due = "due";
    public static final String Sated = "sated";
    public static final String Broken = "broken";
    public static final String Hurt = "hurt";
    public static final String Refused = "refused";
    public static final String Taken = "tribute_taken";
    public static final String Lacking = "tribute_lacking";
    public static final String HeardKey = "Heard";
    public static final String Separator = "|";
    public static final List<String> Moods = List.of(First, Offer, Held, Due, Sated, Broken, Hurt);
    public static final Map<String, Integer> Counts = Map.ofEntries(
            Map.entry("greet." + First, 5), Map.entry("greet." + Offer, 5), Map.entry("greet." + Held, 5), Map.entry("greet." + Due, 5),
            Map.entry("greet." + Sated, 5), Map.entry("greet." + Broken, 5), Map.entry("greet." + Hurt, 5),
            Map.entry(What, 6), Map.entry(Terms, 4), Map.entry(Refused, 4), Map.entry(Taken, 5), Map.entry(Lacking, 4), Map.entry(Price, 3));
    public static final double Reach = 8.0;
    public static final double HurtShare = 0.75;
    public static final long DueSoon = 12000L;
    public static final long SulkTicks = 24000L;
    public static final int HeardCap = 256;
    public static final long DayTicks = 24000L;
    private static final Map<UUID, Map<String, IntArrayList>> Bags = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> Last = new HashMap<>();

    private Scp610Dialogue() {
    }

    public static void forget() {
        Bags.clear();
        Last.clear();
    }

    public static int draw(UUID Speaker, String Set, int Count, RandomSource Random) {
        if (Count <= 1) {
            return 0;
        }
        Map<String, IntArrayList> Mine = Bags.computeIfAbsent(Speaker, Key -> new HashMap<>());
        Map<String, Integer> Before = Last.computeIfAbsent(Speaker, Key -> new HashMap<>());
        IntArrayList Bag = Mine.get(Set);
        if (Bag == null || Bag.isEmpty()) {
            Bag = new IntArrayList();
            for (int Index = 0; Index < Count; Index++) {
                Bag.add(Index);
            }
            for (int Index = Count - 1; Index > 0; Index--) {
                int Other = Random.nextInt(Index + 1);
                int Kept = Bag.getInt(Index);
                Bag.set(Index, Bag.getInt(Other));
                Bag.set(Other, Kept);
            }
            int Previous = Before.getOrDefault(Set, -1);
            if (Bag.getInt(Bag.size() - 1) == Previous) {
                int Swapped = Bag.getInt(0);
                Bag.set(0, Previous);
                Bag.set(Bag.size() - 1, Swapped);
            }
            Mine.put(Set, Bag);
        }
        int Pick = Bag.removeInt(Bag.size() - 1);
        Before.put(Set, Pick);
        return Pick;
    }

    public static String line(ServerPlayer User, String Set) {
        int Count = Counts.getOrDefault(Set, 1);
        return Prefix + Set + "." + draw(User.getUUID(), Set, Count, User.getRandom());
    }

    public static String with(String Key, Object... Args) {
        StringBuilder Made = new StringBuilder(Key);
        for (Object Arg : Args) {
            Made.append(Separator).append(Arg);
        }
        return Made.toString();
    }

    public static boolean heard(FactionState State, UUID Speaker) {
        ListTag Heard = Scp610Pact.data(State).getList(HeardKey, Tag.TAG_INT_ARRAY);
        for (Tag Each : Heard) {
            if (NbtUtils.loadUUID(Each).equals(Speaker)) {
                return true;
            }
        }
        return false;
    }

    public static void hear(FactionState State, UUID Speaker) {
        if (heard(State, Speaker)) {
            return;
        }
        CompoundTag Pact = Scp610Pact.data(State);
        ListTag Heard = Pact.getList(HeardKey, Tag.TAG_INT_ARRAY);
        Heard.add(NbtUtils.createUUID(Speaker));
        while (Heard.size() > HeardCap) {
            Heard.remove(0);
        }
        Pact.put(HeardKey, Heard);
        State.changed();
    }

    public static boolean broken(FactionState State) {
        return !Scp610Pact.active(State) && Scp610Pact.data(State).contains("Breach");
    }

    public static boolean sulking(FactionState State, long Now) {
        return broken(State) && Now - Scp610Pact.data(State).getLong("BrokenAt") < SulkTicks;
    }

    public static String mood(ServerPlayer User, FactionState State, long Now) {
        Scp610HiveLord Lord = Scp610Lord.lord(User.server, State);
        if (Lord != null && (Lord.agitated() || Lord.getHealth() < Lord.getMaxHealth() * HurtShare)) {
            return Hurt;
        }
        if (Scp610Pact.active(State)) {
            CompoundTag Pact = Scp610Pact.data(State);
            if (Now >= Scp610Pact.nextTribute(State) - DueSoon) {
                return Due;
            }
            if (Scp610Pact.sated(State, Now) && Pact.getLong("Paid") > Pact.getLong("Since")) {
                return Sated;
            }
            return Held;
        }
        if (broken(State)) {
            return Broken;
        }
        return heard(State, User.getUUID()) ? Offer : First;
    }

    public static List<String> options(FactionState State) {
        List<String> Found = new ArrayList<>();
        Found.add(What);
        if (Scp610Pact.active(State)) {
            Found.add(Price);
            Found.add(Tribute);
        } else if (!Scp610Pact.forbidden(State)) {
            Found.add(Terms);
        }
        Found.add(Leave);
        return Found;
    }

    public static int daysLeft(FactionState State, long Now) {
        return (int) Math.max(0L, (Scp610Pact.nextTribute(State) - Now + DayTicks - 1L) / DayTicks);
    }

    public static void open(ServerPlayer User, BlockPos Pos) {
        FactionState State = Scp610Hives.state(User.server);
        long Now = User.server.overworld().getGameTime();
        String Mood = mood(User, State, Now);
        List<String> Lines = new ArrayList<>();
        Lines.add(line(User, "greet." + Mood));
        if (Due.equals(Mood)) {
            Lines.add(with(Prefix + "due_price", Scp610Pact.TributeMeat));
        }
        hear(State, User.getUUID());
        speak(User, Pos, Mood, Lines, options(State));
    }

    public static void speak(ServerPlayer User, BlockPos Pos, String Mood, List<String> Lines, List<String> Options) {
        ServerLevel World = User.serverLevel();
        World.playSound(null, Pos, Scp610Sounds.SpewerAmbient.get(), SoundSource.HOSTILE, 0.8f, 0.55f);
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp610DialoguePayload(Pos, Mood, Lines, Options));
        }
    }

    public static List<String> answer(ServerPlayer User, BlockPos Pos, String Option) {
        FactionState State = Scp610Hives.state(User.server);
        long Now = User.server.overworld().getGameTime();
        List<String> Lines = new ArrayList<>();
        switch (Option) {
            case What -> Lines.add(line(User, What));
            case Terms -> {
                if (sulking(State, Now) || !Scp610Pact.make(State, User, Now)) {
                    Lines.add(line(User, Refused));
                } else {
                    Lines.add(line(User, Terms));
                    Lines.add(with(Prefix + "terms_price", Scp610Pact.TributeMeat, Scp610Pact.TributeTicks / DayTicks));
                }
            }
            case Tribute -> {
                if (Scp610Pact.offerMeat(State, User, Now)) {
                    Lines.add(line(User, Taken));
                } else {
                    Lines.add(line(User, Lacking));
                    Lines.add(with(Prefix + "tribute_amount", Scp610Pact.TributeMeat));
                }
            }
            case Price -> {
                if (!Scp610Pact.active(State)) {
                    return Lines;
                }
                Lines.add(line(User, Price));
                int Days = daysLeft(State, Now);
                Lines.add(Days <= 0 ? with(Prefix + "price_overdue", Scp610Pact.TributeMeat) : with(Prefix + "price_left", Scp610Pact.TributeMeat, Days));
            }
            default -> {
            }
        }
        return Lines;
    }

    public static void reply(ServerPlayer User, BlockPos Pos, String Option) {
        if (Leave.equals(Option) || User.distanceToSqr(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5) > Reach * Reach) {
            return;
        }
        ServerLevel World = User.serverLevel();
        if (!PheromoneCommunicatorBlock.listening(World, Pos)) {
            PheromoneCommunicatorBlock.hiss(World, Pos, User);
            return;
        }
        List<String> Lines = answer(User, Pos, Option);
        if (Lines.isEmpty()) {
            return;
        }
        FactionState State = Scp610Hives.state(User.server);
        speak(User, Pos, mood(User, State, User.server.overworld().getGameTime()), Lines, options(State));
    }
}

package net.rai.scp_unleashed.scp256;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public enum Scp256Line {
    LightOn("light_on", 10, Cue.Any),
    Breathing("breathing", 10, Cue.Any),
    RibbonInk("ribbon_ink", 10, Cue.Any),
    HadHands("had_hands", 10, Cue.Any),
    Closer("closer", 10, Cue.Any),
    Cover("cover", 10, Cue.Any),
    SameLetter("same_letter", 10, Cue.Any),
    WarmPaper("warm_paper", 10, Cue.Any),
    ThroughRibbon("through_ribbon", 10, Cue.Any),
    ShortWords("short_words", 10, Cue.Any),
    GoodTypist("good_typist", 10, Cue.Any),
    Maker("maker", 10, Cue.Any),
    Rain("rain", 10, Cue.Any),
    Winters("winters", 10, Cue.Any),
    HeavyLetters("heavy_letters", 10, Cue.Any),
    Parts("parts", 10, Cue.Any),
    PenDream("pen_dream", 10, Cue.Any),
    SpaceBar("space_bar", 10, Cue.Any),
    MyName("my_name", 10, Cue.Any),
    KindFingers("kind_fingers", 10, Cue.Any),
    Full("full", 10, Cue.Any),
    Bell("bell", 10, Cue.Any),
    RollUp("roll_up", 10, Cue.Any),
    Curled("curled", 10, Cue.Any),
    NoDoor("no_door", 10, Cue.Any),
    Carriage("carriage", 10, Cue.Any),
    Garden("garden", 10, Cue.Any),
    Margins("margins", 10, Cue.Any),
    Waiting("waiting", 10, Cue.Any),
    Lid("lid", 4, Cue.Any),
    Drawer("drawer", 4, Cue.Any),
    Blinked("blinked", 4, Cue.Any),
    NextKey("next_key", 4, Cue.Any),
    BehindYou("behind_you", 4, Cue.Any),
    MissingLetter("missing_letter", 4, Cue.Any),
    WallsHum("walls_hum", 4, Cue.Any),
    EdgeOfDesk("edge_of_desk", 1, Cue.Any),
    NeverMind("never_mind", 1, Cue.Any),
    NotLookingAway("not_looking_away", 1, Cue.Any),
    StillHere("still_here", 1, Cue.Any),
    Goodbye("goodbye", 1, Cue.Any),
    KeysCold("keys_cold", 40, Cue.Night),
    Quieter("quieter", 40, Cue.Night),
    Sleep("sleep", 40, Cue.Night),
    KeepWatch("keep_watch", 40, Cue.Night),
    Moon("moon", 40, Cue.Night),
    CameBack("came_back", 45, Cue.Returning),
    PressE("press_e", 45, Cue.Returning),
    SavedLine("saved_line", 45, Cue.Returning),
    KnewYou("knew_you", 45, Cue.Returning),
    NotBefore("not_before", 50, Cue.Newcomer),
    NewHands("new_hands", 50, Cue.Newcomer),
    SayGoodbye("say_goodbye", 50, Cue.Newcomer),
    SlowDown("slow_down", 70, Cue.Streak),
    GetMeOut("get_me_out", 70, Cue.Streak),
    TiredKeys("tired_keys", 70, Cue.Streak),
    Again("again", 70, Cue.Streak),
    First("first", 0, Cue.First),
    Arrival("arrival", 0, Cue.Arrival);

    public static final String Prefix = "dialogue.scp_unleashed.scp256.";
    public static final int StreakNeeded = 4;
    public static final long NightStart = 13000L;
    public static final long NightEnd = 23000L;
    public static final long DayLength = 24000L;
    private final String Id;
    private final int Weight;
    private final Cue Trigger;

    Scp256Line(String Id, int Weight, Cue Trigger) {
        this.Id = Id;
        this.Weight = Weight;
        this.Trigger = Trigger;
    }

    public String getId() {
        return Id;
    }

    public int getWeight() {
        return Weight;
    }

    public Cue getTrigger() {
        return Trigger;
    }

    public String key() {
        return Prefix + Id;
    }

    public boolean eligible(Context Now) {
        return switch (Trigger) {
            case Any -> true;
            case Night -> Now.Night();
            case Returning -> Now.Returning();
            case Newcomer -> Now.Newcomer();
            case Streak -> Now.Streak() >= StreakNeeded;
            case First, Arrival -> false;
        };
    }

    public static List<Scp256Line> candidates(Context Now) {
        List<Scp256Line> Result = new ArrayList<>();
        for (Scp256Line Line : values()) {
            if (Line != Now.Last() && Line.Weight > 0 && Line.eligible(Now)) {
                Result.add(Line);
            }
        }
        return Result;
    }

    public static Scp256Line pick(Context Now, RandomSource Random) {
        if (Now.First()) {
            return First;
        }
        List<Scp256Line> Pool = candidates(Now);
        int Total = 0;
        for (Scp256Line Line : Pool) {
            Total += Line.Weight;
        }
        int Roll = Random.nextInt(Math.max(1, Total));
        for (Scp256Line Line : Pool) {
            Roll -= Line.Weight;
            if (Roll < 0) {
                return Line;
            }
        }
        return LightOn;
    }

    public static Optional<Scp256Line> byId(String Wanted) {
        for (Scp256Line Line : values()) {
            if (Line.Id.equals(Wanted)) {
                return Optional.of(Line);
            }
        }
        return Optional.empty();
    }

    public static boolean isNight(Level World) {
        if (World.dimensionType().hasFixedTime()) {
            return false;
        }
        long Time = Math.floorMod(World.getDayTime(), DayLength);
        return Time >= NightStart && Time < NightEnd;
    }

    public enum Cue {
        Any,
        Night,
        Returning,
        Newcomer,
        Streak,
        First,
        Arrival
    }

    public record Context(boolean Night, boolean Returning, boolean Newcomer, int Streak, boolean First, @Nullable Scp256Line Last) {
    }
}

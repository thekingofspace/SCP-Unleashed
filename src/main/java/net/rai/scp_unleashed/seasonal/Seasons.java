package net.rai.scp_unleashed.seasonal;

import net.minecraft.world.level.Level;
import net.neoforged.fml.util.thread.EffectiveSide;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Seasons {
    public static final Supplier<LocalDate> SystemClock = LocalDate::now;

    private static final Map<Season, SeasonMode> Forced = new EnumMap<>(Season.class);
    private static Supplier<LocalDate> Clock = SystemClock;
    private static volatile int ServerMask;
    private static volatile int ClientMask;

    private Seasons() {
    }

    public static boolean halloween() {
        return active(Season.Halloween);
    }

    public static boolean christmas() {
        return active(Season.Christmas);
    }

    public static boolean active(Season Which) {
        return Which.in(EffectiveSide.get().isServer() ? ServerMask : ClientMask);
    }

    public static boolean active(@Nullable Level World, Season Which) {
        if (World == null) {
            return active(Which);
        }
        return Which.in(World.isClientSide() ? ClientMask : ServerMask);
    }

    public static int serverMask() {
        return ServerMask;
    }

    public static int clientMask() {
        return ClientMask;
    }

    public static Set<Season> of(int Mask) {
        Set<Season> Found = EnumSet.noneOf(Season.class);
        for (Season Each : Season.values()) {
            if (Each.in(Mask)) {
                Found.add(Each);
            }
        }
        return Found;
    }

    public static boolean decide(Season Which, LocalDate Today, SeasonMode Mode) {
        return switch (Mode) {
            case On -> true;
            case Off -> false;
            case Auto -> Which.covers(Today);
        };
    }

    public static int decide(LocalDate Today, Function<Season, SeasonMode> Modes) {
        int Mask = 0;
        for (Season Each : Season.values()) {
            if (decide(Each, Today, Modes.apply(Each))) {
                Mask |= Each.bit();
            }
        }
        return Mask;
    }

    public static SeasonMode forced(Season Which) {
        synchronized (Forced) {
            return Forced.getOrDefault(Which, SeasonMode.Auto);
        }
    }

    public static SeasonMode mode(Season Which) {
        SeasonMode Session = forced(Which);
        return Session != SeasonMode.Auto ? Session : SeasonConfig.mode(Which);
    }

    public static void force(Season Which, SeasonMode Mode) {
        synchronized (Forced) {
            if (Mode == SeasonMode.Auto) {
                Forced.remove(Which);
            } else {
                Forced.put(Which, Mode);
            }
        }
    }

    public static LocalDate today() {
        return Clock.get();
    }

    public static void useClock(Supplier<LocalDate> Source) {
        Clock = Source;
    }

    public static void resetClock() {
        Clock = SystemClock;
    }

    public static int compute() {
        return decide(today(), Seasons::mode);
    }

    public static boolean refresh() {
        int Next = compute();
        if (Next == ServerMask) {
            return false;
        }
        ServerMask = Next;
        return true;
    }

    public static void clearSession() {
        synchronized (Forced) {
            Forced.clear();
        }
        ServerMask = 0;
    }

    public static void applyClient(int Mask) {
        ClientMask = Mask;
    }

    public static void resetClient() {
        ClientMask = 0;
    }
}

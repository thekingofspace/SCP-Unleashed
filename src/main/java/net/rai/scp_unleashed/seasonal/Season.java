package net.rai.scp_unleashed.seasonal;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

public enum Season {
    Halloween("halloween", MonthDay.of(Month.OCTOBER, 26), MonthDay.of(Month.OCTOBER, 31)),
    Christmas("christmas", MonthDay.of(Month.DECEMBER, 20), MonthDay.of(Month.DECEMBER, 26));

    private final String Key;
    private final MonthDay First;
    private final MonthDay Last;

    Season(String Key, MonthDay First, MonthDay Last) {
        this.Key = Key;
        this.First = First;
        this.Last = Last;
    }

    public String getKey() {
        return Key;
    }

    public MonthDay getFirst() {
        return First;
    }

    public MonthDay getLast() {
        return Last;
    }

    public String getLangKey() {
        return "season.scp_unleashed." + Key;
    }

    public String window() {
        return day(First) + " to " + day(Last);
    }

    private static String day(MonthDay Day) {
        return Day.getDayOfMonth() + " " + Day.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int Mask) {
        return (Mask & bit()) != 0;
    }

    public boolean covers(LocalDate Day) {
        MonthDay Today = MonthDay.from(Day);
        if (!First.isAfter(Last)) {
            return !Today.isBefore(First) && !Today.isAfter(Last);
        }
        return !Today.isBefore(First) || !Today.isAfter(Last);
    }

    public static Optional<Season> byKey(String Wanted) {
        for (Season Each : values()) {
            if (Each.Key.equals(Wanted)) {
                return Optional.of(Each);
            }
        }
        return Optional.empty();
    }
}

package net.rai.scp_unleashed.terminal;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.document.DocumentClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TerminalArchiveIndex {
    public static final int MaxQuery = 32;

    public record Row(ResourceLocation Id, String Designation, String Title, DocumentClass Classification, String ClassName, boolean Fresh) {
    }

    public record SortKey(int Group, long Number, String Suffix, String Path) implements Comparable<SortKey> {
        @Override
        public int compareTo(SortKey Other) {
            int Result = Integer.compare(Group, Other.Group);
            if (Result == 0) {
                Result = Long.compare(Number, Other.Number);
            }
            if (Result == 0) {
                Result = Suffix.compareTo(Other.Suffix);
            }
            return Result == 0 ? Path.compareTo(Other.Path) : Result;
        }
    }

    public static final Comparator<Row> Order = Comparator.comparing(Each -> key(Each.Id().getPath()));

    private TerminalArchiveIndex() {
    }

    public static SortKey key(String Path) {
        String Clean = Path == null ? "" : Path.toLowerCase(Locale.ROOT);
        int Group = 2;
        String Rest = Clean;
        if (Clean.startsWith("scp_")) {
            Group = 0;
            Rest = Clean.substring(4);
        } else if (Clean.startsWith("z_")) {
            Group = 1;
            Rest = Clean.substring(2);
        }
        int Digits = 0;
        while (Digits < Rest.length() && Character.isDigit(Rest.charAt(Digits)) && Digits < 18) {
            Digits++;
        }
        long Number = Digits == 0 ? Long.MAX_VALUE : Long.parseLong(Rest.substring(0, Digits));
        return new SortKey(Group, Number, Rest.substring(Digits), Clean);
    }

    public static List<Row> sorted(List<Row> Rows) {
        List<Row> Copy = new ArrayList<>(Rows);
        Copy.sort(Order);
        return Copy;
    }

    public static boolean matches(Row Each, DocumentClass Only, boolean NewOnly, String Query) {
        if (Only != null && Each.Classification() != Only) {
            return false;
        }
        if (NewOnly && !Each.Fresh()) {
            return false;
        }
        String Clean = Query == null ? "" : Query.strip().toLowerCase(Locale.ROOT);
        if (Clean.isEmpty()) {
            return true;
        }
        String Haystack = (Each.Designation() + " " + Each.Title() + " " + Each.Id().getPath() + " " + Each.ClassName()).toLowerCase(Locale.ROOT);
        String Compact = Haystack.replace("-", "").replace("_", "");
        for (String Token : Clean.split("\\s+")) {
            if (!Haystack.contains(Token) && !Compact.contains(Token.replace("-", "").replace("_", ""))) {
                return false;
            }
        }
        return true;
    }

    public static List<Row> filter(List<Row> Rows, DocumentClass Only, boolean NewOnly, String Query) {
        List<Row> Kept = new ArrayList<>();
        for (Row Each : Rows) {
            if (matches(Each, Only, NewOnly, Query)) {
                Kept.add(Each);
            }
        }
        return Kept;
    }

    public static int fresh(List<Row> Rows) {
        int Count = 0;
        for (Row Each : Rows) {
            if (Each.Fresh()) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean typable(char Typed, String Query) {
        return Query.length() < MaxQuery && Typed >= ' ' && Typed != 127 && !Character.isISOControl(Typed);
    }
}

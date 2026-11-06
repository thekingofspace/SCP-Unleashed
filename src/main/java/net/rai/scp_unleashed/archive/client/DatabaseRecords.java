package net.rai.scp_unleashed.archive.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DatabaseRecords {
    private static final Pattern Number = Pattern.compile("(\\d+)");
    private static List<DatabaseRecord> Records = List.of();
    private static int Revision;

    private DatabaseRecords() {
    }

    public static void accept(List<DatabaseRecord> Incoming) {
        Records = sorted(Incoming);
        Revision++;
    }

    public static void resort() {
        Records = sorted(Records);
    }

    private static List<DatabaseRecord> sorted(List<DatabaseRecord> Incoming) {
        List<DatabaseRecord> Sorted = new ArrayList<>(Incoming);
        Sorted.sort(Comparator.comparingLong(DatabaseRecords::number).thenComparing(Entry -> designation(Entry).toLowerCase(Locale.ROOT))
                .thenComparing(Entry -> Entry.Document().toString()));
        return List.copyOf(Sorted);
    }

    public static void clear() {
        Records = List.of();
        Revision++;
    }

    public static List<DatabaseRecord> records() {
        return Records;
    }

    public static int count() {
        return Records.size();
    }

    public static int revision() {
        return Revision;
    }

    public static Optional<DatabaseRecord> find(ResourceLocation Id) {
        return Records.stream().filter(Entry -> Entry.Document().equals(Id)).findFirst();
    }

    public static String designation(DatabaseRecord Entry) {
        AnomalyDocument Document = AnomalyDocuments.client().get(Entry.Document());
        return Document == null ? Entry.Document().getPath().toUpperCase(Locale.ROOT) : Document.designation().getString();
    }

    private static long number(DatabaseRecord Entry) {
        Matcher Found = Number.matcher(designation(Entry));
        if (!Found.find()) {
            return Long.MAX_VALUE;
        }
        try {
            return Long.parseLong(Found.group(1));
        } catch (NumberFormatException Problem) {
            return Long.MAX_VALUE;
        }
    }
}

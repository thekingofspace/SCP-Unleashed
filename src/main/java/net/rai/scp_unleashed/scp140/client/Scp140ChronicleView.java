package net.rai.scp_unleashed.scp140.client;

import net.rai.scp_unleashed.scp140.Scp140Mood;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;
import net.rai.scp_unleashed.scp140.network.Scp140ChroniclePayload;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class Scp140ChronicleView {
    private static int Written;
    private static float Progress;
    private static long SeenMask;
    private static int Mood;
    private static final Scp140ChroniclePayload.PageEntry[] Pages = new Scp140ChroniclePayload.PageEntry[Scp140Pages.Total];
    private static int Revision;

    private Scp140ChronicleView() {
    }

    public static void accept(Scp140ChroniclePayload Payload) {
        Written = Payload.Written();
        Progress = Math.max(0.0f, Math.min(1.0f, Payload.Progress()));
        SeenMask = Payload.SeenMask();
        Mood = Payload.Mood();
        Arrays.fill(Pages, null);
        for (Scp140ChroniclePayload.PageEntry Entry : Payload.Pages()) {
            if (Entry.Page() >= 1 && Entry.Page() <= Scp140Pages.Total) {
                Pages[Entry.Page() - 1] = Entry;
            }
        }
        Revision++;
    }

    public static void clear() {
        Written = 0;
        Progress = 0.0f;
        SeenMask = 0L;
        Mood = 0;
        Arrays.fill(Pages, null);
        Revision++;
    }

    public static int written() {
        return Written;
    }

    public static float progress() {
        return Progress;
    }

    public static Scp140Mood mood() {
        return Scp140Mood.byId(Mood);
    }

    public static int revision() {
        return Revision;
    }

    public static boolean isWritten(int Page) {
        return Page >= 1 && Page <= Written;
    }

    public static boolean wasSeen(int Page) {
        return Page >= 1 && Page <= Scp140Pages.Total && (SeenMask & (1L << (Page - 1))) != 0L;
    }

    public static void markSeen(int Page) {
        if (Page >= 1 && Page <= Scp140Pages.Total) {
            SeenMask |= 1L << (Page - 1);
        }
    }

    @Nullable
    public static Scp140ChroniclePayload.PageEntry page(int Page) {
        return Page >= 1 && Page <= Scp140Pages.Total ? Pages[Page - 1] : null;
    }
}

package net.rai.scp_unleashed.notice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

public final class NoticeStack {
    public static final int MaxVisible = 4;
    public static final int MaxQueued = 12;
    public static final int QueueExpiry = 600;
    public static final int SlideTicks = 6;
    public static final int FadeTicks = 10;
    public static final int BumpFadeTicks = 4;
    public static final int RequeueFloor = 40;
    public static final int Gap = 2;
    public static final int MinHeight = 24;
    public static final float SettleRate = 0.5f;
    public static final float SnapDistance = 0.6f;
    private static final Comparator<Card> Ranking = Comparator.<Card>comparingInt(Held -> -Held.Shown.Priority().ordinal()).thenComparingLong(Held -> Held.Order);

    private final List<Card> Active = new ArrayList<>();
    private final List<Card> Waiting = new ArrayList<>();
    private final List<Card> Fresh = new ArrayList<>();
    private long Sequence;
    private int Capacity = MaxVisible;

    public Card push(Notice Incoming) {
        for (Card Open : Active) {
            if (Open.Shown.sameAs(Incoming)) {
                if (!Open.isClosing() || Open.Requeue) {
                    Open.Life = Math.max(Open.Life, Open.Age + Incoming.Duration());
                }
                return Open;
            }
        }
        for (Card Queued : Waiting) {
            if (Queued.Shown.sameAs(Incoming)) {
                Queued.Waited = 0;
                Queued.Life = Math.max(Queued.Life, Queued.Age + Incoming.Duration());
                return Queued;
            }
        }
        Card Added = new Card(Incoming, Sequence++);
        Waiting.add(Added);
        trim();
        return Added;
    }

    public void clear() {
        Active.clear();
        Waiting.clear();
        Fresh.clear();
    }

    public boolean isEmpty() {
        return Active.isEmpty() && Waiting.isEmpty();
    }

    public void tick(boolean Paused) {
        for (Card Open : Active) {
            Open.PrevY = Open.Y;
        }
        if (Paused) {
            return;
        }
        Waiting.removeIf(Queued -> ++Queued.Waited > QueueExpiry && !Queued.Shown.Priority().isGuaranteed());
        for (Card Open : List.copyOf(Active)) {
            if (Open.isClosing()) {
                Open.Closing++;
                if (Open.Closing >= Open.ClosingLength) {
                    Active.remove(Open);
                    if (Open.Requeue) {
                        Open.reopen();
                        Waiting.add(Open);
                    }
                }
                continue;
            }
            Open.Age++;
            if (Open.Age >= Open.Life) {
                Open.close(FadeTicks, false);
            }
        }
        displace();
        promote();
        for (Card Open : Active) {
            Open.settle();
        }
    }

    public List<Card> layout(int Top, int Floor, ToIntFunction<Card> Height) {
        List<Card> Ordered = ordered();
        int Cursor = Top;
        int Used = 0;
        int Showing = 0;
        for (Card Open : Ordered) {
            Open.Height = Math.max(MinHeight, Height.applyAsInt(Open));
            if (!Open.isClosing()) {
                Used += Open.Height + Gap;
                Showing++;
            }
            Open.TargetY = Cursor;
            if (!Open.Placed) {
                Open.place(Cursor);
            }
            Cursor += Open.Height + Gap;
        }
        Capacity = Math.max(1, Math.min(MaxVisible, Showing + Math.max(0, Floor - Top - Used) / (MinHeight + Gap)));
        return Ordered;
    }

    public Frame frame(Card Open, float Partial) {
        float Raw = Open.PrevY + (Open.Y - Open.PrevY) * Partial;
        if (Open.TargetY >= Open.DrawnY) {
            Raw = Math.max(Open.DrawnY, Math.min(Open.TargetY, Raw));
        } else {
            Raw = Math.max(Open.TargetY, Math.min(Open.DrawnY, Raw));
        }
        Open.DrawnY = Raw;
        float Alpha = Open.isClosing() ? 1.0f - (Open.Closing + Partial) / Open.ClosingLength : 1.0f;
        Open.DrawnAlpha = Math.max(0.0f, Math.min(Open.DrawnAlpha, Alpha));
        float Progress = Open.isClosing() ? 1.0f : (Open.Age + Partial) / SlideTicks;
        Open.DrawnSlide = Math.min(1.0f, Math.max(Open.DrawnSlide, Progress));
        float Remaining = 1.0f - Open.DrawnSlide;
        return new Frame(Math.round(Open.DrawnY), Open.DrawnAlpha, 1.0f - Remaining * Remaining * Remaining);
    }

    public List<Card> ordered() {
        List<Card> Ordered = new ArrayList<>(Active);
        Ordered.sort(Ranking);
        return Ordered;
    }

    public List<Card> waiting() {
        return List.copyOf(Waiting);
    }

    public List<Card> takeFresh() {
        List<Card> Taken = List.copyOf(Fresh);
        Fresh.clear();
        return Taken;
    }

    public int capacity() {
        return Capacity;
    }

    private int openCount() {
        int Count = 0;
        for (Card Open : Active) {
            if (!Open.isClosing()) {
                Count++;
            }
        }
        return Count;
    }

    private static Card best(List<Card> Candidates) {
        Card Found = null;
        for (Card Candidate : Candidates) {
            if (Found == null || Ranking.compare(Candidate, Found) < 0) {
                Found = Candidate;
            }
        }
        return Found;
    }

    private void displace() {
        Card Next = best(Waiting);
        if (Next == null || Next.Pushed || openCount() < Capacity) {
            return;
        }
        Card Weakest = null;
        for (Card Open : Active) {
            if (!Open.isClosing() && (Weakest == null || Ranking.compare(Open, Weakest) > 0)) {
                Weakest = Open;
            }
        }
        if (Weakest != null && Next.Shown.Priority().outranks(Weakest.Shown.Priority())) {
            Next.Pushed = true;
            Weakest.Demotions++;
            Weakest.close(BumpFadeTicks, Weakest.Shown.Priority().isGuaranteed() || Weakest.Life - Weakest.Age > RequeueFloor);
        }
    }

    private void promote() {
        while (openCount() < Capacity) {
            Card Next = best(Waiting);
            if (Next == null) {
                return;
            }
            Waiting.remove(Next);
            Next.Placed = false;
            Next.Shows++;
            Active.add(Next);
            if (!Next.Announced) {
                Next.Announced = true;
                Fresh.add(Next);
            }
        }
    }

    private void trim() {
        while (Waiting.size() > MaxQueued) {
            Card Weakest = null;
            for (Card Queued : Waiting) {
                if (!Queued.Shown.Priority().isGuaranteed() && (Weakest == null || Ranking.compare(Queued, Weakest) > 0)) {
                    Weakest = Queued;
                }
            }
            if (Weakest == null) {
                return;
            }
            Waiting.remove(Weakest);
        }
    }

    public record Frame(int Y, float Alpha, float Slide) {
    }

    public static final class Card {
        private final Notice Shown;
        private final long Order;
        private int Age;
        private int Life;
        private int Closing = -1;
        private int ClosingLength = FadeTicks;
        private boolean Requeue;
        private boolean Pushed;
        private int Waited;
        private boolean Placed;
        private boolean Announced;
        private int Shows;
        private int Demotions;
        private int Height = MinHeight;
        private float Y;
        private float PrevY;
        private float TargetY;
        private float DrawnY;
        private float DrawnAlpha = 1.0f;
        private float DrawnSlide;

        private Card(Notice Shown, long Order) {
            this.Shown = Shown;
            this.Order = Order;
            this.Life = Shown.Duration();
        }

        public Notice getNotice() {
            return Shown;
        }

        public boolean isClosing() {
            return Closing >= 0;
        }

        public int getHeight() {
            return Height;
        }

        public float getTargetY() {
            return TargetY;
        }

        public int getShows() {
            return Shows;
        }

        public int getDemotions() {
            return Demotions;
        }

        public int getRemaining() {
            return Life - Age;
        }

        private void close(int Length, boolean Again) {
            Closing = 0;
            ClosingLength = Length;
            Requeue = Again;
        }

        private void reopen() {
            Life = Math.max(Life - Age, RequeueFloor);
            Age = 0;
            Closing = -1;
            Requeue = false;
            Waited = 0;
            Placed = false;
        }

        private void place(float Where) {
            Placed = true;
            Y = Where;
            PrevY = Where;
            TargetY = Where;
            DrawnY = Where;
            DrawnAlpha = 1.0f;
            DrawnSlide = 0.0f;
        }

        private void settle() {
            float Distance = TargetY - Y;
            Y = Math.abs(Distance) < SnapDistance ? TargetY : Y + Distance * SettleRate;
        }
    }
}

package net.rai.scp_unleashed.terminal;

import java.util.ArrayList;
import java.util.List;

public final class TerminalAux {
    public static final String ModConfig = "mod_config";
    public static final String ScpConfig = "scp_config";
    public static final String Archive = "archive";
    public static final List<String> Own = List.of(ModConfig, ScpConfig, Archive);

    public record Item(String Id, int Width, int Height, boolean Row) {
    }

    public record Spot(String Id, int X, int Y, int Width, int Height) {
        public boolean overlaps(Spot Other) {
            return X < Other.X + Other.Width && Other.X < X + Width && Y < Other.Y + Other.Height && Other.Y < Y + Height;
        }
    }

    private TerminalAux() {
    }

    public static List<String> order(List<String> Others) {
        List<String> Ids = new ArrayList<>(Own);
        for (String Each : Others) {
            if (!Ids.contains(Each)) {
                Ids.add(Each);
            }
        }
        return Ids;
    }

    public static List<Spot> place(List<Item> Items, int Left, int Top, int Limit, int ColumnW, int RowH, int ColumnGap, int IconMax, int IconGap) {
        List<Spot> Spots = new ArrayList<>();
        int ColumnX = Left;
        int Y = Top;
        List<Item> Icons = new ArrayList<>();
        for (Item Each : Items) {
            if (!Each.Row() && Each.Width() <= IconMax && Each.Height() <= IconMax) {
                Icons.add(Each);
                continue;
            }
            int Height = Each.Row() ? RowH - 1 : Each.Height();
            int Step = Each.Row() ? RowH : Each.Height() + 2;
            if (Y + Height > Limit && Y > Top) {
                ColumnX += ColumnW + ColumnGap;
                Y = Top;
            }
            Spots.add(Each.Row() ? new Spot(Each.Id(), ColumnX, Y, ColumnW, Height) : new Spot(Each.Id(), ColumnX + 3, Y, Each.Width(), Each.Height()));
            Y += Step;
        }
        int IconX = ColumnX + 3;
        int StripH = 0;
        for (Item Each : Icons) {
            if (IconX + Each.Width() > ColumnX + ColumnW && IconX > ColumnX + 3) {
                Y += StripH + 2;
                IconX = ColumnX + 3;
                StripH = 0;
            }
            if (Y + Each.Height() > Limit && Y > Top) {
                ColumnX += ColumnW + ColumnGap;
                Y = Top;
                IconX = ColumnX + 3;
                StripH = 0;
            }
            Spots.add(new Spot(Each.Id(), IconX, Y, Each.Width(), Each.Height()));
            IconX += Each.Width() + IconGap;
            StripH = Math.max(StripH, Each.Height());
        }
        return Spots;
    }
}

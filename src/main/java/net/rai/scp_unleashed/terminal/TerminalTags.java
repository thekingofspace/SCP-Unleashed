package net.rai.scp_unleashed.terminal;

import java.util.List;

public final class TerminalTags {
    public static final int Inner = 5;
    public static final int Width = Inner + 2;
    public static final int CellPixels = 6;
    public static final String Ok = "OK";
    public static final String Net = "NET";
    public static final String Env = "ENV";
    public static final String Sec = "SEC";
    public static final String Gen = "GEN";
    public static final String Dim = "DIM";
    public static final String Auth = "KEY";
    public static final String Sync = "SYN";
    public static final String Sys = "SYS";
    public static final String Map = "MAP";
    public static final String Save = "DSK";
    public static final String Fail = "ERR";
    public static final List<String> Labels = List.of(Ok, Net, Env, Sec, Gen, Dim, Auth, Sync, Sys, Map, Save, Fail);

    private TerminalTags() {
    }

    public static String tag(String Label) {
        String Clean = Label.length() > Inner ? Label.substring(0, Inner) : Label;
        int Left = (Inner - Clean.length()) / 2;
        int Right = Inner - Clean.length() - Left;
        return "[" + " ".repeat(Left) + Clean + " ".repeat(Right) + "]";
    }

    public static int labelOffset(int LabelPixels, int CellPixels) {
        return (Width * CellPixels - LabelPixels) / 2;
    }

    public static int closeOffset(int CellPixels) {
        return (Width - 1) * CellPixels;
    }

    public static int textOffset(int CellPixels) {
        return (Width + 1) * CellPixels;
    }

    public static String blank() {
        return "[" + " ".repeat(Inner) + "]";
    }

    public static String spinner(int Step) {
        int Span = Inner - 2;
        int Cycle = Span * 2 - 2;
        int Phase = Math.floorMod(Step, Math.max(1, Cycle));
        int At = Phase < Span ? Phase : Cycle - Phase;
        StringBuilder Inside = new StringBuilder(" ".repeat(Inner));
        Inside.setCharAt(1 + At, '*');
        return "[" + Inside + "]";
    }
}

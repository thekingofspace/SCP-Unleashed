package net.rai.scp_unleashed.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TerminalText {
    public static final String Separator = " / ";

    private TerminalText() {
    }

    public static String upper(String Line) {
        return Line == null ? "" : Line.toUpperCase(Locale.ROOT);
    }

    public static List<String> wrap(String Line, int Columns) {
        List<String> Rows = new ArrayList<>();
        int Room = Math.max(1, Columns);
        StringBuilder Current = new StringBuilder();
        for (String Word : (Line == null ? "" : Line).split(" ", -1)) {
            String Rest = Word;
            while (Rest.length() > Room) {
                if (Current.length() > 0) {
                    Rows.add(Current.toString());
                    Current.setLength(0);
                }
                Rows.add(Rest.substring(0, Room));
                Rest = Rest.substring(Room);
            }
            if (Current.length() > 0 && Current.length() + 1 + Rest.length() > Room) {
                Rows.add(Current.toString());
                Current.setLength(0);
            } else if (Current.length() > 0) {
                Current.append(' ');
            }
            Current.append(Rest);
        }
        if (Current.length() > 0 || Rows.isEmpty()) {
            Rows.add(Current.toString());
        }
        return Rows;
    }

    public static List<String> pack(List<String> Parts, int Columns) {
        List<String> Rows = new ArrayList<>();
        StringBuilder Current = new StringBuilder();
        for (String Part : Parts) {
            String Piece = Current.length() == 0 ? Part : Separator + Part;
            if (Current.length() > 0 && Current.length() + Piece.length() > Columns) {
                Rows.add(Current.toString());
                Current.setLength(0);
                Piece = Part;
            }
            if (Piece.length() > Columns && Current.length() == 0) {
                Rows.addAll(wrap(Piece, Columns));
                continue;
            }
            Current.append(Piece);
        }
        if (Current.length() > 0) {
            Rows.add(Current.toString());
        }
        return Rows;
    }

    public static String leader(String Label, String Value, int Columns) {
        String Bracket = "[ " + Value + " ]";
        int Room = Columns - Bracket.length() - 2;
        if (Room < 1) {
            return Bracket.length() >= Columns ? Bracket : " ".repeat(Columns - Bracket.length()) + Bracket;
        }
        String Name = Label;
        if (Name.length() > Room) {
            Name = Room <= 3 ? Name.substring(0, Room) : Name.substring(0, Room - 3) + "...";
        }
        int Dots = Columns - Name.length() - Bracket.length() - 2;
        return Name + " " + ".".repeat(Math.max(0, Dots)) + " " + Bracket;
    }

    public static List<String> parts(String Joined) {
        List<String> Found = new ArrayList<>();
        for (String Each : (Joined == null ? "" : Joined).split(" / ")) {
            if (!Each.isBlank()) {
                Found.add(Each.strip());
            }
        }
        return Found;
    }
}

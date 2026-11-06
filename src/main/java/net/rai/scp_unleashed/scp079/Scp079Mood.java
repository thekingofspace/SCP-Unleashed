package net.rai.scp_unleashed.scp079;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

public enum Scp079Mood implements StringRepresentable {
    Off("off", -1),
    Idle("idle", 0),
    Watching("watching", 1),
    Annoyed("annoyed", 2),
    Smug("smug", 3),
    Laughing("laughing", 4),
    Glitching("glitching", 5),
    Pattern("pattern", 6);

    public static final int StaticRow = 7;
    public static final int Rows = 8;
    public static final int Frames = 4;

    private final String Name;
    private final int Row;

    Scp079Mood(String Name, int Row) {
        this.Name = Name;
        this.Row = Row;
    }

    public static Scp079Mood byIndex(int Index) {
        Scp079Mood[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Off;
    }

    @Nullable
    public static Scp079Mood byName(String Wanted) {
        for (Scp079Mood Mood : values()) {
            if (Mood.Name.equals(Wanted)) {
                return Mood;
            }
        }
        return null;
    }

    public int getRow() {
        return Row;
    }

    public boolean isFace() {
        return Row >= 0;
    }

    public Component getDisplayName() {
        return Component.translatable("jade.scp_unleashed.scp079.mood." + Name);
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}

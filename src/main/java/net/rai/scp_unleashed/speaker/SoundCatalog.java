package net.rai.scp_unleashed.speaker;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SoundCatalog {
    public static final List<Entry> Entries = build();

    private SoundCatalog() {
    }

    private static List<Entry> build() {
        List<Entry> Found = new ArrayList<>();
        add(Found, Category.Facility, "alarm", "klaxon", "breach_alarm", "pa_chime", "announcement_start", "announcement_end", "door_buzzer",
                "decontamination", "generator_startup");
        add(Found, Category.Ambience, "ventilation", "distant_machinery", "static", "fluorescent_hum");
        add(Found, Category.Anomalous);
        return List.copyOf(Found);
    }

    private static void add(List<Entry> Found, Category Group, String... Names) {
        for (String Name : Names) {
            Found.add(new Entry(Group, Name, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "speaker." + Group.getSerializedName() + "." + Name)));
        }
    }

    public static Entry first() {
        return Entries.getFirst();
    }

    @Nullable
    public static Entry find(ResourceLocation Id) {
        for (Entry Candidate : Entries) {
            if (Candidate.Id().equals(Id)) {
                return Candidate;
            }
        }
        return null;
    }

    public static List<Entry> inCategory(Category Group) {
        List<Entry> Found = new ArrayList<>();
        for (Entry Candidate : Entries) {
            if (Candidate.Group() == Group) {
                Found.add(Candidate);
            }
        }
        return Found;
    }

    public enum Category {
        Facility("facility"),
        Ambience("ambience"),
        Anomalous("anomalous");

        private final String Name;

        Category(String Name) {
            this.Name = Name;
        }

        public String getSerializedName() {
            return Name;
        }

        public String getTranslationKey() {
            return "gui.scp_unleashed.sound_console.category." + Name;
        }
    }

    public record Entry(Category Group, String Name, ResourceLocation Id) {
        public boolean isAnomalous() {
            return Group == Category.Anomalous;
        }

        public String getTranslationKey() {
            return "gui.scp_unleashed.sound_console.sound." + Name;
        }

        public String getRegistryName() {
            return Id.getPath();
        }
    }
}

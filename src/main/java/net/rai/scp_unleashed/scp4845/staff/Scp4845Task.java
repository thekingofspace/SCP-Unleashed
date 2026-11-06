package net.rai.scp_unleashed.scp4845.staff;

import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public enum Scp4845Task {
    None(0, "idle", false, null),
    Register(1, "register", false, null),
    HandOver(2, "hand_over", false, "register", "prop_tray"),
    Wave(3, "wave", false, null),
    Grill(4, "grill", false, null, "prop_spatula"),
    Fry(5, "fry", false, null, "prop_basket"),
    Prep(6, "prep", false, null, "prop_tortilla"),
    Mop(7, "mop", false, null, "prop_mop"),
    Wipe(8, "wipe", false, null, "prop_rag"),
    Restock(9, "restock", false, null, "prop_box"),
    Wash(10, "wash", false, null, "prop_rag", "prop_dish"),
    Break(11, "break", true, null, "prop_taco", "prop_cup"),
    Chat(12, "chat", false, null),
    Type(13, "type", true, null),
    Sigh(14, "sigh", true, "type"),
    Stretch(15, "stretch", true, "type"),
    Glance(16, "glance", true, "type");

    public static final List<String> AllProps = List.of("prop_spatula", "prop_basket", "prop_mop", "prop_rag", "prop_tray", "prop_box", "prop_taco", "prop_cup",
            "prop_tortilla", "prop_dish");

    public final byte Id;
    public final String Clip;
    public final boolean Seated;
    public final String Follow;
    public final List<String> Props;
    public final RawAnimation Animation;

    Scp4845Task(int Id, String Clip, boolean Seated, String Follow, String... Props) {
        this.Id = (byte) Id;
        this.Clip = Clip;
        this.Seated = Seated;
        this.Follow = Follow;
        this.Props = List.of(Props);
        this.Animation = Follow == null ? RawAnimation.begin().thenLoop(Clip) : RawAnimation.begin().thenPlay(Clip).thenLoop(Follow);
    }

    public boolean working() {
        return this != None && this != Wave && this != Chat;
    }

    public static Scp4845Task byId(int Id) {
        for (Scp4845Task Entry : values()) {
            if (Entry.Id == Id) {
                return Entry;
            }
        }
        return None;
    }
}

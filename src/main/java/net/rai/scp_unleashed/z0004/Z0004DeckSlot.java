package net.rai.scp_unleashed.z0004;

import java.util.List;

public record Z0004DeckSlot(String Bone, int Variant, String Loop, String Prop, double Forward, double Left, double Up) {
    public static final int Captain = -1;
    public static final String NoProp = "";
    public static final List<Z0004DeckSlot> All = List.of(
            new Z0004DeckSlot("slot_haul", Z0004Pirate.Cutlass, "deck_haul", NoProp, 0.5625, -1.25, 1.875),
            new Z0004DeckSlot("slot_swab", Z0004Pirate.Hook, "deck_swab", "prop_mop", 3.0, 0.75, 1.875),
            new Z0004DeckSlot("slot_ram_port", Z0004Pirate.Pistol, "deck_ram", "prop_rammer", -2.5, 1.5, 1.875),
            new Z0004DeckSlot("slot_ram_starboard", Z0004Pirate.Blunderbuss, "deck_ram_b", "prop_rammer", -1.5, -1.5, 1.875),
            new Z0004DeckSlot("slot_climb", Z0004Pirate.Cutlass, "deck_climb", NoProp, 0.0, 1.875, 1.875),
            new Z0004DeckSlot("slot_captain", Captain, "deck_steer", NoProp, -9.25, 0.0, 3.875));

    public boolean captain() {
        return Variant == Captain;
    }

    public boolean gunCrew() {
        return "prop_rammer".equals(Prop);
    }
}

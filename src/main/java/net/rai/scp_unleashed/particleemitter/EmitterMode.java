package net.rai.scp_unleashed.particleemitter;

import net.minecraft.util.StringRepresentable;

public enum EmitterMode implements StringRepresentable {
    ColdFloor("cold_floor");

    private final String Name;

    EmitterMode(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public static EmitterMode byName(String Wanted) {
        for (EmitterMode Each : values()) {
            if (Each.Name.equals(Wanted)) {
                return Each;
            }
        }
        return ColdFloor;
    }

    public EmitterMode next() {
        EmitterMode[] All = values();
        return All[(ordinal() + 1) % All.length];
    }

    public String translationKey() {
        return "mode.scp_unleashed.particle_emitter." + Name;
    }
}

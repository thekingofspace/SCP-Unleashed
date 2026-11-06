package net.rai.scp_unleashed.medical;

import net.minecraft.world.level.block.SoundType;

public enum MedicalMaterial {
    Metal(SoundType.METAL, 1.5f, true),
    Cloth(SoundType.WOOL, 0.5f, false);

    private final SoundType Sound;
    private final float Hardness;
    private final boolean Solid;

    MedicalMaterial(SoundType Sound, float Hardness, boolean Solid) {
        this.Sound = Sound;
        this.Hardness = Hardness;
        this.Solid = Solid;
    }

    public SoundType getSound() {
        return Sound;
    }

    public float getHardness() {
        return Hardness;
    }

    public boolean isSolid() {
        return Solid;
    }
}

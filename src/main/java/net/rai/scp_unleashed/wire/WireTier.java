package net.rai.scp_unleashed.wire;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.rai.scp_unleashed.registry.ModItems;

public enum WireTier implements StringRepresentable {
    Coated("coated", 50, 35.0, 1.5 / 16.0, "wire"),
    Advanced("advanced", 300, 32.0, 1.5 / 16.0, "wire_advanced"),
    SuperHeavy("super_heavy", 2000, 16.0, 3.0 / 16.0, "wire_super_heavy"),
    Ultimate("ultimate", 8000, 12.0, 3.0 / 16.0, "wire_ultimate"),
    Tether("tether", 20000, 10.0, 3.0 / 16.0, "wire_tether", 9),
    Creative("creative", Integer.MAX_VALUE / 4, 64.0, 1.5 / 16.0, "wire_creative");

    public static final Codec<WireTier> TierCodec = StringRepresentable.fromEnum(WireTier::values);

    private final String Name;
    private final int MaxDemand;
    private final double MaxLength;
    private final double HalfWidth;
    private final String TextureName;
    private final int Glow;

    WireTier(String Name, int MaxDemand, double MaxLength, double HalfWidth, String TextureName) {
        this(Name, MaxDemand, MaxLength, HalfWidth, TextureName, 0);
    }

    WireTier(String Name, int MaxDemand, double MaxLength, double HalfWidth, String TextureName, int Glow) {
        this.Name = Name;
        this.MaxDemand = MaxDemand;
        this.MaxLength = MaxLength;
        this.HalfWidth = HalfWidth;
        this.TextureName = TextureName;
        this.Glow = Glow;
    }

    public int getMaxDemand() {
        return MaxDemand;
    }

    public double getMaxLength() {
        return MaxLength;
    }

    public double getHalfWidth() {
        return HalfWidth;
    }

    public String getTextureName() {
        return TextureName;
    }

    public int getGlow() {
        return Glow;
    }

    public Item getCoil() {
        return switch (this) {
            case Coated -> ModItems.WireCoil.get();
            case Advanced -> ModItems.AdvancedWire.get();
            case SuperHeavy -> ModItems.SuperHeavyWire.get();
            case Ultimate -> ModItems.UltimateWire.get();
            case Tether -> ModItems.TetherWire.get();
            case Creative -> ModItems.CreativeWire.get();
        };
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}

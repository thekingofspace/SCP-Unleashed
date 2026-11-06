package net.rai.scp_unleashed.upgrade;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.rai.scp_unleashed.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public enum UpgradeType implements StringRepresentable {
    AutoReset("auto_reset", "block.scp_unleashed.breaker", 1),
    Speed("speed", "upgrade.scp_unleashed.speed.fits", 3),
    Range("range", "upgrade.scp_unleashed.range.fits", 2),
    Muffler("muffler", "upgrade.scp_unleashed.muffler.fits", 1);

    public static final Codec<UpgradeType> TypeCodec = StringRepresentable.fromEnum(UpgradeType::values);

    private final String Name;
    private final String FitsKey;
    private final int Limit;

    UpgradeType(String Name, String FitsKey, int Limit) {
        this.Name = Name;
        this.FitsKey = FitsKey;
        this.Limit = Limit;
    }

    @Nullable
    public static UpgradeType byName(String Wanted) {
        for (UpgradeType Type : values()) {
            if (Type.Name.equals(Wanted)) {
                return Type;
            }
        }
        return null;
    }

    public int getLimit() {
        return Limit;
    }

    public Component getDisplayName() {
        return Component.translatable("upgrade.scp_unleashed." + Name);
    }

    public Component getDescription() {
        return Component.translatable("upgrade.scp_unleashed." + Name + ".tooltip");
    }

    public Component getFits() {
        return Component.translatable(FitsKey);
    }

    public Item getItem() {
        return switch (this) {
            case AutoReset -> ModItems.AutoResetUpgrade.get();
            case Speed -> ModItems.SpeedUpgrade.get();
            case Range -> net.rai.scp_unleashed.rifttech.RiftTechRegistry.RangeUpgrade.get();
            case Muffler -> ModItems.SoundMufflerUpgrade.get();
        };
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}

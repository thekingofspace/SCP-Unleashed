package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.RiftFragment;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum ArchivePool implements StringRepresentable {
    Normal("normal", null, 0),
    Apocalyptic("apocalyptic", "apocalyptic_fragments", 0xFFC23C);

    public static final Codec<ArchivePool> PoolCodec = StringRepresentable.fromEnum(ArchivePool::values);

    private final String SerializedName;
    @Nullable
    private final TagKey<Item> Keys;
    private final int Colour;

    ArchivePool(String SerializedName, @Nullable String KeyTag, int Colour) {
        this.SerializedName = SerializedName;
        this.Keys = KeyTag == null ? null : TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, KeyTag));
        this.Colour = Colour;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public int getColour() {
        return Colour;
    }

    public String labelKey() {
        return "archive.scp_unleashed.rarity." + SerializedName;
    }

    public static Optional<ArchivePool> forFragment(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return Optional.empty();
        }
        if (Stack.getItem() instanceof RiftFragment) {
            return Optional.of(Normal);
        }
        for (ArchivePool Pool : values()) {
            if (Pool.Keys != null && Stack.is(Pool.Keys)) {
                return Optional.of(Pool);
            }
        }
        return Optional.empty();
    }

    public static ArchivePool poolOf(ItemStack Stack) {
        return forFragment(Stack).orElse(Normal);
    }

    public static int tierFor(ItemStack Stack) {
        if (Stack.getItem() instanceof RiftFragment Source) {
            return Source.getRarity().getTier();
        }
        return forFragment(Stack).isPresent() ? DocumentRoller.Tiers : 0;
    }
}

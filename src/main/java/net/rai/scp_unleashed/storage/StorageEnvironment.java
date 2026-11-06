package net.rai.scp_unleashed.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public enum StorageEnvironment implements StringRepresentable {
    Normal("normal", ChatFormatting.GRAY),
    Heated("heated", ChatFormatting.GOLD),
    Cold("cold", ChatFormatting.AQUA);

    private final String Name;
    private final ChatFormatting Colour;

    StorageEnvironment(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public Component getDisplayName() {
        return Component.translatable("storage.scp_unleashed.environment." + Name);
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public boolean isConditioned() {
        return this != Normal;
    }

    @Nullable
    public Item getUpgrade() {
        return switch (this) {
            case Heated -> StorageRegistry.HeatedUpgrade.get();
            case Cold -> StorageRegistry.ColdUpgrade.get();
            default -> null;
        };
    }

    public ItemStack upgradeStack() {
        Item Upgrade = getUpgrade();
        return Upgrade == null ? ItemStack.EMPTY : new ItemStack(Upgrade);
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}

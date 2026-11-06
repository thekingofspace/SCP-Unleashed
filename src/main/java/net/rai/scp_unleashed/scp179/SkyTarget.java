package net.rai.scp_unleashed.scp179;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public record SkyTarget(ResourceLocation Id, String Designation, String NameKey, String RegionKey) {
    public Component name() {
        return Component.translatable(NameKey);
    }

    public Component region() {
        return Component.translatable(RegionKey);
    }

    public Component label() {
        return Component.literal(Designation);
    }
}

package net.rai.scp_unleashed.scp539.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp539.Scp539Helicopter;
import net.rai.scp_unleashed.scp539.Scp539Registry;
import net.rai.scp_unleashed.scp539.Scp539Stone;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp539JadeProvider implements IEntityComponentProvider {
    Stone("scp539_stone"),
    Helicopter("scp539_helicopter");

    private final ResourceLocation Uid;
    private final String Key;

    Scp539JadeProvider(String Path) {
        Uid = Scp539Registry.location(Path);
        Key = "jade.scp_unleashed." + Path;
    }

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (Accessor.getEntity() instanceof Scp539Stone || Accessor.getEntity() instanceof Scp539Helicopter) {
            Tooltip.add(Component.translatable(Key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}

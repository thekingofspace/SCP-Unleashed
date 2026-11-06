package net.rai.scp_unleashed.scp280.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp280.Scp280Registry;
import net.rai.scp_unleashed.scp280.Scp280Shade;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp280JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp280Registry.location("scp280_shade");
    private static final String Prefix = "jade.scp_unleashed.scp280.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp280Shade Shade)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.RED));
        String State;
        ChatFormatting Colour;
        if (Shade.isContained()) {
            State = "contained";
            Colour = ChatFormatting.DARK_AQUA;
        } else if (Shade.action() == Scp280Shade.ActionEmerge) {
            State = "emerging";
            Colour = ChatFormatting.DARK_PURPLE;
        } else if (Shade.isPinned()) {
            State = "cowering";
            Colour = ChatFormatting.GOLD;
        } else if (Shade.action() == Scp280Shade.ActionRecoil) {
            State = "recoiling";
            Colour = ChatFormatting.GOLD;
        } else if (Shade.isChasing()) {
            State = "hunting";
            Colour = ChatFormatting.DARK_RED;
        } else {
            State = "watching";
            Colour = ChatFormatting.GRAY;
        }
        Tooltip.add(Component.translatable(Prefix + State).withStyle(Colour));
    }
}

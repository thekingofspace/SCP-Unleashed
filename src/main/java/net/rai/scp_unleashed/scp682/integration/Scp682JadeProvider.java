package net.rai.scp_unleashed.scp682.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp682.Scp682;
import net.rai.scp_unleashed.scp682.Scp682Registry;
import net.rai.scp_unleashed.scp682.Scp682Size;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp682JadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int FadedGold = 0x9C7C3A;
    public static final int FadedAqua = 0x3F6E6E;
    private static final ResourceLocation Uid = Scp682Registry.location("scp682");
    private static final String Prefix = "jade.scp_unleashed.scp682.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp682 Owner)) {
            return;
        }
        if (Owner.size() != Scp682Size.Full) {
            Tooltip.add(Component.translatable(Prefix + Owner.size().key()).withStyle(ChatFormatting.GRAY));
        }
        if (Owner.isCollapsed()) {
            Tooltip.add(Component.translatable(Prefix + "collapsed").withStyle(ChatFormatting.DARK_RED));
        } else if (Owner.isCalm()) {
            Tooltip.add(Component.translatable(Prefix + "calm").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (Owner.adaptedCount() > 0) {
            Tooltip.add(Owner.fadingHide() != 0 ? Component.translatable(Prefix + "adapted.fading").withColor(FadedGold)
                    : Component.translatable(Prefix + "adapted").withStyle(ChatFormatting.GOLD));
        }
        if (Owner.signatures() != 0) {
            Tooltip.add(Owner.fadingSignatures() != 0 ? Component.translatable(Prefix + "marked.fading").withColor(FadedAqua)
                    : Component.translatable(Prefix + "marked").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (Owner.isSoaked()) {
            Tooltip.add(Component.translatable(Prefix + "soaked").withStyle(ChatFormatting.GREEN));
        }
    }
}

package net.rai.scp_unleashed.scp860.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import net.rai.scp_unleashed.scp860.Scp860Watcher;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp860WatcherJadeProvider implements IEntityComponentProvider {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp860_2.";
    private static final ResourceLocation Uid = Scp860Registry.location("scp860_2");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    public static String stateKey(byte Posture) {
        return switch (Posture) {
            case Scp860Watcher.Watching -> "watching";
            case Scp860Watcher.Restless -> "restless";
            case Scp860Watcher.Hunting -> "hunting";
            case Scp860Watcher.Wary -> "wary";
            default -> "still";
        };
    }

    public static ChatFormatting stateStyle(byte Posture) {
        return switch (Posture) {
            case Scp860Watcher.Hunting -> ChatFormatting.RED;
            case Scp860Watcher.Restless -> ChatFormatting.GOLD;
            case Scp860Watcher.Wary -> ChatFormatting.YELLOW;
            default -> ChatFormatting.GRAY;
        };
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp860Watcher Owner)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withColor(DocumentClass.Safe.getColour() & 0xFFFFFF));
        Tooltip.add(Component.translatable(Prefix + "health", Mth.ceil(Owner.getHealth()), Mth.ceil(Owner.getMaxHealth())).withStyle(ChatFormatting.GRAY));
        byte Posture = Owner.posture();
        Tooltip.add(Component.translatable(Prefix + stateKey(Posture)).withStyle(stateStyle(Posture)));
    }
}

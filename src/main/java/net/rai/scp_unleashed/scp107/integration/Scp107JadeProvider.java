package net.rai.scp_unleashed.scp107.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp107.Scp107RainKind;
import net.rai.scp_unleashed.scp107.Scp107ShellBlock;
import net.rai.scp_unleashed.scp107.client.Scp107ClientRain;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

public enum Scp107JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp107_shell");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockState().getBlock() instanceof Scp107ShellBlock)) {
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp107.class").withStyle(ChatFormatting.GOLD));
        Optional<Scp107RainKind> Kind = Scp107ClientRain.kind();
        if (Kind.isEmpty()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp107.dry").withStyle(ChatFormatting.GREEN));
            return;
        }
        ChatFormatting Colour = switch (Kind.get()) {
            case Water -> ChatFormatting.AQUA;
            case Lava -> ChatFormatting.RED;
            case Caustic -> ChatFormatting.YELLOW;
            case Milk -> ChatFormatting.WHITE;
            case Oil -> ChatFormatting.DARK_GRAY;
        };
        Tooltip.add(Component.translatable("jade.scp_unleashed." + Kind.get().key()).withStyle(Colour));
        if (Scp107ClientRain.gathering()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp107.gathering").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else if (Scp107ClientRain.easing()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp107.easing").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}

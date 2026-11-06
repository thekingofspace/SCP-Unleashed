package net.rai.scp_unleashed.scp085.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp085.Scp085Act;
import net.rai.scp_unleashed.scp085.Scp085Form;
import net.rai.scp_unleashed.scp085.Scp085Scene;
import net.rai.scp_unleashed.scp085.Scp085SheetBlock;
import net.rai.scp_unleashed.scp085.Scp085SheetBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp085JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp085_sheet");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp085SheetBlock Sheet)) {
            return;
        }
        Scp085Scene Scene = Sheet.sceneOf(State);
        if (Sheet.isHomeSheet()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp085_home").withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp085_drawing", Component.translatable(Scene.titleKey()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }
        if (!(Accessor.getBlockEntity() instanceof Scp085SheetBlockEntity Paper) || !Paper.isHere()) {
            return;
        }
        Scp085Form Form = Paper.form();
        boolean Shifting = Paper.act() == Scp085Act.ShiftOut || Paper.act() == Scp085Act.ShiftBack;
        if (Form == Scp085Form.Woman || Shifting) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp085_here").withStyle(ChatFormatting.DARK_AQUA));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp085_here_as", Component.translatable(Form.nameKey())).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}

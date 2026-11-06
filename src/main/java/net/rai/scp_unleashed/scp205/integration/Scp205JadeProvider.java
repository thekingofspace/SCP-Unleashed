package net.rai.scp_unleashed.scp205.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp205.Scp205Beam;
import net.rai.scp_unleashed.scp205.Scp205LampBlock;
import net.rai.scp_unleashed.scp205.Scp205LampBlockEntity;
import net.rai.scp_unleashed.scp205.Scp205Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp205JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp205Registry.location("scp205_lamp");
    private static final String Prefix = "jade.scp_unleashed.scp205_lamp.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp205LampBlock)) {
            return;
        }
        boolean Lit = State.getValue(Scp205LampBlock.Lit);
        Tooltip.add(Component.translatable(Prefix + (Lit ? "lit" : "dark"), Scp205LampBlockEntity.PowerDraw).withStyle(Lit ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Prefix + "tilt", Component.translatable(Prefix + "tilt." + Scp205Beam.tilt(State).getSerializedName())).withStyle(ChatFormatting.GRAY));
        BlockState Lower = Accessor.getLevel().getBlockState(Scp205LampBlock.lowerPos(State, Accessor.getPosition()));
        if (Lit && Accessor.getLevel().getBlockEntity(Scp205LampBlock.lowerPos(State, Accessor.getPosition())) instanceof Scp205LampBlockEntity Lamp) {
            byte Mode = Lamp.mode();
            if (Mode == Scp205LampBlockEntity.ModeShadow || Mode == Scp205LampBlockEntity.ModeDevil) {
                Tooltip.add(Component.translatable(Prefix + "figure").withStyle(ChatFormatting.DARK_PURPLE));
            } else if (Mode == Scp205LampBlockEntity.ModeOut) {
                Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.DARK_RED));
            }
        }
        if (Lower.getBlock() instanceof Scp205LampBlock && !Lower.getValue(Scp205LampBlock.Connected)) {
            Tooltip.add(Component.translatable(Prefix + "unwired").withStyle(ChatFormatting.GOLD));
        }
    }
}

package net.rai.scp_unleashed.scp076.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp076.Scp076Commands;
import net.rai.scp_unleashed.scp076.Scp076DoorBlock;
import net.rai.scp_unleashed.scp076.Scp076DoorBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp076DoorProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp076_door");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp076DoorBlock) || !(Accessor.getLevel().getBlockEntity(Scp076DoorBlock.origin(Accessor.getPosition(), State))
                instanceof Scp076DoorBlockEntity Door)) {
            return;
        }
        byte Phase = Door.phase();
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp076_door." + Scp076Commands.phaseKey(Phase))
                .withStyle(Phase == Scp076DoorBlockEntity.Sealed ? ChatFormatting.GRAY : ChatFormatting.DARK_RED));
        int Held = 0;
        for (int Lock = 0; Lock < Scp076DoorBlockEntity.LockCount; Lock++) {
            if (Door.engaged(Lock, 0.0f) > 0.5f) {
                Held++;
            }
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp076_door.locks", Held, Scp076DoorBlockEntity.LockCount).withStyle(ChatFormatting.DARK_GRAY));
    }
}

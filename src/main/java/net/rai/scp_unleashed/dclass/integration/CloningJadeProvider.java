package net.rai.scp_unleashed.dclass.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.CloningMachineBlock;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;
import net.rai.scp_unleashed.dclass.CloningStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CloningJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "cloning_machine");
    private static final String DataKey = "CloningMachine";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        CloningMachineBlockEntity Tank = CloningMachineBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Tank == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", (byte) Tank.status().ordinal());
        Info.putByte("Growing", (byte) Tank.growing().ordinal());
        Info.putInt("Progress", Math.round(Tank.progressFraction() * 100.0f));
        Info.putString("Sample", Tank.sampleName());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        CloningStatus Status = CloningStatus.byIndex(Info.getByte("Status"));
        CloningKind Kind = CloningKind.byIndex(Info.getByte("Growing"));
        if (Kind != CloningKind.None) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.cloning_machine.growing." + Kind.getKey(), Info.getInt("Progress"), Info.getString("Sample")).withStyle(ChatFormatting.GOLD));
        }
        ChatFormatting Colour = switch (Status) {
            case Growing, Releasing -> ChatFormatting.GOLD;
            case Idle -> ChatFormatting.GREEN;
            case Blocked, TrayFull -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        };
        Tooltip.add(Component.translatable("gui.scp_unleashed.cloning_machine.status." + Status.getKey()).withStyle(Colour));
    }
}

package net.rai.scp_unleashed.gas.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.VaporizerBlockEntity;
import net.rai.scp_unleashed.gas.VaporizerStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum VaporizerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "liquid_vaporizer");
    private static final String DataKey = "LiquidVaporizer";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof VaporizerBlockEntity Machine)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        FluidStack Liquid = Machine.getInputTank().getFluid();
        if (!Liquid.isEmpty()) {
            Info.put("Liquid", Liquid.save(Accessor.getLevel().registryAccess()));
        }
        Info.put("Gas", Machine.getGasTank().getGas().save());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        VaporizerStatus Status = VaporizerStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        FluidStack Liquid = Info.contains("Liquid") ? FluidStack.parseOptional(Accessor.getLevel().registryAccess(), Info.getCompound("Liquid")) : FluidStack.EMPTY;
        if (!Liquid.isEmpty()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.liquid_vaporizer.input", Liquid.getHoverName(), Liquid.getAmount(), VaporizerBlockEntity.TankCapacity)
                    .withStyle(ChatFormatting.AQUA));
        }
        GasStack Held = GasStack.load(Info.getCompound("Gas"));
        if (!Held.isEmpty()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.liquid_vaporizer.output", Held.getGas().getName(), Held.getAmount(), VaporizerBlockEntity.GasCapacity)
                    .withStyle(Held.getGas().isToxic() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        }
    }
}

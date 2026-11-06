package net.rai.scp_unleashed.oil.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.oil.CombustionGeneratorBlockEntity;
import net.rai.scp_unleashed.oil.OilMachineBlock;
import net.rai.scp_unleashed.oil.OilMachineBlockEntity;
import net.rai.scp_unleashed.oil.OilStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum OilMachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "oil_machine");
    private static final String DataKey = "OilMachine";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        OilMachineBlockEntity Machine = OilMachineBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Progress", Math.round(Machine.getProgress() * 100.0f));
        Info.putString("Kind", Machine.getKind().getName());
        ListTag Tanks = new ListTag();
        for (FluidTank Tank : Machine.getTanks()) {
            CompoundTag Entry = new CompoundTag();
            if (!Tank.isEmpty()) {
                Entry.putString("Fluid", BuiltInRegistries.FLUID.getKey(Tank.getFluid().getFluid()).toString());
                Entry.putInt("Amount", Tank.getFluidAmount());
            }
            Entry.putInt("Capacity", Tank.getCapacity());
            Tanks.add(Entry);
        }
        Info.put("Tanks", Tanks);
        if (Machine instanceof CombustionGeneratorBlockEntity Generator) {
            Info.putInt("Generating", Generator.getGenerating());
            Info.putInt("Exhaust", Generator.getExhaustTank().getAmount());
            Info.putInt("ExhaustCapacity", Generator.getExhaustTank().getCapacity());
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        OilStatus Status = OilStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (Status.isWorking() && Info.getInt("Progress") > 0 && !Info.contains("Generating")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.oil.progress", Info.getInt("Progress")).withStyle(ChatFormatting.GRAY));
        }
        ListTag Tanks = Info.getList("Tanks", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Tanks.size(); Index++) {
            CompoundTag Entry = Tanks.getCompound(Index);
            if (!Entry.contains("Fluid")) {
                continue;
            }
            ResourceLocation Id = ResourceLocation.tryParse(Entry.getString("Fluid"));
            if (Id == null || !BuiltInRegistries.FLUID.containsKey(Id)) {
                continue;
            }
            Fluid Liquid = BuiltInRegistries.FLUID.get(Id);
            Tooltip.add(Component.translatable("jade.scp_unleashed.oil.tank", new FluidStack(Liquid, 1).getHoverName(), Entry.getInt("Amount"), Entry.getInt("Capacity"))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (Info.contains("Exhaust") && Info.getInt("Exhaust") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.oil.tank", GasRegistry.ToxicGas.get().getName(), Info.getInt("Exhaust"), Info.getInt("ExhaustCapacity"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}

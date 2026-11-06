package net.rai.scp_unleashed.gas.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.plumbing.DrainBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DrainJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "drain");
    private static final String DataKey = "Drain";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof DrainBlockEntity Drain)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Rate", Drain.getRate());
        Info.putString("Fluid", BuiltInRegistries.FLUID.getKey(Drain.getPouring()).toString());
        Info.putInt("Reach", DrainBlockEntity.reach(Accessor.getLevel(), Accessor.getPosition()));
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        int Rate = Info.getInt("Rate");
        ResourceLocation Id = ResourceLocation.tryParse(Info.getString("Fluid"));
        Fluid Liquid = Id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(Id);
        if (Rate > 0 && Liquid != Fluids.EMPTY) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.drain.pouring", Rate, new FluidStack(Liquid, 1).getHoverName()).withStyle(ChatFormatting.AQUA));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.drain.dry").withStyle(ChatFormatting.GRAY));
        }
        int Reach = Info.getInt("Reach");
        Tooltip.add(Component.translatable(Reach > 0 ? "jade.scp_unleashed.drain.reach" : "jade.scp_unleashed.drain.blocked", Reach)
                .withStyle(Reach > 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD));
    }
}

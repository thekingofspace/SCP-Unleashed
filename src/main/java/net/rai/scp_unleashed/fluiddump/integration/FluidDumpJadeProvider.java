package net.rai.scp_unleashed.fluiddump.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fluiddump.DumpMode;
import net.rai.scp_unleashed.fluiddump.FluidDumpBlockEntity;
import net.rai.scp_unleashed.fluiddump.PollutionField;
import net.rai.scp_unleashed.fluiddump.PollutionMath;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum FluidDumpJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fluid_dump");
    private static final String DataKey = "FluidDump";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof FluidDumpBlockEntity Dump) || !(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Mode", (byte) Dump.getMode().ordinal());
        Info.putInt("Rate", Dump.getRate());
        Info.putString("Fluid", BuiltInRegistries.FLUID.getKey(Dump.getHandling()).toString());
        Info.putFloat("Pollution", (float) PollutionField.get(World).densityAt(Accessor.getPosition()));
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        DumpMode Mode = DumpMode.byIndex(Info.getByte("Mode"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.fluid_dump.mode." + Mode.getKey()).withStyle(Mode.getColour()));
        ResourceLocation Id = ResourceLocation.tryParse(Info.getString("Fluid"));
        Fluid Liquid = Id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(Id);
        int Rate = Info.getInt("Rate");
        if (Mode.isWorking() && Rate > 0 && Liquid != Fluids.EMPTY) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.fluid_dump.rate", Rate, new FluidStack(Liquid, 1).getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        double Pollution = Info.getFloat("Pollution");
        String Grade = PollutionMath.level(Pollution);
        ChatFormatting Colour = switch (Grade) {
            case "toxic" -> ChatFormatting.RED;
            case "murky" -> ChatFormatting.GOLD;
            case "light" -> ChatFormatting.YELLOW;
            default -> ChatFormatting.GREEN;
        };
        Tooltip.add(Component.translatable("jade.scp_unleashed.fluid_dump.pollution", Component.translatable("jade.scp_unleashed.fluid_dump.level." + Grade),
                Math.round(Pollution * 100.0)).withStyle(Colour));
    }
}

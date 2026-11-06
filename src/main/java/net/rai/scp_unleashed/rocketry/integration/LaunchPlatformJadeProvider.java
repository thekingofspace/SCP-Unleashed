package net.rai.scp_unleashed.rocketry.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.oil.OilRegistry;
import net.rai.scp_unleashed.rocketry.platform.LaunchCheck;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformBlock;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformBlockEntity;
import net.rai.scp_unleashed.rocketry.platform.LaunchStatus;
import net.rai.scp_unleashed.rocketry.platform.RocketPlatformBlockEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketDestinations;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum LaunchPlatformJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rocketry_platform");
    private static final String DataKey = "LaunchPlatform";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        LaunchPlatformBlockEntity Platform = LaunchPlatformBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Platform == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Platform.getStatus().getSerializedName());
        Info.putInt("Fuel", Platform.getFuel().getFluidAmount());
        Info.putInt("Capacity", Platform.getKind().getFuelCapacity());
        if (Platform instanceof RocketPlatformBlockEntity Rocket && Rocket.destination() != null) {
            Info.putString("Destination", Rocket.destination().Id().toString());
        }
        LaunchCheck Missing = Platform.firstMissing();
        if (Missing != null) {
            Info.putString("Missing", Missing.getName());
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
        LaunchStatus Status = LaunchStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (Info.contains("Destination")) {
            for (RocketDestinations.Entry Entry : RocketDestinations.client()) {
                if (Entry.Destination().Id().toString().equals(Info.getString("Destination"))) {
                    Tooltip.add(Component.translatable("gui.scp_unleashed.rocketry.destination", Entry.Destination().Name()).withStyle(ChatFormatting.AQUA));
                }
            }
        }
        if (Info.contains("Missing") && Status != LaunchStatus.Launching) {
            for (LaunchCheck Check : LaunchCheck.values()) {
                if (Check.getName().equals(Info.getString("Missing"))) {
                    Tooltip.add(Component.translatable(Check.failKey()).withStyle(ChatFormatting.GOLD));
                }
            }
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.oil.tank", new FluidStack(OilRegistry.HeavyFuel.getSource(), 1).getHoverName(), Info.getInt("Fuel"),
                Info.getInt("Capacity")).withStyle(ChatFormatting.GRAY));
    }
}

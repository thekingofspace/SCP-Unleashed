package net.rai.scp_unleashed.scp500.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp500.Scp500BottleBlock;
import net.rai.scp_unleashed.scp500.Scp500BottleBlockEntity;
import net.rai.scp_unleashed.scp500.Scp500Stock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp500BottleJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp500_bottle");
    private static final String DataKey = "Scp500Bottle";
    private static final String Prefix = "jade.scp_unleashed.scp500_bottle.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    public static long daysAway(long Ticks) {
        return Math.max(1L, (Ticks + Scp500Stock.DayLength - 1L) / Scp500Stock.DayLength);
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof Scp500BottleBlockEntity Bottle) || Accessor.getLevel() == null) {
            return;
        }
        long Now = Accessor.getLevel().getGameTime();
        Scp500Stock Current = Bottle.stockAt(Now);
        CompoundTag Info = new CompoundTag();
        Info.putInt("Pills", Current.Pills());
        Info.putLong("Left", Current.ticksLeft(Now));
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        int Pills = Scp500BottleBlock.pills(Accessor.getBlockState());
        long Left = -1L;
        if (Data.contains(DataKey)) {
            Pills = Data.getCompound(DataKey).getInt("Pills");
            Left = Data.getCompound(DataKey).getLong("Left");
        }
        if (Pills > 0) {
            Tooltip.add(Component.translatable(Prefix + "left", Pills, Scp500Stock.Full).withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
        }
        if (Pills < Scp500Stock.Full && Left >= 0L) {
            long Days = daysAway(Left);
            Component Hint = Days <= 1L ? Component.translatable(Prefix + "soon") : Component.translatable(Prefix + "days", Days);
            Tooltip.add(Hint.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}

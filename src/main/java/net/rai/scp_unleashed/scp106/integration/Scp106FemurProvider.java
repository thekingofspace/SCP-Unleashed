package net.rai.scp_unleashed.scp106.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp106.Scp106FemurBreakerBlock;
import net.rai.scp_unleashed.scp106.Scp106FemurBreakerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp106FemurProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_femur_breaker");
    private static final String DataKey = "Scp106Femur";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        Scp106FemurBreakerBlockEntity Machine = Scp106FemurBreakerBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        LivingEntity Occupant = Machine.occupant();
        if (Occupant != null) {
            Info.putString("Occupant", Component.Serializer.toJson(Occupant.getDisplayName(), Accessor.getLevel().registryAccess()));
        }
        Info.putBoolean("Cycling", Machine.isCycling());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (Info.contains("Occupant")) {
            Component Name = Component.Serializer.fromJson(Info.getString("Occupant"), Accessor.getLevel().registryAccess());
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp106_femur_breaker.occupied", Name == null ? Component.literal("?") : Name).withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp106_femur_breaker.empty").withStyle(ChatFormatting.GRAY));
        }
        if (Info.getBoolean("Cycling")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp106_femur_breaker.cycling").withStyle(ChatFormatting.RED));
        }
    }
}

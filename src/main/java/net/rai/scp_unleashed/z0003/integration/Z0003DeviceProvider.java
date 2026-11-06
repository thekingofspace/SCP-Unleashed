package net.rai.scp_unleashed.z0003.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0003.Z0003Device;
import net.rai.scp_unleashed.z0003.Z0003Phrase;
import net.rai.scp_unleashed.z0003.Z0003Voice;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Z0003DeviceProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.z0003.";
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0003_device");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Z0003Device Device && Device.isArmed()) {
            Data.putLong("Z0003Left", Math.max(0L, Device.due() - Device.level().getGameTime()));
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Z0003Device Device)) {
            return;
        }
        String Radius = Z0003Voice.number(Device.radius());
        if (Device.isActive()) {
            Tooltip.add(Component.translatable(Prefix + "active", Radius).withStyle(ChatFormatting.GREEN));
            return;
        }
        if (Device.isArmed()) {
            CompoundTag Data = Accessor.getServerData();
            if (Data.contains("Z0003Left")) {
                float Seconds = Data.getLong("Z0003Left") / (float) Z0003Phrase.TicksPerSecond;
                Tooltip.add(Component.translatable(Prefix + "armed", Radius, Z0003Voice.number(Seconds)).withStyle(ChatFormatting.YELLOW));
            } else {
                Tooltip.add(Component.translatable(Prefix + "armed_quiet").withStyle(ChatFormatting.YELLOW));
            }
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "idle").withStyle(ChatFormatting.GRAY));
        if (Device.tetherHolder() != null) {
            Tooltip.add(Component.translatable(Prefix + "tethered").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

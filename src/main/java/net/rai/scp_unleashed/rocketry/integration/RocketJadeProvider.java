package net.rai.scp_unleashed.rocketry.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformKind;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum RocketJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rocketry_vehicle");
    private static final String DataKey = "RocketryVehicle";
    private static final String Prefix = "jade.scp_unleashed.rocketry.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        CompoundTag Info = new CompoundTag();
        if (Accessor.getEntity() instanceof RocketEntity Rocket) {
            Info.putInt("Fuel", Rocket.getStoredFuel());
        } else if (Accessor.getEntity() instanceof DropPodEntity Pod) {
            Info.putInt("Thrusters", Math.round(Pod.getThrusterFuel() * 100.0f / DropPodEntity.ThrusterTicks));
            Info.putBoolean("Kit", Pod.getContents() == DropPodEntity.Kit.Return);
        } else {
            return;
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        CompoundTag Info = Data.getCompound(DataKey);
        if (Accessor.getEntity() instanceof RocketEntity Rocket) {
            String State = switch (Rocket.getPhase()) {
                case RocketEntity.PhaseCountdown -> "countdown";
                case RocketEntity.PhaseFlight -> "flight";
                default -> Rocket.hasCrew() ? "crewed" : "empty";
            };
            Tooltip.add(Component.translatable(Prefix + "rocket." + State).withStyle(ChatFormatting.AQUA));
            if (Info.getInt("Fuel") >= LaunchPlatformKind.Rocket.getFuelCost()) {
                Tooltip.add(Component.translatable("item.scp_unleashed.rocket.fuelled").withStyle(ChatFormatting.GRAY));
            }
        } else if (Accessor.getEntity() instanceof DropPodEntity Pod) {
            Tooltip.add(Component.translatable(Prefix + (Pod.isLanded() ? "pod.landed" : "pod.falling")).withStyle(ChatFormatting.AQUA));
            if (!Pod.isLanded() && Info.contains("Thrusters")) {
                Tooltip.add(Component.translatable(Prefix + "pod.thrusters", Info.getInt("Thrusters")).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}

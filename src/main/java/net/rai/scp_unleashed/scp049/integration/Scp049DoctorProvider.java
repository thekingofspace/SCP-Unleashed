package net.rai.scp_unleashed.scp049.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp049DoctorProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_doctor");
    private static final String DataKey = "Scp049";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof Scp049Doctor Doctor) || !(Doctor.level() instanceof ServerLevel World)) {
            return;
        }
        CompoundTag Details = new CompoundTag();
        Details.putString("State", Doctor.stateKey());
        Details.putBoolean("Person", Doctor.quarryId() != null);
        Details.putBoolean("Hunt", !Doctor.trayUsable(World));
        Details.putInt("Cured", Doctor.followers(World).size());
        LivingEntity Held = Doctor.held(World);
        if (Held != null) {
            Details.putString("Held", Component.Serializer.toJson(Held.getDisplayName(), World.registryAccess()));
        }
        BlockPos Tray = Doctor.trayPos();
        if (Tray != null) {
            Details.putInt("Distance", (int) Math.round(Math.sqrt(Doctor.distanceToSqr(Tray.getX() + 0.5, Tray.getY(), Tray.getZ() + 0.5))));
        }
        Data.put(DataKey, Details);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp049Doctor)) {
            return;
        }
        CompoundTag Data = Accessor.getServerData().getCompound(DataKey);
        if (Data.isEmpty()) {
            return;
        }
        boolean Hunting = Data.getBoolean("Hunt") && !Data.getBoolean("Person");
        Tooltip.add(Component.translatable(Data.getBoolean("Person") ? "jade.scp_unleashed.scp049.routine.person" : Hunting ? "jade.scp_unleashed.scp049.routine.hunt"
                : "jade.scp_unleashed.scp049.routine.villager").withStyle(Data.getBoolean("Person") || Hunting ? ChatFormatting.RED : ChatFormatting.GOLD));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp049." + Data.getString("State")).withStyle(ChatFormatting.GRAY));
        if (Data.contains("Held")) {
            Component Name = Component.Serializer.fromJson(Data.getString("Held"), Accessor.getLevel().registryAccess());
            if (Name != null) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp049.held", Name).withStyle(ChatFormatting.RED));
            }
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp049.cured", Data.getInt("Cured")).withStyle(ChatFormatting.DARK_GREEN));
        if (Data.contains("Distance")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp049.tray", Data.getInt("Distance")).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}

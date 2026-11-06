package net.rai.scp_unleashed.dclass.duty.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import net.rai.scp_unleashed.dclass.duty.DClassDuty;
import net.rai.scp_unleashed.dclass.duty.DClassRole;
import net.rai.scp_unleashed.dclass.duty.DClassTask;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DClassDutyJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass_duty");
    private static final String DataKey = "DClassDuty";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof DClass Subject) {
            DClassDuty Duty = DClassDuties.of(Subject);
            if (Duty.idle()) {
                return;
            }
            CompoundTag Info = new CompoundTag();
            Info.putString("Role", Duty.Role().getId());
            Info.putString("Task", Duty.Task().getId());
            if (!Duty.Meal().isEmpty()) {
                Info.putString("Meal", Component.Serializer.toJson(Duty.Meal().getHoverName(), Accessor.getLevel().registryAccess()));
            }
            Data.put(DataKey, Info);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof DClass)) {
            return;
        }
        CompoundTag Info = Accessor.getServerData().getCompound(DataKey);
        if (Info.isEmpty()) {
            return;
        }
        DClassRole Role = DClassRole.byId(Info.getString("Role"));
        DClassTask Task = DClassTask.byId(Info.getString("Task"));
        if (Role.active()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.role", Role.getName()).withStyle(ChatFormatting.AQUA));
        }
        if (Task.active()) {
            Component Name = Task.getName();
            if (Task == DClassTask.EatThis && Info.contains("Meal")) {
                Component Meal = Component.Serializer.fromJson(Info.getString("Meal"), Accessor.getLevel().registryAccess());
                if (Meal != null) {
                    Name = Component.translatable("jade.scp_unleashed.dclass.task_meal", Name, Meal);
                }
            }
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.task", Name).withStyle(ChatFormatting.YELLOW));
        }
    }
}

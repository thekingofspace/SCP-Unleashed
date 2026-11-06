package net.rai.scp_unleashed.scp4845.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Task;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp4845EmployeeJadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = Scp4845Registry.location("scp4845_employee");
    private static final String Prefix = "jade.scp_unleashed.scp4845_employee.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp4845Employee Worker)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "badge", Worker.speakerName(), Worker.roleName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        Scp4845Task Task = Worker.task();
        String Key = Task == Scp4845Task.None ? "between" : Task.Clip;
        Tooltip.add(Component.translatable(Prefix + "task." + Key).withStyle(ChatFormatting.GRAY));
    }
}

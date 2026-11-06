package net.rai.scp_unleashed.guards.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.guards.GuardDuty;
import net.rai.scp_unleashed.guards.GuardOrders;
import net.rai.scp_unleashed.guards.GuardStatus;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.guards.PatrolRoute;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GuardJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = GuardsRegistry.location("patrol_guard");
    private static final String DataKey = "PatrolGuard";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof DClass Guard)) {
            return;
        }
        GuardOrders Orders = GuardDuty.orders(Guard);
        if (Orders == null) {
            return;
        }
        PatrolRoute Route = Orders.route();
        CompoundTag Info = new CompoundTag();
        GuardStatus Status = Orders.onDuty(Guard) ? Orders.status() : GuardStatus.Elsewhere;
        Info.putByte("Status", (byte) Status.ordinal());
        Info.putInt("Post", Orders.index() + 1);
        Info.putInt("Posts", Route.size());
        Info.putBoolean("HoldFire", Route.Rules().HoldFire());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        GuardStatus Status = GuardStatus.byIndex(Info.getByte("Status"));
        Component State = Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour());
        if (Status == GuardStatus.Patrolling || Status == GuardStatus.Returning) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.patrol_guard.post", State, Info.getInt("Post"), Info.getInt("Posts")));
        } else {
            Tooltip.add(State);
        }
        if (Info.getBoolean("HoldFire")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.patrol_guard.hold_fire").withStyle(ChatFormatting.YELLOW));
        }
    }
}

package net.rai.scp_unleashed.guards.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.guards.GuardRules;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.guards.PatrolManagerBlockEntity;
import net.rai.scp_unleashed.guards.PatrolOrdersItem;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public enum PatrolManagerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = GuardsRegistry.location("patrol_manager");
    private static final String DataKey = "PatrolManager";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof PatrolManagerBlockEntity Manager)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Posts", Manager.getPoints().size());
        Info.putBoolean("PingPong", Manager.isPingPong());
        Info.putInt("Mask", Manager.getRules().getMask());
        Info.putBoolean("HoldFire", Manager.isHoldFire());
        Info.putBoolean("Melee", Manager.isMelee());
        Info.putInt("Issued", Manager.getIssued());
        Info.putString("Owner", Manager.getOwnerName());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        int Posts = Info.getInt("Posts");
        if (Posts == 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.patrol_manager.empty").withStyle(ChatFormatting.GRAY));
        } else {
            String Mode = Posts == 1 ? "post" : Info.getBoolean("PingPong") ? "ping_pong" : "loop";
            Tooltip.add(Component.translatable("jade.scp_unleashed.patrol_manager.route", Posts, Component.translatable("gui.scp_unleashed.patrol_manager.mode." + Mode))
                    .withStyle(ChatFormatting.GOLD));
        }
        boolean HoldFire = Info.getBoolean("HoldFire");
        Tooltip.add(Component.translatable(HoldFire ? "gui.scp_unleashed.patrol_manager.fire.hold" : "gui.scp_unleashed.patrol_manager.fire.free")
                .withStyle(HoldFire ? ChatFormatting.YELLOW : ChatFormatting.RED));
        GuardRules Rules = new GuardRules(Info.getInt("Mask"), List.of(), HoldFire, Info.getBoolean("Melee"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.patrol_manager.threats", PatrolOrdersItem.threats(Rules)).withStyle(ChatFormatting.GRAY));
        if (Info.getInt("Issued") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.patrol_manager.issued", Info.getInt("Issued")).withStyle(ChatFormatting.DARK_GRAY));
        }
        String Owner = Info.getString("Owner");
        if (!Owner.isEmpty()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.patrol_manager.owner", Owner).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

package net.rai.scp_unleashed.radio.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.radio.RadioCarry;
import net.rai.scp_unleashed.radio.RadioNetwork;
import net.rai.scp_unleashed.radio.RadioTowerBlockEntity;
import net.rai.scp_unleashed.radio.RadioTowerStatus;
import net.rai.scp_unleashed.radio.VoiceConfig;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum RadioTowerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "radio_booster_tower");
    private static final String DataKey = "RadioTower";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof RadioTowerBlockEntity Tower) || !(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        RadioNetwork.Ranges Limits = VoiceConfig.ranges();
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Tower.status().getSerializedName());
        Info.putInt("Masts", Tower.masts());
        Info.putInt("Channel", Tower.channel());
        Info.putInt("Links", Tower.links(World));
        Info.putInt("Reach", (int) Limits.coverage(Tower.effectiveMasts()));
        Info.putInt("Link", (int) Limits.link(Tower.effectiveMasts()));
        Info.putInt("Demand", Tower.demand());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        RadioTowerStatus Status = RadioTowerStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.key()).withStyle(Status.colour()));
        int Masts = Info.getInt("Masts");
        Tooltip.add(Component.translatable("jade.scp_unleashed.radio_tower.masts", Math.min(Masts, RadioNetwork.MaxMasts), RadioNetwork.MaxMasts).withStyle(ChatFormatting.GRAY));
        if (Status == RadioTowerStatus.NoMast) {
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.radio_tower.carrier", RadioCarry.label(Info.getInt("Channel"))).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable("jade.scp_unleashed.radio_tower.range", Info.getInt("Reach"), Info.getInt("Link")).withStyle(ChatFormatting.GRAY));
        if (Status == RadioTowerStatus.Online) {
            int Links = Info.getInt("Links");
            Tooltip.add((Links > 0 ? Component.translatable("jade.scp_unleashed.radio_tower.links", Links) : Component.translatable("jade.scp_unleashed.radio_tower.alone"))
                    .withStyle(Links > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.radio_tower.draw", Info.getInt("Demand")).withStyle(ChatFormatting.GRAY));
    }
}

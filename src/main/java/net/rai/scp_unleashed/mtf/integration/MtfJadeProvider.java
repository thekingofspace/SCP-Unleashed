package net.rai.scp_unleashed.mtf.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.mtf.MtfMission;
import net.rai.scp_unleashed.mtf.MtfOperative;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MtfJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "mtf_operative");
    private static final String Prefix = "jade.scp_unleashed.mtf_operative.";
    private static final String DataKey = "MtfOperative";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof MtfOperative Subject)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", Subject.status());
        MtfMission Mission = Subject.mission();
        if (Mission != null && Mission.stage() == MtfMission.Stage.Active) {
            Info.putInt("Left", Mission.remaining(Subject.level().getGameTime()));
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof MtfOperative Subject)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "unit", Subject.tier().displayName()).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Prefix + "role", Subject.role().displayName()).withStyle(ChatFormatting.GRAY));
        CompoundTag Info = Accessor.getServerData().getCompound(DataKey);
        byte Status = Info.contains("Status") ? Info.getByte("Status") : Subject.status();
        ChatFormatting Colour = switch (Status) {
            case MtfOperative.StatusEngaging -> ChatFormatting.RED;
            case MtfOperative.StatusCover -> ChatFormatting.GOLD;
            case MtfOperative.StatusRegroup, MtfOperative.StatusBoarding -> ChatFormatting.AQUA;
            default -> ChatFormatting.GREEN;
        };
        Tooltip.add(Component.translatable(Prefix + "status." + statusKey(Status)).withStyle(Colour));
        if (Info.contains("Left")) {
            float Rate = Accessor.getLevel().tickRateManager().tickrate();
            Tooltip.add(Component.translatable(Prefix + "left", StringUtil.formatTickDuration(Info.getInt("Left"), Rate)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String statusKey(byte Status) {
        return switch (Status) {
            case MtfOperative.StatusEngaging -> "engaging";
            case MtfOperative.StatusCover -> "cover";
            case MtfOperative.StatusRegroup -> "regroup";
            case MtfOperative.StatusRoping -> "roping";
            case MtfOperative.StatusBoarding -> "boarding";
            default -> "following";
        };
    }
}

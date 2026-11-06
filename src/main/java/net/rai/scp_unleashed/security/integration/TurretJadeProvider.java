package net.rai.scp_unleashed.security.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.security.SecurityRegistry;
import net.rai.scp_unleashed.security.SentryTurretBlockEntity;
import net.rai.scp_unleashed.security.TurretRound;
import net.rai.scp_unleashed.security.TurretStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum TurretJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = SecurityRegistry.location("sentry_turret");
    private static final String DataKey = "SentryTurret";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof SentryTurretBlockEntity Turret)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", (byte) Turret.getStatus().ordinal());
        Info.putInt("Rounds", Turret.rounds());
        TurretRound Round = Turret.loadedRound();
        Info.putInt("Round", Round == null ? -1 : Round.ordinal());
        Info.putString("Owner", Turret.getOwnerName());
        if (Turret.getTarget() != null) {
            Info.putString("Target", Component.Serializer.toJson(Turret.getTarget().getDisplayName(), Accessor.getLevel().registryAccess()));
        }
        Data.put(DataKey, Info);
    }

    private static Component targetName(String Serialized, BlockAccessor Accessor) {
        try {
            Component Parsed = Component.Serializer.fromJson(Serialized, Accessor.getLevel().registryAccess());
            return Parsed != null ? Parsed : Component.literal(Serialized);
        } catch (RuntimeException Unparsed) {
            return Component.literal(Serialized);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        TurretStatus Status = TurretStatus.byIndex(Info.getByte("Status"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.sentry_turret.status", Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour())));
        int Index = Info.getInt("Round");
        if (Index >= 0) {
            Component Calibre = Component.translatable("gui.scp_unleashed.sentry_turret.round." + TurretRound.byIndex(Index).name().toLowerCase(Locale.ROOT));
            Tooltip.add(Component.translatable("jade.scp_unleashed.sentry_turret.ammo", Info.getInt("Rounds"), Calibre).withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.sentry_turret.empty").withStyle(ChatFormatting.RED));
        }
        if (Info.contains("Target")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.sentry_turret.target", targetName(Info.getString("Target"), Accessor)).withStyle(ChatFormatting.RED));
        }
        String Owner = Info.getString("Owner");
        if (!Owner.isEmpty()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.sentry_turret.owner", Owner).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

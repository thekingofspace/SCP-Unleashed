package net.rai.scp_unleashed.dclass.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassNumbers;
import net.rai.scp_unleashed.scp012.Scp012Composer;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DClassJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass");
    private static final String DataKey = "DClass";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof DClass Subject) {
            CompoundTag Info = new CompoundTag();
            Info.putInt("Food", Subject.food().food());
            Info.putBoolean("Composer", Scp012Composer.isComposer(Subject));
            if (Subject.exposure().exposed()) {
                Info.putInt("Composing", Subject.exposure().meter());
            }
            Data.put(DataKey, Info);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof DClass Subject)) {
            return;
        }
        if (Subject.keepsRecords() && Subject.number() > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.filed", DClassNumbers.format(Subject.number())).withStyle(ChatFormatting.GOLD));
        } else if (Subject.keepsRecords()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.unfiled").withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.trait", Subject.trait().getName()).withStyle(ChatFormatting.GRAY));
        CompoundTag Info = Accessor.getServerData().getCompound(DataKey);
        if (Info.contains("Food")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.food", Info.getInt("Food")).withStyle(ChatFormatting.GRAY));
        }
        if (Info.getBoolean("Composer")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.composer").withStyle(ChatFormatting.DARK_RED));
        }
        if (Info.contains("Composing")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.composing", Info.getInt("Composing")).withStyle(ChatFormatting.RED));
        }
        if (Subject.isRestingPose()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.dclass.asleep").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}

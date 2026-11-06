package net.rai.scp_unleashed.scp407.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp407TitanProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp407Registry.location("scp407_titan");
    private static final String StateKey = "Scp407State";
    private static final String TargetKey = "Scp407Target";
    private static final String EatenKey = "Scp407Eaten";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof Scp407Titan Titan)) {
            return;
        }
        Data.putString(StateKey, Titan.stateKey());
        Data.putInt(EatenKey, Titan.eaten());
        LivingEntity Target = Titan.getTarget();
        if (Target != null && Target.isAlive()) {
            Data.putString(TargetKey, Target.getDisplayName().getString());
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(StateKey)) {
            return;
        }
        String State = Data.getString(StateKey);
        if (Data.contains(TargetKey) && State.equals("hunting")) {
            Tooltip.add(Component.translatable(Scp407SpeakerProvider.Prefix + "titan.hunting", Data.getString(TargetKey)).withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Scp407SpeakerProvider.Prefix + "titan." + State).withStyle(ChatFormatting.DARK_GREEN));
        }
        int Eaten = Data.getInt(EatenKey);
        if (Eaten > 0) {
            Tooltip.add(Component.translatable(Scp407SpeakerProvider.Prefix + "titan.eaten", Eaten).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}

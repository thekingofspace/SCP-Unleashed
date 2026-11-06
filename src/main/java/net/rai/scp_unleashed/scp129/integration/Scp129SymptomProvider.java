package net.rai.scp_unleashed.scp129.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp129.Scp129Course;
import net.rai.scp_unleashed.scp129.Scp129Infection;
import net.rai.scp_unleashed.scp129.Scp129Symptoms;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp129SymptomProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final int UnwellStage = 3;
    public static final int GraveStage = 4;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_symptoms");
    private static final String DataKey = "Scp129Signs";
    private static final String Prefix = "jade.scp_unleashed.scp129.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof LivingEntity Subject)) {
            return;
        }
        int Kind = Scp129Symptoms.activeKind(Subject);
        int Stage = Scp129Course.symptomStage(Scp129Infection.stateOf(Subject));
        if (Kind == Scp129Symptoms.Cough || Kind == Scp129Symptoms.Freeze || Stage >= UnwellStage) {
            CompoundTag Signs = new CompoundTag();
            Signs.putInt("Episode", Kind);
            Signs.putInt("Stage", Stage);
            Data.put(DataKey, Signs);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        CompoundTag Signs = Accessor.getServerData().getCompound(DataKey);
        if (Signs.isEmpty()) {
            return;
        }
        int Kind = Signs.getInt("Episode");
        if (Kind == Scp129Symptoms.Cough) {
            Tooltip.add(Component.translatable(Prefix + "coughing").withStyle(ChatFormatting.GRAY));
        } else if (Kind == Scp129Symptoms.Freeze) {
            Tooltip.add(Component.translatable(Prefix + "frozen").withStyle(ChatFormatting.GOLD));
        }
        int Stage = Signs.getInt("Stage");
        if (Stage >= GraveStage) {
            Tooltip.add(Component.translatable(Prefix + "grave").withStyle(ChatFormatting.DARK_RED));
        } else if (Stage >= UnwellStage) {
            Tooltip.add(Component.translatable(Prefix + "unwell").withStyle(ChatFormatting.RED));
        }
    }
}

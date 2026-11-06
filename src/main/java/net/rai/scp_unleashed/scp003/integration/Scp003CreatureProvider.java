package net.rai.scp_unleashed.scp003.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp003.Scp003Creature;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp003CreatureProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp003_creature");
    private static final String DataKey = "Scp003";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp003Creature Creature) {
            CompoundTag Details = new CompoundTag();
            Details.putInt("Stage", Creature.stage());
            Creature.writeJade(Details);
            Data.put(DataKey, Details);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp003Creature Creature)) {
            return;
        }
        int Stage = Creature.stage();
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.stage." + Stage).withStyle(Stage >= 4 ? ChatFormatting.DARK_PURPLE : ChatFormatting.RED));
        CompoundTag Data = Accessor.getServerData().getCompound(DataKey);
        if (Data.isEmpty()) {
            return;
        }
        switch (Stage) {
            case 1 -> {
                if (Data.getBoolean("Moulting")) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.moulting").withStyle(ChatFormatting.GOLD));
                } else {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.growth", Math.round(Data.getFloat("Growth") * 100.0f)).withStyle(ChatFormatting.GOLD));
                }
            }
            case 2 -> {
                if (Data.getBoolean("Moulting")) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.metamorph").withStyle(ChatFormatting.GOLD));
                } else {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.kills", Data.getInt("Kills"), Data.getInt("KillsNeeded")).withStyle(ChatFormatting.GOLD));
                }
            }
            case 3 -> {
                String Phase = Data.getString("Phase");
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.build." + Phase.toLowerCase(java.util.Locale.ROOT), Data.getInt("Grown")).withStyle(ChatFormatting.GOLD));
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.builds", Data.getInt("Builds"), Data.getInt("MaxBuilds")).withStyle(ChatFormatting.GRAY));
                int Minutes = Data.getInt("Cooldown") / 1200;
                if ("None".equals(Phase) && Data.getInt("Builds") < Data.getInt("MaxBuilds") && Minutes > 0) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.resting", Minutes).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
            default -> {
                Tooltip.add(Component.translatable(Data.getBoolean("Channeling") ? "jade.scp_unleashed.scp003.channeling" : "jade.scp_unleashed.scp003.alpha")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                Tooltip.add(Component.translatable("jade.scp_unleashed.scp003.healed", Data.getInt("Healed")).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}

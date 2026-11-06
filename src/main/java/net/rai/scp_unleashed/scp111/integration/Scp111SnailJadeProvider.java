package net.rai.scp_unleashed.scp111.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp111.Scp111Snail;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp111SnailJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp111_snail");
    private static final String Prefix = "jade.scp_unleashed.scp111.";
    private static final String FedKey = "Scp111Fed";
    private static final String RestKey = "Scp111Rest";
    private static final String LaidKey = "Scp111Laid";
    private static final String LeavingKey = "Scp111Leaving";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp111Snail Creature) {
            Data.putInt(FedKey, Creature.getFed());
            Data.putInt(RestKey, Creature.layCooldown());
            Data.putInt(LaidKey, Creature.getLaid());
            Data.putBoolean(LeavingKey, Creature.leavingFrom() != null);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp111Snail Creature)) {
            return;
        }
        Tooltip.add(Creature.getVariant().displayName());
        if (Creature.isBaby()) {
            Tooltip.add(Component.translatable(Prefix + "juvenile").withStyle(ChatFormatting.YELLOW));
        }
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(FedKey)) {
            return;
        }
        int Fed = Data.getInt(FedKey);
        String Hunger = Fed >= Scp111Snail.WellFed ? "well_fed" : Fed >= Scp111Snail.Content ? "content" : "hungry";
        ChatFormatting HungerColour = Fed >= Scp111Snail.WellFed ? ChatFormatting.GREEN : Fed >= Scp111Snail.Content ? ChatFormatting.GRAY : ChatFormatting.GOLD;
        Tooltip.add(Component.translatable(Prefix + "hunger", Component.translatable(Prefix + "hunger." + Hunger)).withStyle(HungerColour));
        if (!Creature.isBaby()) {
            String Eggs = Data.getInt(RestKey) > 0 ? "resting" : Fed >= Scp111Snail.Content ? "ready" : "hungry";
            Tooltip.add(Component.translatable(Prefix + "eggs", Component.translatable(Prefix + "eggs." + Eggs)).withStyle(ChatFormatting.GRAY));
            int Laid = Data.getInt(LaidKey);
            if (Laid > 0) {
                Tooltip.add(Component.translatable(Prefix + "laid", Laid).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (Data.getBoolean(LeavingKey)) {
            Tooltip.add(Component.translatable(Prefix + "leaving").withStyle(ChatFormatting.AQUA));
        }
    }
}

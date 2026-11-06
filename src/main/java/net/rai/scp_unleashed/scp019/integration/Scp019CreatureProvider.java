package net.rai.scp_unleashed.scp019.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp019.Scp019Brute;
import net.rai.scp_unleashed.scp019.Scp019Creature;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp019CreatureProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp019_creature");
    private static final String DataKey = "Scp019";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp019Creature Creature) {
            CompoundTag Details = new CompoundTag();
            Details.putString("State", Creature.state());
            BlockPos Pot = Creature.potPos();
            if (Creature.isOwned() && Pot != null) {
                Details.putInt("Distance", (int) Math.round(Math.sqrt(Creature.distanceToSqr(Pot.getX() + 0.5, Pot.getY(), Pot.getZ() + 0.5))));
            }
            Data.put(DataKey, Details);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp019Creature Creature)) {
            return;
        }
        boolean Heavy = Creature instanceof Scp019Brute;
        CompoundTag Data = Accessor.getServerData().getCompound(DataKey);
        if (Data.isEmpty()) {
            return;
        }
        String State = Data.getString("State");
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp019." + (Heavy ? "brute." : "drone.") + State)
                .withStyle(Heavy ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (Data.contains("Distance")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp019.distance", Data.getInt("Distance")).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp019.unbound").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}

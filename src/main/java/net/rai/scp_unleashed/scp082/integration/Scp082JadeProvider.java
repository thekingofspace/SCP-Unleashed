package net.rai.scp_unleashed.scp082.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp082.Scp082Fernand;
import net.rai.scp_unleashed.scp082.Scp082Mood;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp082JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final int BarCells = 10;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp082_fernand");
    private static final String DataKey = "Scp082";
    private static final String Prefix = "jade.scp_unleashed.scp082.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof Scp082Fernand Fernand) || !(Fernand.level() instanceof ServerLevel World)) {
            return;
        }
        CompoundTag Details = new CompoundTag();
        Details.putFloat("Hunger", Fernand.hunger());
        Details.putString("State", Fernand.stateKey());
        LivingEntity Held = Fernand.held(World);
        if (Held != null) {
            Details.putString("Held", Component.Serializer.toJson(Held.getDisplayName(), World.registryAccess()));
        }
        Data.put(DataKey, Details);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp082Fernand Fernand)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.DARK_RED));
        CompoundTag Data = Accessor.getServerData().getCompound(DataKey);
        Scp082Mood Mood = Fernand.mood();
        if (!Data.isEmpty()) {
            float Hunger = Data.getFloat("Hunger");
            Mood = Scp082Mood.of(Hunger);
            int Filled = Math.round(Hunger / 100.0f * BarCells);
            Component Bar = Component.literal("■".repeat(Math.max(0, Filled))).withStyle(Mood.Colour)
                    .append(Component.literal("■".repeat(Math.max(0, BarCells - Filled))).withStyle(ChatFormatting.DARK_GRAY));
            Tooltip.add(Component.translatable(Prefix + "hunger", Bar, Math.round(Hunger)).withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + "mood", Component.translatable(Prefix + "mood." + Mood.Key).withStyle(Mood.Colour)).withStyle(ChatFormatting.GRAY));
        if (Data.isEmpty()) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "state." + Data.getString("State")).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (Data.contains("Held")) {
            Component Name = Component.Serializer.fromJson(Data.getString("Held"), Accessor.getLevel().registryAccess());
            if (Name != null) {
                Tooltip.add(Component.translatable(Prefix + "held", Name).withStyle(ChatFormatting.RED));
            }
        }
    }
}

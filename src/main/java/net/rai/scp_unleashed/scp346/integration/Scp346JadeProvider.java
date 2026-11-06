package net.rai.scp_unleashed.scp346.integration;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp346.Scp346Pterosaur;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;
import java.util.UUID;

public enum Scp346JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp346");
    private static final String OwnerKey = "Scp346Owner";
    private static final String Prefix = "jade.scp_unleashed.scp346.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof Scp346Pterosaur Bird) || !Bird.isTame()) {
            return;
        }
        LivingEntity Owner = Bird.getOwner();
        if (Owner != null) {
            Data.putString(OwnerKey, Owner.getName().getString());
            return;
        }
        UUID Id = Bird.getOwnerUUID();
        MinecraftServer Server = Bird.getServer();
        if (Id != null && Server != null && Server.getProfileCache() != null) {
            Optional<GameProfile> Profile = Server.getProfileCache().get(Id);
            Profile.ifPresent(Found -> Data.putString(OwnerKey, Found.getName()));
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp346Pterosaur Bird)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "colouring", Bird.getVariant().displayName()).withStyle(ChatFormatting.GRAY));
        if (Bird.isTame()) {
            CompoundTag Data = Accessor.getServerData();
            if (Data.contains(OwnerKey)) {
                Tooltip.remove(JadeIds.MC_ANIMAL_OWNER);
                Tooltip.add(Component.translatable(Prefix + "owner", Data.getString(OwnerKey)).withStyle(ChatFormatting.GREEN));
            } else {
                Tooltip.add(Component.translatable(Prefix + "tame").withStyle(ChatFormatting.GREEN));
            }
            if (Bird.isOrderedToSit()) {
                Tooltip.add(Component.translatable(Prefix + "sitting").withStyle(ChatFormatting.GRAY));
            }
        } else {
            Tooltip.add(Component.translatable(Prefix + "wild").withStyle(ChatFormatting.GRAY));
        }
        if (Bird.isRoosting()) {
            Tooltip.add(Component.translatable(Prefix + "asleep").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
    }
}

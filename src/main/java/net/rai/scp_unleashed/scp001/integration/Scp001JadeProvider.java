package net.rai.scp_unleashed.scp001.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001.Scp001Prototype;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp001JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001_prototype");
    private static final String Prefix = "jade.scp_unleashed.scp001.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp001Prototype Creature)) {
            return;
        }
        String State = Creature.isLocked() ? "dazed" : Creature.isRestrained() ? "held" : Creature.isAggressive() ? "hunting" : "prowling";
        ChatFormatting Colour = Creature.isLocked() ? ChatFormatting.AQUA : Creature.isRestrained() ? ChatFormatting.GREEN : ChatFormatting.RED;
        Tooltip.add(Component.translatable(Prefix + "state", Component.translatable(Prefix + "state." + State).withStyle(Colour)).withStyle(ChatFormatting.GRAY));
    }
}

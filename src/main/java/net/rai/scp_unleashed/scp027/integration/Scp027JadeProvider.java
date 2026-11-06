package net.rai.scp_unleashed.scp027.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp027.Scp027Entity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp027JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp027");
    private static final String Prefix = "jade.scp_unleashed.scp027.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp027Entity Subject)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.RED));
        String State = Subject.isStriding() ? "shuffling" : "standing";
        Tooltip.add(Component.translatable(Prefix + State).withStyle(ChatFormatting.GRAY));
        int Swarm = Subject.swarmSize();
        String Crowd = Swarm >= 7 ? "swarm.thick" : Swarm >= 3 ? "swarm.some" : Swarm > 0 ? "swarm.few" : "swarm.none";
        Tooltip.add(Component.translatable(Prefix + Crowd).withStyle(ChatFormatting.DARK_GRAY));
    }
}

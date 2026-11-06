package net.rai.scp_unleashed.scp427.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp427.Scp427Flesh;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp427FleshJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp427_1");
    private static final String Prefix = "jade.scp_unleashed.scp427_1.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp427Flesh Creature)) {
            return;
        }
        String State = Creature.stateKey();
        ChatFormatting Colour = switch (State) {
            case "lunging", "slamming", "roaring", "hunting" -> ChatFormatting.RED;
            case "collapsing" -> ChatFormatting.DARK_GRAY;
            default -> ChatFormatting.GOLD;
        };
        Tooltip.add(Component.translatable(Prefix + "state", Component.translatable(Prefix + "state." + State).withStyle(Colour)).withStyle(ChatFormatting.GRAY));
        if (Creature.isAlive() && Creature.getHealth() < Creature.getMaxHealth()) {
            Tooltip.add(Component.translatable(Prefix + "mending").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}

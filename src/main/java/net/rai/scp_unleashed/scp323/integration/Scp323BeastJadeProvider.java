package net.rai.scp_unleashed.scp323.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp323.Scp323Beast;
import net.rai.scp_unleashed.scp323.Scp323Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp323BeastJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp323Registry.location("scp323");
    private static final String Prefix = "jade.scp_unleashed.scp323.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp323Beast Creature)) {
            return;
        }
        String State = Creature.stateKey();
        ChatFormatting Colour = switch (State) {
            case "emerging", "receding" -> ChatFormatting.DARK_PURPLE;
            case "howling", "hunting" -> ChatFormatting.RED;
            default -> ChatFormatting.GOLD;
        };
        Tooltip.add(Component.translatable(Prefix + "state", Component.translatable(Prefix + "state." + State).withStyle(Colour)).withStyle(ChatFormatting.GRAY));
    }
}

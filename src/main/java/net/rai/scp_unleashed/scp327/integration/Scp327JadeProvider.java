package net.rai.scp_unleashed.scp327.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp327.Scp327Siren;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp327JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_siren");
    private static final String Prefix = "jade.scp_unleashed.scp327.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp327Siren Creature)) {
            return;
        }
        String State = switch (Creature.mode()) {
            case Scp327Siren.ModeSing, Scp327Siren.ModeBeachSing -> "singing";
            case Scp327Siren.ModeBeached, Scp327Siren.ModeHaul -> "beached";
            case Scp327Siren.ModeDrag -> "dragging";
            case Scp327Siren.ModeStranded -> "stranded";
            default -> "swimming";
        };
        ChatFormatting Colour = switch (State) {
            case "singing" -> ChatFormatting.AQUA;
            case "dragging" -> ChatFormatting.DARK_RED;
            case "stranded" -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
        Tooltip.add(Component.translatable(Prefix + "state." + State).withStyle(Colour));
        if (Creature.isPup()) {
            Tooltip.add(Component.translatable(Prefix + "stage." + (Creature.isBaby() ? "baby" : "teen")).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
        if (!Creature.isInWater() && Creature.moistureShare() < 0.5f) {
            Tooltip.add(Component.translatable(Prefix + "drying").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        }
    }
}

package net.rai.scp_unleashed.scp457.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp457.Scp457;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp457JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp457");
    private static final String TargetKey = "Scp457Target";
    private static final String Prefix = "jade.scp_unleashed.scp457.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp457 Burning) {
            LivingEntity Target = Burning.getTarget();
            if (Target != null && Target.isAlive()) {
                Data.putString(TargetKey, Target.getDisplayName().getString());
            }
        }
    }

    public static String stateKey(Scp457 Burning, boolean Hunting) {
        if (Burning.isRising()) {
            return "kindling";
        }
        if (Burning.isSteaming()) {
            return "hissing";
        }
        return Hunting ? "hunting" : "smouldering";
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp457 Burning)) {
            return;
        }
        CompoundTag Data = Accessor.getServerData();
        boolean Hunting = Data.contains(TargetKey);
        String Key = stateKey(Burning, Hunting);
        ChatFormatting Colour = switch (Key) {
            case "hunting" -> ChatFormatting.RED;
            case "kindling" -> ChatFormatting.YELLOW;
            case "hissing" -> ChatFormatting.GRAY;
            default -> ChatFormatting.GOLD;
        };
        Component State = Component.translatable(Prefix + "state." + Key);
        if (Hunting && Key.equals("hunting")) {
            Tooltip.add(Component.translatable(Prefix + "state.target", State, Data.getString(TargetKey)).withStyle(Colour));
        } else {
            Tooltip.add(Component.translatable(Prefix + "state", State).withStyle(Colour));
        }
        Component Origin = Component.translatable(Prefix + "instance." + instanceKey(Burning));
        Tooltip.add(Component.translatable(Prefix + "instance", Origin).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static String instanceKey(Scp457 Burning) {
        return Burning.isPrime() ? "prime" : "offshoot";
    }
}

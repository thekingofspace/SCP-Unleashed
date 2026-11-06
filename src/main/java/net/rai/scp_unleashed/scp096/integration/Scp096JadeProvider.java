package net.rai.scp_unleashed.scp096.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp096JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp096_shy_guy");
    private static final String DataKey = "Scp096";
    private static final String Prefix = "jade.scp_unleashed.scp096.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (!(Accessor.getEntity() instanceof Scp096ShyGuy Guy)) {
            return;
        }
        CompoundTag Details = new CompoundTag();
        Details.putString("State", Guy.stateKey());
        Details.putInt("Targets", Guy.targetKeys().size());
        Data.put(DataKey, Details);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp096ShyGuy Guy)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withStyle(ChatFormatting.GOLD));
        CompoundTag Data = Accessor.getServerData().getCompound(DataKey);
        String State = Data.contains("State") ? Data.getString("State") : Guy.stateKey();
        ChatFormatting Colour = switch (State) {
            case "enraged" -> ChatFormatting.DARK_RED;
            case "distressed" -> ChatFormatting.RED;
            case "calming", "crying" -> ChatFormatting.YELLOW;
            default -> ChatFormatting.GREEN;
        };
        Tooltip.add(Component.translatable(Prefix + "state", Component.translatable(Prefix + "state." + State).withStyle(Colour)).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Guy.isBagged() ? Prefix + "bagged" : Prefix + "unbagged").withStyle(Guy.isBagged() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY));
        int Targets = Data.contains("Targets") ? Data.getInt("Targets") : Guy.targetCount();
        if (Targets > 0 && (Guy.isRaging() || Guy.isDistressed())) {
            Tooltip.add(Component.translatable(Prefix + "targets", Targets).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }
}

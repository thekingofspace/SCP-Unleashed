package net.rai.scp_unleashed.scp2845.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp2845.Scp2845;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;
import snownee.jade.api.Accessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IBoxElement;

public enum Scp2845JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp2845.";
    public static final String DataKey = "Scp2845";
    public static final int Wandering = 0;
    public static final int Hostile = 1;
    public static final int Still = 2;
    public static final int Contained = 3;
    public static final int Uncontained = 4;
    private static final ResourceLocation Uid = Scp2845Registry.location("scp2845");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    public static int state(Scp2845 Owner) {
        return switch (Owner.mode()) {
            case Scp2845.Contained -> Contained;
            case Scp2845.Breaking -> Uncontained;
            case Scp2845.Bowing -> Still;
            case Scp2845.Recalled -> Wandering;
            case Scp2845.Arriving -> Hostile;
            default -> Owner.getTarget() != null ? Hostile : Wandering;
        };
    }

    public static String stateKey(int State) {
        return switch (State) {
            case Hostile -> "hostile";
            case Still -> "still";
            case Contained -> "contained";
            case Uncontained -> "uncontained";
            default -> "wandering";
        };
    }

    public static ChatFormatting stateStyle(int State) {
        return switch (State) {
            case Hostile, Uncontained -> ChatFormatting.RED;
            case Contained -> ChatFormatting.GREEN;
            case Still -> ChatFormatting.GRAY;
            default -> ChatFormatting.YELLOW;
        };
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp2845 Owner) {
            CompoundTag Info = new CompoundTag();
            Info.putInt("State", state(Owner));
            Data.put(DataKey, Info);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp2845 Owner)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withColor(DocumentClass.Keter.getColour() & 0xFFFFFF));
        CompoundTag Info = Accessor.getServerData().getCompound(DataKey);
        int State = Info.contains("State") ? Info.getInt("State") : state(Owner);
        Tooltip.add(Component.translatable(Prefix + stateKey(State)).withStyle(stateStyle(State)));
    }

    public static void collected(IBoxElement Root, Accessor<?> Found) {
        if (!(Found instanceof EntityAccessor Looked) || !(Looked.getEntity() instanceof Scp2845)) {
            return;
        }
        Root.getTooltip().replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(Component.translatable(Prefix + "title")));
        Root.getTooltip().remove(JadeIds.MC_ENTITY_HEALTH);
    }
}

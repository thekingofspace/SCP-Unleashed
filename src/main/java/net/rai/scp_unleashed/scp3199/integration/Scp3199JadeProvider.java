package net.rai.scp_unleashed.scp3199.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp3199.Scp3199;
import net.rai.scp_unleashed.scp3199.Scp3199Registry;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp3199JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp3199.";
    public static final String DataKey = "Scp3199";
    public static final int Hunting = 1;
    public static final int Roaming = 2;
    public static final int Settled = 3;
    public static final int Heaving = 4;
    public static final int Swimming = 5;
    private static final ResourceLocation Uid = Scp3199Registry.location("scp3199");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    public static int state(Scp3199 Owner) {
        if (Owner.isLaying()) {
            return Heaving;
        }
        if (Owner.isInWater()) {
            return Swimming;
        }
        if (Owner.getTarget() != null) {
            return Hunting;
        }
        return Owner.nesting().isSettled() ? Settled : Roaming;
    }

    public static String stateKey(int State) {
        return switch (State) {
            case Hunting -> "hunting";
            case Settled -> "settled";
            case Heaving -> "heaving";
            case Swimming -> "swimming";
            default -> "roaming";
        };
    }

    public static ChatFormatting stateStyle(int State) {
        return switch (State) {
            case Hunting, Heaving -> ChatFormatting.RED;
            case Settled -> ChatFormatting.GOLD;
            case Swimming -> ChatFormatting.AQUA;
            default -> ChatFormatting.YELLOW;
        };
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp3199 Owner) {
            CompoundTag Info = new CompoundTag();
            Info.putInt("State", state(Owner));
            Data.put(DataKey, Info);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp3199 Owner)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "class").withColor(DocumentClass.Keter.getColour() & 0xFFFFFF));
        Tooltip.add(Component.translatable(Prefix + "health", Mth.ceil(Owner.getHealth()), Mth.ceil(Owner.getMaxHealth())).withStyle(ChatFormatting.GRAY));
        CompoundTag Info = Accessor.getServerData().getCompound(DataKey);
        int State = Owner.isLaying() ? Heaving : Info.contains("State") ? Info.getInt("State") : Roaming;
        Tooltip.add(Component.translatable(Prefix + stateKey(State)).withStyle(stateStyle(State)));
        if (!Owner.isAdult()) {
            int Share = Mth.floor(100.0f * Owner.growth() / (float) Scp3199.GrowTicks);
            Tooltip.add(Component.translatable(Prefix + "maturing", Share).withStyle(ChatFormatting.GRAY));
        }
    }
}

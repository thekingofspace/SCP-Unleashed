package net.rai.scp_unleashed.scp337.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp337.Scp337Commands;
import net.rai.scp_unleashed.scp337.Scp337Mass;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp337JadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp337");
    private static final String TargetKey = "Scp337Target";
    private static final String Prefix = "jade.scp_unleashed.scp337.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, EntityAccessor Accessor) {
        if (Accessor.getEntity() instanceof Scp337Mass Mass) {
            LivingEntity Target = Mass.getTarget();
            if (Target != null && Target.isAlive()) {
                Data.putString(TargetKey, Target.getDisplayName().getString());
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp337Mass Mass)) {
            return;
        }
        byte Current = Mass.state();
        CompoundTag Data = Accessor.getServerData();
        ChatFormatting Colour = switch (Current) {
            case Scp337Mass.StateLashing -> ChatFormatting.RED;
            case Scp337Mass.StateReaching -> ChatFormatting.GOLD;
            case Scp337Mass.StateSettling -> ChatFormatting.DARK_GRAY;
            default -> ChatFormatting.GRAY;
        };
        Component State = Component.translatable(Prefix + "state." + Scp337Commands.stateKey(Current));
        if (Data.contains(TargetKey) && (Current == Scp337Mass.StateLashing || Current == Scp337Mass.StateReaching)) {
            Tooltip.add(Component.translatable(Prefix + "state.target", State, Data.getString(TargetKey)).withStyle(Colour));
        } else {
            Tooltip.add(Component.translatable(Prefix + "state", State).withStyle(Colour));
        }
    }
}

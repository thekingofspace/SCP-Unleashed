package net.rai.scp_unleashed.reactor.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.reactor.GridState;
import net.rai.scp_unleashed.reactor.ReactorControllerBlockEntity;
import net.rai.scp_unleashed.reactor.ReactorGrid;
import net.rai.scp_unleashed.reactor.TowerHeat;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum ControllerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "reactor_controller");
    private static final String DataKey = "ReactorController";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World) || !(Accessor.getBlockEntity() instanceof ReactorControllerBlockEntity Controller)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        ReactorGrid Grid = Controller.resolve(World);
        Info.putBoolean("Linked", Controller.isLinked());
        BlockPos Link = Controller.getLink();
        if (Link != null) {
            Info.putLong("Link", Link.asLong());
        }
        Info.putByte("State", (byte) (Grid == null ? GridState.Missing : Grid.getState()).ordinal());
        if (Grid != null) {
            Info.putInt("Towers", Grid.getTowers().size());
            Info.putFloat("Steam", Grid.getCore().totalSteam());
            Info.putFloat("Hottest", Grid.getCore().hottest());
            Info.putFloat("Weakest", Grid.getCore().weakest());
            Info.putBoolean("Scram", Grid.isScramHeld(World.getGameTime()));
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        GridState State = GridState.byIndex(Info.getByte("State"));
        if (State == GridState.Missing) {
            Tooltip.add(Component.translatable(Info.getBoolean("Linked") ? "jade.scp_unleashed.reactor_controller.lost" : "jade.scp_unleashed.reactor_controller.unlinked")
                    .withStyle(ChatFormatting.GOLD));
            return;
        }
        BlockPos Link = BlockPos.of(Info.getLong("Link"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.reactor_controller.linked", Info.getInt("Towers"), Link.getX(), Link.getY(), Link.getZ()).withStyle(ChatFormatting.GRAY));
        if (State != GridState.Ready) {
            Tooltip.add(Component.translatable(State.getTranslationKey()).withStyle(State.getColour()));
        }
        TowerHeat Heat = TowerHeat.of(Info.getFloat("Hottest"), Info.getFloat("Weakest"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.reactor_controller.hottest", Math.round(Info.getFloat("Hottest")), Component.translatable(Heat.getTranslationKey()))
                .withStyle(Heat.getColour()));
        Tooltip.add(Component.translatable("jade.scp_unleashed.reactor_controller.steam", String.format(Locale.ROOT, "%.1f", Info.getFloat("Steam"))).withStyle(ChatFormatting.AQUA));
        if (Info.getBoolean("Scram")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.reactor_controller.scram").withStyle(ChatFormatting.RED));
        }
    }
}

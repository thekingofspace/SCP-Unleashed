package net.rai.scp_unleashed.warhead.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.warhead.AlphaWarheadBlock;
import net.rai.scp_unleashed.warhead.PanelStage;
import net.rai.scp_unleashed.warhead.WarheadNetwork;
import net.rai.scp_unleashed.warhead.WarheadPanelBlock;
import net.rai.scp_unleashed.warhead.WarheadPanelBlockEntity;
import net.rai.scp_unleashed.warhead.WarheadSequence;
import net.rai.scp_unleashed.warhead.WarheadStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Optional;

public enum WarheadJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Alpha("warhead_alpha"),
    Panel("warhead_panel");

    private static final String DataKey = "Warhead";
    private final ResourceLocation Uid;

    WarheadJadeProvider(String Path) {
        this.Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        BlockState State = Accessor.getBlockState();
        CompoundTag Info = new CompoundTag();
        long Now = World.getGameTime();
        WarheadNetwork Network = WarheadNetwork.get(World);
        if (this == Alpha && State.getBlock() instanceof AlphaWarheadBlock) {
            BlockPos Origin = AlphaWarheadBlock.masterOf(Accessor.getPosition(), State);
            Info.putString("Status", Network.statusFor(World, Origin).getSerializedName());
            Info.putInt("Panels", Network.panelsNear(World, Origin, WarheadNetwork.LinkRange));
            Optional<WarheadSequence> Running = Network.sequenceTargeting(Origin);
            Running.ifPresent(Sequence -> Info.putInt("Left", Sequence.ticksLeft(Now)));
        } else if (this == Panel && State.getBlock() instanceof WarheadPanelBlock) {
            WarheadPanelBlockEntity Console = WarheadPanelBlock.masterEntity(World, Accessor.getPosition(), State);
            if (Console == null) {
                return;
            }
            Info.putString("Stage", Console.stage().getSerializedName());
            Info.putInt("Linked", Console.getLinked());
            Info.putBoolean("Foreign", Console.isForeign());
            if (Console.isEmitting()) {
                Info.putInt("Left", Console.ticksLeft(Now));
            }
        } else {
            return;
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
        if (this == Alpha) {
            String Status = Info.getString("Status");
            ChatFormatting Colour = Status.equals(WarheadStatus.Idle.getSerializedName()) ? ChatFormatting.GREEN
                    : Status.equals(WarheadStatus.Armed.getSerializedName()) ? ChatFormatting.GOLD : ChatFormatting.RED;
            Tooltip.add(Component.translatable("jade.scp_unleashed.warhead.status", Component.translatable("jade.scp_unleashed.warhead.status." + Status)).withStyle(Colour));
            Tooltip.add(Component.translatable("jade.scp_unleashed.warhead.panels", Info.getInt("Panels")).withStyle(ChatFormatting.GRAY));
        } else {
            String Stage = Info.getString("Stage");
            ChatFormatting Colour = Stage.equals(PanelStage.Sealed.getSerializedName()) ? ChatFormatting.GRAY
                    : Stage.equals(PanelStage.Counting.getSerializedName()) || Stage.equals(PanelStage.Locked.getSerializedName()) ? ChatFormatting.RED : ChatFormatting.GOLD;
            Tooltip.add(Component.translatable("jade.scp_unleashed.warhead.panel_stage", Component.translatable("jade.scp_unleashed.warhead.stage." + Stage)).withStyle(Colour));
            int Linked = Info.getInt("Linked");
            Tooltip.add(Component.translatable("jade.scp_unleashed.warhead.linked", Linked, (int) WarheadNetwork.LinkRange).withStyle(Linked > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
            if (Info.getBoolean("Foreign")) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.warhead.remote").withStyle(ChatFormatting.DARK_RED));
            }
        }
        if (Info.contains("Left")) {
            int Seconds = (Info.getInt("Left") + 19) / 20;
            Tooltip.add(Component.translatable("jade.scp_unleashed.warhead.countdown", Seconds).withStyle(ChatFormatting.RED));
            if (Seconds <= WarheadNetwork.LockTicks / 20) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.warhead.cancel_locked").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }
}

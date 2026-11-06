package net.rai.scp_unleashed.rifttech.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlock;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.GridStatus;
import net.rai.scp_unleashed.rifttech.GridTuning;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlock;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlockEntity;
import net.rai.scp_unleashed.rifttech.PylonGrid;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum PylonJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "instability_pylon");
    private static final String DataKey = "InstabilityPylon";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        InstabilityPylonBlockEntity Pylon = InstabilityPylonBlock.baseEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Pylon == null) {
            return;
        }
        InstabilityPylonBlockEntity Control = Pylon.controller();
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Pylon.viewStatus().getSerializedName());
        Info.putInt("Issue", Pylon.viewIssue().ordinal());
        BlockPos Where = Pylon.viewIssueAt();
        if (Where != null) {
            Info.putLong("Where", Where.asLong());
        }
        Info.putInt("Tier", Control.getTier().getTier());
        Info.putBoolean("Apocalyptic", Control.isApocalypticMode());
        Info.putInt("Charge", Control.isApocalypticMode() ? Control.apocalypticScaled() : Control.chargeScaled());
        Info.putInt("Eta", Control.etaSeconds());
        PylonGrid.Layout Grid = Control.getGrid();
        if (Grid != null) {
            Info.putInt("SizeX", Grid.SizeX());
            Info.putInt("SizeZ", Grid.SizeZ());
            Info.putInt("Rise", Grid.Rise());
            Info.putInt("Rifts", Control.getRiftsInside());
            Info.putInt("Cap", Grid.cap());
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
        GridStatus Status = GridStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.instability_pylon.status", Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour())));
        PylonGrid.Problem Issue = PylonGrid.Problem.byIndex(Info.getInt("Issue"));
        if (Issue != PylonGrid.Problem.None) {
            if (Info.contains("Where") && Issue.hasSpot()) {
                BlockPos Where = BlockPos.of(Info.getLong("Where"));
                Component Spot = Component.translatable("jade.scp_unleashed.instability_pylon.spot", Where.getX(), Where.getY(), Where.getZ());
                Tooltip.add(Component.translatable(Issue.getTranslationKey() + ".at", Spot).withStyle(ChatFormatting.GOLD));
            } else {
                Tooltip.add(Component.translatable(Issue.getTranslationKey()).withStyle(ChatFormatting.GOLD));
            }
        }
        if (Info.contains("SizeX")) {
            Component Size = Info.getInt("Rise") > 0
                    ? Component.translatable("gui.scp_unleashed.instability_pylon.size_box", Info.getInt("SizeX"), Info.getInt("SizeZ"), Info.getInt("Rise"))
                    : Component.translatable("gui.scp_unleashed.instability_pylon.size_flat", Info.getInt("SizeX"), Info.getInt("SizeZ"));
            Tooltip.add(Size.copy().withStyle(ChatFormatting.GRAY));
            boolean Apocalyptic = Info.getBoolean("Apocalyptic");
            Component Mode = Apocalyptic
                    ? Component.translatable("gui.scp_unleashed.instability_pylon.apocalyptic").withStyle(Style.EMPTY.withColor(ApocalypticRiftEntity.Colour & 0xFFFFFF))
                    : Component.translatable(InstabilityMatrixBlock.tierKey(FragmentRarity.byTier(Info.getInt("Tier")))).withStyle(Style.EMPTY.withColor(FragmentRarity.byTier(Info.getInt("Tier")).getColour() & 0xFFFFFF));
            Tooltip.add(Component.translatable("jade.scp_unleashed.instability_pylon.mode", Mode));
            int Percent = Math.round(Info.getInt("Charge") * 100.0f / GridTuning.Scale);
            Tooltip.add(Component.translatable("jade.scp_unleashed.instability_pylon.charge", Percent, GridTuning.clock(Info.getInt("Eta"))).withStyle(ChatFormatting.GRAY));
            if (!Apocalyptic) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.instability_pylon.rifts", Info.getInt("Rifts"), Info.getInt("Cap")).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}

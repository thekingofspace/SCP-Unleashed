package net.rai.scp_unleashed.powerbridge.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.powerbridge.PowerBridgeBlock;
import net.rai.scp_unleashed.powerbridge.PowerBridgeBlockEntity;
import net.rai.scp_unleashed.powerbridge.PowerBridgeConversion;
import net.rai.scp_unleashed.powerbridge.PowerBridgeKind;
import net.rai.scp_unleashed.powerbridge.PowerBridgeRegistry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum PowerBridgeJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = PowerBridgeRegistry.location(PowerBridgeRegistry.BridgeId);
    private static final String Prefix = "jade.scp_unleashed." + PowerBridgeRegistry.BridgeId + ".";
    private static final String DataKey = "PowerBridge";
    private static final int InboundColour = 0xFF3FB8D0;
    private static final int InboundShade = 0xFF16566A;
    private static final int OutboundColour = 0xFFE28A2E;
    private static final int OutboundShade = 0xFF6E3A10;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof PowerBridgeBlockEntity Bridge)) {
            return;
        }
        CompoundTag Status = new CompoundTag();
        Status.putInt("Fe", Bridge.getShownFe());
        Status.putInt("Units", Bridge.getShownUnits());
        Status.putInt("Buffer", Bridge.getBufferFe());
        Status.putInt("Line", Bridge.getLineCapacity());
        Status.putBoolean("Linked", Bridge.isLinked());
        Status.putBoolean("Neighbour", Bridge.hasNeighbour());
        Data.put(DataKey, Status);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlock() instanceof PowerBridgeBlock Bridge) || !Accessor.getServerData().contains(DataKey)) {
            return;
        }
        PowerBridgeKind Kind = Bridge.getKind();
        String Side = Prefix + Kind.getSerializedName() + ".";
        CompoundTag Status = Accessor.getServerData().getCompound(DataKey);
        boolean Active = Accessor.getBlockState().getValue(PowerBridgeBlock.Active);
        boolean Linked = Status.getBoolean("Linked");
        boolean Neighbour = Status.getBoolean("Neighbour");
        ChatFormatting Tone = Kind.takesFe() ? ChatFormatting.AQUA : ChatFormatting.GOLD;
        if (!Linked) {
            Tooltip.add(Component.translatable(Prefix + "no_network").withStyle(ChatFormatting.RED));
        }
        if (!Neighbour) {
            Tooltip.add(Component.translatable(Side + "no_neighbour").withStyle(ChatFormatting.GOLD));
        }
        if (Linked && Neighbour) {
            Tooltip.add(Component.translatable(Prefix + (Active ? "converting" : "idle")).withStyle(Active ? Tone : ChatFormatting.GRAY));
        }
        int Fe = Status.getInt("Fe");
        int Units = Status.getInt("Units");
        Tooltip.add(Component.translatable(Side + "in", Kind.takesFe() ? Fe : Units).withStyle(Active ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Side + "out", Kind.takesFe() ? Units : Fe).withStyle(Active ? Tone : ChatFormatting.GRAY));
        int Capacity = PowerBridgeBlockEntity.BufferCapacity;
        int Buffer = Math.min(Status.getInt("Buffer"), Capacity);
        IElementHelper Helper = IElementHelper.get();
        Tooltip.add(Helper.progress((float) Buffer / Capacity, Component.translatable(Prefix + "buffer", String.format("%,d", Buffer), String.format("%,d", Capacity)),
                Helper.progressStyle().color(Kind.takesFe() ? InboundColour : OutboundColour, Kind.takesFe() ? InboundShade : OutboundShade).textColor(0xFFFFFFFF),
                BoxStyle.getNestedBox(), true));
        if (Linked) {
            Tooltip.add(Component.translatable(Prefix + "line", String.format("%,d", Status.getInt("Line"))).withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Side + "rate", PowerBridgeConversion.Sample, PowerBridgeConversion.sampleOut(Kind)).withStyle(ChatFormatting.DARK_GRAY));
    }
}

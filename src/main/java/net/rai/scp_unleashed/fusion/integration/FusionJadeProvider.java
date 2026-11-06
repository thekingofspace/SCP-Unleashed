package net.rai.scp_unleashed.fusion.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fusion.FusionBus;
import net.rai.scp_unleashed.fusion.FusionLayout;
import net.rai.scp_unleashed.fusion.FusionNotice;
import net.rai.scp_unleashed.fusion.FusionPhase;
import net.rai.scp_unleashed.fusion.FusionReactorBlock;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum FusionJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fusion_reactor");
    private static final String DataKey = "FusionReactor";
    private static final int ChargeColour = 0xFF2FB8C8;
    private static final int ChargeShade = 0xFF15606A;
    private static final int WaterColour = 0xFF3A78D8;
    private static final int WaterShade = 0xFF1A3A78;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof FusionReactorBlock)) {
            return;
        }
        FusionReactorBlockEntity Reactor = FusionReactorBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), State);
        if (Reactor == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Phase", (byte) Reactor.getPhase().ordinal());
        Info.putByte("Notice", (byte) Reactor.getNotice().ordinal());
        Info.putShort("Stability", (short) Reactor.getStability());
        Info.putInt("Charge", Reactor.getCharge());
        Info.putInt("Water", Reactor.getWaterAmount());
        Info.putInt("Units", Reactor.getFuelUnits());
        Info.putShort("Fuel", (short) Reactor.getFuelPermille());
        Info.putInt("Output", Reactor.getLastDelivered());
        Info.putInt("Intake", Reactor.getLastIntake());
        int Part = State.getValue(FusionReactorBlock.Part);
        if (Part == FusionLayout.InputPart || Part == FusionLayout.OutputPart) {
            FusionBus.Rating Bus = Part == FusionLayout.InputPart ? Reactor.getInputBus() : Reactor.getOutputBus();
            Info.putString("Port", Part == FusionLayout.InputPart ? "input" : "output");
            Info.putInt("Rating", Bus.Capacity());
            Info.putInt("Outlets", Bus.Outlets());
        } else if (Part == FusionLayout.InletPart) {
            Info.putString("Port", "inlet");
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
        if (Info.contains("Port")) {
            String Port = Info.getString("Port");
            Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.port_" + Port).withStyle(ChatFormatting.AQUA));
            if (Info.contains("Rating")) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.bus", grouped(Info.getInt("Rating")), Info.getInt("Outlets")).withStyle(ChatFormatting.GRAY));
            }
        }
        FusionPhase Phase = FusionPhase.byIndex(Info.getByte("Phase"));
        FusionNotice Notice = FusionNotice.byIndex(Info.getByte("Notice"));
        if (Phase == FusionPhase.Offline && Notice != FusionNotice.None) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.status_notice", Component.translatable(Phase.getTranslationKey()), Component.translatable(Notice.getTranslationKey())).withStyle(Phase.getColour()));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.status", Component.translatable(Phase.getTranslationKey())).withStyle(Phase.getColour()));
        }
        IElementHelper Helper = IElementHelper.get();
        if (Phase.isLit()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.stability", Math.round(Info.getShort("Stability") / 10.0f)));
        }
        if (Phase == FusionPhase.Charging) {
            int Charge = Info.getInt("Charge");
            Tooltip.add(Helper.progress(Charge / (float) FusionReactorBlockEntity.StartupEnergy,
                    Component.translatable("jade.scp_unleashed.fusion.charge", grouped(Charge), grouped(FusionReactorBlockEntity.StartupEnergy)),
                    Helper.progressStyle().color(ChargeColour, ChargeShade).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
            Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.intake", grouped(Info.getInt("Intake"))));
        }
        if (Phase == FusionPhase.Running || Info.getInt("Output") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.output", grouped(Info.getInt("Output")), grouped(FusionReactorBlockEntity.MaxOutput)));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.fusion.fuel", Info.getInt("Units"), Math.round(Info.getShort("Fuel") / 10.0f)));
        int Water = Info.getInt("Water");
        Tooltip.add(Helper.progress(Water / (float) FusionReactorBlockEntity.WaterCapacity,
                Component.translatable("jade.scp_unleashed.fusion.water", grouped(Water), grouped(FusionReactorBlockEntity.WaterCapacity)),
                Helper.progressStyle().color(WaterColour, WaterShade).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
    }

    private static String grouped(long Value) {
        return FusionReactorBlock.grouped(Value);
    }
}

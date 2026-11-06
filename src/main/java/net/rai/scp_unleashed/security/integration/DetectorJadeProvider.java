package net.rai.scp_unleashed.security.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.security.MotionDetectorBlock;
import net.rai.scp_unleashed.security.MotionDetectorBlockEntity;
import net.rai.scp_unleashed.security.SecurityRegistry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DetectorJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = SecurityRegistry.location("motion_detector");
    private static final String[] ModeKeys = {"gui.scp_unleashed.motion_detector.mode.pulse", "gui.scp_unleashed.motion_detector.mode.hold"};
    private static final String[] FilterKeys = {"gui.scp_unleashed.motion_detector.filter.all", "gui.scp_unleashed.motion_detector.filter.players",
            "gui.scp_unleashed.motion_detector.filter.mobs"};

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof MotionDetectorBlockEntity Detector)) {
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.port", String.format("%04d", Detector.getPort())));
        Tooltip.add(Component.translatable("jade.scp_unleashed.motion_detector.setup", Detector.getRange(), Component.translatable(ModeKeys[Detector.getMode()]),
                Component.translatable(FilterKeys[Detector.getFilter()])).withStyle(ChatFormatting.GRAY));
        boolean Tripped = Accessor.getBlockState().hasProperty(MotionDetectorBlock.Tripped) && Accessor.getBlockState().getValue(MotionDetectorBlock.Tripped);
        Tooltip.add(Component.translatable(Tripped ? "gui.scp_unleashed.motion_detector.status.tripped" : "gui.scp_unleashed.motion_detector.status.watching")
                .withStyle(Tripped ? ChatFormatting.RED : ChatFormatting.GREEN));
    }
}

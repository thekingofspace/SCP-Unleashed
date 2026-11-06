package net.rai.scp_unleashed.speaker.integration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.speaker.SoundCatalog;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SpeakerPortProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "speaker_port");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (Accessor.getBlockEntity() instanceof SpeakerBlockEntity Speaker) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.port", String.format("%04d", Speaker.getPort())));
            SoundMessages.Request Loop = Speaker.getLooping();
            if (Loop != null) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.speaker.looping", nameOf(Loop.Id())));
            }
        }
    }

    private static Component nameOf(ResourceLocation Id) {
        SoundCatalog.Entry Entry = SoundCatalog.find(Id);
        if (Entry != null && !Entry.isAnomalous()) {
            return Component.translatable(Entry.getTranslationKey());
        }
        for (CassetteRecording Recording : AnomalousLibrary.client()) {
            if (Recording.Sound().equals(Id)) {
                return Recording.Title();
            }
        }
        return Component.literal(Id.toString());
    }
}

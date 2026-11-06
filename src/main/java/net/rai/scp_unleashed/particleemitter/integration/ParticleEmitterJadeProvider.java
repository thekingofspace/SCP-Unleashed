package net.rai.scp_unleashed.particleemitter.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlock;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ParticleEmitterJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "particle_emitter");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof ParticleEmitterBlock)) {
            return;
        }
        if (Accessor.getBlockEntity() instanceof ParticleEmitterBlockEntity Emitter) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.particle_emitter.mode",
                    Component.translatable(Emitter.getMode().translationKey()).withStyle(ChatFormatting.AQUA)));
        }
        Tooltip.add(State.getValue(ParticleEmitterBlock.Powered)
                ? Component.translatable("jade.scp_unleashed.particle_emitter.active").withStyle(ChatFormatting.GREEN)
                : Component.translatable("jade.scp_unleashed.particle_emitter.idle").withStyle(ChatFormatting.GRAY));
    }
}

package net.rai.scp_unleashed.scp1730.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp1730DefenceJadeProvider implements IBlockComponentProvider {
    Turret("scp1730_turret"),
    Laser("scp1730_laser_emitter");

    public static final String Prefix = "jade.scp_unleashed.scp1730_defence.";
    private final ResourceLocation Uid;

    Scp1730DefenceJadeProvider(String Name) {
        this.Uid = Scp1730Registry.location(Name);
    }

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        boolean Disabled;
        boolean Live;
        float Health;
        float Max;
        if (Accessor.getBlockEntity() instanceof Scp1730TurretBlockEntity Gun) {
            Disabled = Gun.wrecked();
            Live = Gun.live();
            Health = Gun.health();
            Max = Scp1730TurretBlockEntity.MaxHealth;
        } else if (Accessor.getBlockEntity() instanceof Scp1730LaserBlockEntity Beam) {
            Disabled = State.getValue(Scp1730LaserBlock.Broken);
            Live = State.getValue(Scp1730LaserBlock.Powered);
            Health = Beam.health();
            Max = Scp1730LaserBlockEntity.MaxHealth;
        } else {
            return;
        }
        if (Disabled) {
            Tooltip.add(Component.translatable(Prefix + "disabled").withStyle(ChatFormatting.RED));
            return;
        }
        if (Health < Max) {
            int Percent = Math.max(1, Math.round((1.0f - Health / Max) * 100.0f));
            Tooltip.add(Component.translatable(Prefix + "damaged", Percent).withStyle(ChatFormatting.GOLD));
        }
        Tooltip.add(Component.translatable(Prefix + (Live ? "active" : "unpowered")).withStyle(Live ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }
}

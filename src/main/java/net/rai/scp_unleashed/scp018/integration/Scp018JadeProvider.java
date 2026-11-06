package net.rai.scp_unleashed.scp018.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp018.BallTuning;
import net.rai.scp_unleashed.scp018.Scp018Ball;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum Scp018JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp018_ball");
    private static final String Prefix = "jade.scp_unleashed.scp018_ball";
    public static final double CriticalDanger = 0.75;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp018Ball Ball)) {
            return;
        }
        double Speed = Ball.speedNow();
        String Shown = String.format(Locale.ROOT, "%.1f", Speed * BallTuning.TicksPerSecond);
        double Danger = BallTuning.danger(Speed);
        ChatFormatting Colour = Speed < BallTuning.DamageSpeed ? ChatFormatting.GREEN : Danger < CriticalDanger ? ChatFormatting.GOLD : ChatFormatting.RED;
        Tooltip.add(Component.translatable(Prefix + ".speed", Shown).withStyle(Colour));
        String State = Speed < BallTuning.DamageSpeed ? ".catchable" : Danger < CriticalDanger ? ".dangerous" : ".critical";
        Tooltip.add(Component.translatable(Prefix + State).withStyle(ChatFormatting.GRAY));
    }
}

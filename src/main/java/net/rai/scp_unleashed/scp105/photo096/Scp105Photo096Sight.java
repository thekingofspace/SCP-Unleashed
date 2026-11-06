package net.rai.scp_unleashed.scp105.photo096;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp096.Scp096Persons;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;

import java.util.Optional;

public final class Scp105Photo096Sight {
    public static final double Reach = Scp096ShyGuy.LoseRange;
    public static final int DreadTicks = 160;
    public static final String DreadKey = "message.scp_unleashed.scp105_photograph_096.dread";

    private Scp105Photo096Sight() {
    }

    public static Optional<Scp096ShyGuy> nearest(ServerLevel World, Vec3 From) {
        Scp096ShyGuy Best = null;
        double BestGap = Reach * Reach;
        for (Scp096ShyGuy Guy : World.getEntities(Scp096Registry.ShyGuy.get(), Found -> Found.isAlive() && !Found.isRemoved())) {
            double Gap = Guy.distanceToSqr(From);
            if (Gap <= BestGap) {
                Best = Guy;
                BestGap = Gap;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static Optional<Scp096ShyGuy> view(ServerPlayer Viewer) {
        ServerLevel World = Viewer.serverLevel();
        World.playSound(Viewer, Viewer.getX(), Viewer.getEyeY(), Viewer.getZ(), Scp105Photo096Registry.Rattle.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
        Optional<Scp096ShyGuy> Found = Scp096Persons.isWitness(Viewer) ? nearest(World, Viewer.position()) : Optional.empty();
        if (Found.isPresent()) {
            Found.get().forceRage(World, Viewer);
            return Found;
        }
        dread(Viewer);
        return Optional.empty();
    }

    private static void dread(ServerPlayer Viewer) {
        Viewer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DreadTicks, 0, false, false, false));
        Viewer.displayClientMessage(Component.translatable(DreadKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
    }
}

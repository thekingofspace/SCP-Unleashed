package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Stage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Scp024ClientState {
    public static final int StreamersPerCannon = 4;
    public static final int ConfettiPerCannon = 3;
    public static final int SmallCannons = 2;
    public static final int SmallStreamers = 4;
    public static final double CeilingGap = 5.0;
    private static final RandomSource Dice = RandomSource.create();
    private static final Set<UUID> Bars = new LinkedHashSet<>();
    private static final List<BlockPos> Cannons = new ArrayList<>();
    @Nullable
    private static BlockPos Focus;

    private Scp024ClientState() {
    }

    public static boolean inStage(Minecraft Client) {
        return Client.level != null && Scp024Stage.inStage(Client.level);
    }

    public static void track(UUID Bar) {
        Bars.add(Bar);
    }

    public static boolean lingering(Minecraft Client) {
        return !Bars.isEmpty() || !Cannons.isEmpty() || Scp024Music.active() || Scp024Crowd.active() || Scp024QuipOverlay.active()
                || Client.screen instanceof Scp024DialogueScreen;
    }

    public static void clear(Minecraft Client, Collection<UUID> Extra) {
        Set<UUID> Gone = new LinkedHashSet<>(Bars);
        Gone.addAll(Extra);
        for (UUID Bar : Gone) {
            Client.gui.getBossOverlay().update(ClientboundBossEventPacket.createRemovePacket(Bar));
        }
        Bars.clear();
        Cannons.clear();
        Focus = null;
        Scp024QuipOverlay.reset();
        Scp024Crowd.reset();
        Scp024Music.stop(Client);
        if (Client.screen instanceof Scp024DialogueScreen Dialogue) {
            Dialogue.dismiss();
        }
    }

    public static void reset() {
        Bars.clear();
        Cannons.clear();
        Focus = null;
        Scp024QuipOverlay.reset();
        Scp024Crowd.reset();
    }

    public static void tick(Minecraft Client) {
        if (Client.player == null || Client.level == null) {
            return;
        }
        if (!inStage(Client) && lingering(Client)) {
            clear(Client, List.of());
        }
    }

    public static void burst(Minecraft Client, List<BlockPos> From, BlockPos Target, int Strength) {
        if (!inStage(Client)) {
            return;
        }
        Cannons.clear();
        Cannons.addAll(From);
        Focus = Target;
        fire(Client, Cannons, Strength * StreamersPerCannon, Strength * ConfettiPerCannon);
    }

    public static void smallBurst(Minecraft Client) {
        if (!inStage(Client) || Cannons.isEmpty()) {
            return;
        }
        List<BlockPos> Pair = Cannons.subList(0, Math.min(SmallCannons, Cannons.size()));
        fire(Client, Pair, SmallStreamers, 2);
        ClientLevel World = Client.level;
        BlockPos First = Pair.get(0);
        World.playLocalSound(First.getX() + 0.5, First.getY() + 0.5, First.getZ() + 0.5, Scp024Registry.ConfettiPop.get(), SoundSource.AMBIENT, 0.5f,
                1.1f + Dice.nextFloat() * 0.2f, false);
    }

    private static void fire(Minecraft Client, List<BlockPos> From, int Streamers, int Confetti) {
        ClientLevel World = Client.level;
        if (World == null || Focus == null) {
            return;
        }
        Vec3 Aim = Vec3.atBottomCenterOf(Focus);
        for (BlockPos Cannon : From) {
            Vec3 Muzzle = new Vec3(Cannon.getX() + 0.5, Cannon.getY() + 0.3, Cannon.getZ() + 0.5);
            boolean Ceiling = Cannon.getY() > Focus.getY() + CeilingGap;
            Vec3 Toward = new Vec3(Aim.x - Muzzle.x, 0.0, Aim.z - Muzzle.z);
            Toward = Toward.lengthSqr() < 1.0E-4 ? Vec3.ZERO : Toward.normalize();
            for (int Index = 0; Index < Streamers + Confetti; Index++) {
                double Dx;
                double Dy;
                double Dz;
                if (Ceiling) {
                    Dx = (Dice.nextDouble() - 0.5) * 0.3;
                    Dy = -0.02 - Dice.nextDouble() * 0.05;
                    Dz = (Dice.nextDouble() - 0.5) * 0.3;
                } else {
                    double Push = 0.1 + Dice.nextDouble() * 0.16;
                    Dx = Toward.x * Push + (Dice.nextDouble() - 0.5) * 0.18;
                    Dy = 0.5 + Dice.nextDouble() * 0.25;
                    Dz = Toward.z * Push + (Dice.nextDouble() - 0.5) * 0.18;
                }
                World.addParticle(Index < Streamers ? Scp024Registry.Streamer.get() : Scp024Registry.Confetti.get(), Muzzle.x, Muzzle.y, Muzzle.z, Dx, Dy, Dz);
            }
        }
    }
}

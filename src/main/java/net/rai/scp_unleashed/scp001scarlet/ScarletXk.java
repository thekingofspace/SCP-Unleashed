package net.rai.scp_unleashed.scp001scarlet;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.worldend.WorldEndState;
import net.rai.scp_unleashed.worldend.WorldEnding;
import org.slf4j.Logger;

public final class ScarletXk {
    public static final int ClearTicks = 1200;
    public static final int AmbientMin = 900;
    public static final int AmbientSpread = 900;
    private static long NextAmbient;
    private static final Logger Log = LogUtils.getLogger();

    private ScarletXk() {
    }

    public static boolean paused(ScarletState.Phase Phase) {
        return Phase == ScarletState.Phase.Rite || Phase == ScarletState.Phase.Fight;
    }

    public static void begin(MinecraftServer Server) {
        ScarletState State = ScarletState.get(Server);
        ScarletWorld Data = ScarletWorld.get(Server);
        if (State.phase() == ScarletState.Phase.Xk2 || paused(State.phase()) || State.phase() == ScarletState.Phase.Freed) {
            return;
        }
        State.manifestation(1.0f);
        State.phase(ScarletState.Phase.Xk2, Server.overworld().getGameTime());
        Data.clock(0L);
        Data.forgetDayTime();
        Data.endingBegun(false);
        Data.stopClearing();
        Log.warn("SCP-001 (The Scarlet King): XK-2 begun");
        if (!Data.announced()) {
            Data.announced(true);
            for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
                User.sendSystemMessage(Component.translatable(ScarletMeter.Prefix + "xk2.head").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                User.sendSystemMessage(Component.translatable(ScarletMeter.Prefix + "xk2.body").withStyle(ChatFormatting.RED));
                User.playNotifySound(Scp001ScarletRegistry.Toll.get(), SoundSource.AMBIENT, 1.0f, 0.6f);
                User.playNotifySound(Scp001ScarletRegistry.TollThunder.get(), SoundSource.AMBIENT, 0.8f, 0.5f);
            }
        }
        ScarletMeter.syncAll(Server);
    }

    private static void ambience(MinecraftServer Server, ScarletWorld Data) {
        long Now = Server.overworld().getGameTime();
        if (Now < NextAmbient) {
            return;
        }
        NextAmbient = Now + AmbientMin + Server.overworld().random.nextInt(AmbientSpread);
        for (ServerPlayer User : Server.overworld().players()) {
            User.playNotifySound(Scp001ScarletRegistry.Xk2Ambient.get(), SoundSource.AMBIENT, 0.35f + 0.45f * Data.sky(), 1.0f);
        }
    }

    public static boolean ours(MinecraftServer Server) {
        return Scp001ScarletRegistry.Source.toString().equals(WorldEndState.get(Server).source());
    }

    public static void tick(MinecraftServer Server) {
        ScarletState State = ScarletState.get(Server);
        ScarletWorld Data = ScarletWorld.get(Server);
        ScarletState.Phase Phase = State.phase();
        switch (Phase) {
            case Xk2 -> {
                if (Data.clearing()) {
                    Data.stopClearing();
                }
                ambience(Server, Data);
                long Delta = Data.advance(Server.overworld().getDayTime());
                Data.clock(Data.clock() + Delta);
                long Total = ScarletConfig.xk2Ticks();
                Data.sky((float) Math.min(1.0, 0.15 + 0.85 * Data.clock() / (double) Total));
                if (Data.clock() >= Total && !Data.endingBegun()) {
                    Data.endingBegun(true);
                    BlockPos Dais = State.thronePlaced() ? State.throne() : Server.overworld().getSharedSpawnPos();
                    WorldEnding.begin(Server, Component.translatable(ScarletMeter.Prefix + "ending"), Scp001ScarletRegistry.Source,
                            Vec3.atCenterOf(Dais).add(0.0, 24.0, 0.0));
                }
            }
            case Rite, Fight -> {
                Data.forgetDayTime();
                if (WorldEnding.running(Server) && ours(Server)) {
                    WorldEnding.cancel(Server);
                }
            }
            case Freed -> {
                Data.forgetDayTime();
                if (WorldEnding.running(Server) && ours(Server)) {
                    WorldEnding.cancel(Server);
                }
                if (Data.sky() > 0.0f && !Data.clearing()) {
                    Data.beginClearing();
                }
            }
            default -> {
                if (Data.sky() > 0.0f && !Data.clearing()) {
                    Data.beginClearing();
                }
            }
        }
        if (Data.clearing()) {
            int Elapsed = Data.clearTick();
            Data.sky(Data.clearFrom() * (1.0f - Math.min(1.0f, Elapsed / (float) ClearTicks)));
            if (Elapsed >= ClearTicks) {
                Data.sky(0.0f);
                Data.stopClearing();
            }
        }
    }
}

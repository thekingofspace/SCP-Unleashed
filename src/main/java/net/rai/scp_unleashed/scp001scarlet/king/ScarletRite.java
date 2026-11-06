package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.discord.DiscordApocalypse;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletCourtHook;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletConfirmPayload;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletRiteFxPayload;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneRise;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Registry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ScarletRite {
    public static final double BindRadius = 6.0;
    public static final int ConfirmTicks = 1200;
    public static final int SplitStart = 80;
    public static final int KingAt = 150;
    public static final int DissolveAt = 230;
    public static final int FightAt = 270;
    public static final int XkClock = 72000;
    public static final int MinimumLeft = 24000;
    private static final Map<UUID, Pending> Waiting = new HashMap<>();

    public enum Outcome { Silent, Stirring, Elsewhere, Unbound, Unsuitable, Ready }

    public record Pending(BlockPos Throne, UUID Blob, long Expires, int Nonce, boolean Build) {
    }

    private ScarletRite() {
    }

    public static Outcome check(ServerPlayer Speaker) {
        ScarletState State = ScarletState.get(Speaker.server);
        boolean Home = Speaker.level() == Speaker.server.overworld();
        if (!Home && State.phase() == ScarletState.Phase.Xk2) {
            return Outcome.Elsewhere;
        }
        return outcome(Speaker.serverLevel(), Speaker.position(), State.phase(), State.thronePlaced(), State.throne());
    }

    public static Outcome outcome(ServerLevel World, Vec3 Where, ScarletState.Phase Phase, boolean Placed, BlockPos Throne) {
        if (Phase == ScarletState.Phase.Asleep || Phase == ScarletState.Phase.Freed || Phase == ScarletState.Phase.Rite || Phase == ScarletState.Phase.Fight) {
            return Outcome.Silent;
        }
        if (Phase == ScarletState.Phase.Stirring) {
            return Outcome.Stirring;
        }
        if (Placed) {
            int Radius = ScarletKingConfig.riteRadius();
            if (Where.distanceToSqr(Vec3.atBottomCenterOf(Throne)) > Radius * Radius) {
                return Outcome.Elsewhere;
            }
            return offering(World, Where) == null ? Outcome.Unbound : Outcome.Ready;
        }
        Scp999Blob Blob = offering(World, Where);
        if (Blob == null) {
            return Outcome.Unbound;
        }
        return ScarletThroneRise.survey(World, Blob.position()).fine() ? Outcome.Ready : Outcome.Unsuitable;
    }

    @Nullable
    public static Scp999Blob offering(ServerLevel World, Vec3 Near) {
        List<Scp999Blob> Found = World.getEntitiesOfClass(Scp999Blob.class, new AABB(Near, Near).inflate(BindRadius), Entity::isAlive);
        Scp999Blob Best = null;
        double Closest = BindRadius * BindRadius;
        for (Scp999Blob Each : Found) {
            double Distance = Each.position().distanceToSqr(Near);
            if (Distance <= Closest) {
                Closest = Distance;
                Best = Each;
            }
        }
        return Best;
    }

    public static void heard(ServerPlayer Speaker, String Raw) {
        if (!ScarletPhrase.matches(Raw)) {
            return;
        }
        Outcome Result = check(Speaker);
        ServerLevel World = Speaker.serverLevel();
        switch (Result) {
            case Stirring -> World.playSound(null, Speaker.getX(), Speaker.getY(), Speaker.getZ(), ScarletKingRegistry.Whisper.get(), SoundSource.AMBIENT, 0.6f, 0.6f);
            case Elsewhere -> {
                World.playSound(null, Speaker.getX(), Speaker.getY(), Speaker.getZ(), ScarletKingRegistry.Whisper.get(), SoundSource.AMBIENT, 0.8f, 0.5f);
                boolean Raised = ScarletState.get(Speaker.server).thronePlaced() && Speaker.level() == Speaker.server.overworld();
                Speaker.displayClientMessage(Component.translatable("message.scp_unleashed.scarlet_king.elsewhere" + (Raised ? ".raised" : ""))
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
            case Unbound -> {
                World.playSound(null, Speaker.getX(), Speaker.getY(), Speaker.getZ(), ScarletKingRegistry.ChoirLow.get(), SoundSource.AMBIENT, 1.0f, 0.5f);
                Speaker.displayClientMessage(Component.translatable("message.scp_unleashed.scarlet_king.unbound").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
            case Unsuitable -> refuse(Speaker);
            case Ready -> ask(Speaker);
            default -> {
            }
        }
    }

    private static void refuse(ServerPlayer Speaker) {
        ServerLevel World = Speaker.serverLevel();
        Scp999Blob Blob = offering(World, Speaker.position());
        ScarletThroneRise.Refusal Reason = Blob == null ? ScarletThroneRise.Refusal.Footing : ScarletThroneRise.survey(World, Blob.position()).Reason();
        World.playSound(null, Speaker.getX(), Speaker.getY(), Speaker.getZ(), ScarletKingRegistry.Whisper.get(), SoundSource.AMBIENT, 0.9f, 0.55f);
        Speaker.displayClientMessage(Component.translatable("message.scp_unleashed.scarlet_king.unsuitable." + Reason.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
    }

    private static void ask(ServerPlayer Speaker) {
        ScarletState State = ScarletState.get(Speaker.server);
        Scp999Blob Blob = offering(Speaker.serverLevel(), Speaker.position());
        if (Blob == null) {
            return;
        }
        boolean Build = !State.thronePlaced();
        int Nonce = Speaker.getRandom().nextInt(Integer.MAX_VALUE);
        Waiting.put(Speaker.getUUID(), new Pending(State.throne(), Blob.getUUID(), Speaker.serverLevel().getGameTime() + ConfirmTicks, Nonce, Build));
        Speaker.serverLevel().playSound(null, Speaker.getX(), Speaker.getY(), Speaker.getZ(), ScarletKingRegistry.ChoirBell.get(), SoundSource.AMBIENT, 1.0f, 0.45f);
        PacketDistributor.sendToPlayer(Speaker, new ScarletConfirmPayload(Nonce, Build));
    }

    public static int pendingNonce(UUID Speaker) {
        Pending Asked = Waiting.get(Speaker);
        return Asked == null ? -1 : Asked.Nonce();
    }

    public static void answer(ServerPlayer Speaker, int Nonce, boolean Yes) {
        Pending Asked = Waiting.remove(Speaker.getUUID());
        if (Asked == null || Asked.Nonce() != Nonce) {
            return;
        }
        ServerLevel World = Speaker.serverLevel();
        if (!Yes) {
            Speaker.displayClientMessage(Component.translatable("message.scp_unleashed.scarlet_king.declined").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        if (World.getGameTime() > Asked.Expires() || check(Speaker) != Outcome.Ready) {
            return;
        }
        Entity Found = World.getEntity(Asked.Blob());
        Scp999Blob Blob = Found instanceof Scp999Blob Each && Each.isAlive() && Each.distanceToSqr(Speaker) < 64.0 ? Each : offering(World, Speaker.position());
        if (Blob == null) {
            return;
        }
        ScarletState State = ScarletState.get(World.getServer());
        if (Asked.Build() && !State.thronePlaced()) {
            ScarletThroneRise.Survey Site = ScarletThroneRise.survey(World, Blob.position());
            if (!Site.fine()) {
                refuse(Speaker);
                return;
            }
            ScarletThroneRise.start(World, Site.Throne(), Blob, Speaker, State.phaseTick());
            return;
        }
        ScarletThrone.refit(World, State.throne());
        begin(World, State.throne(), Blob, Speaker);
    }

    public static void begin(ServerLevel World, BlockPos Throne, Scp999Blob Blob) {
        begin(World, Throne, Blob, null);
    }

    public static void begin(ServerLevel World, BlockPos Throne, Scp999Blob Blob, @Nullable Player Leader) {
        begin(World, Throne, Blob, Leader, ScarletState.get(World.getServer()).phaseTick());
    }

    public static void begin(ServerLevel World, BlockPos Throne, Scp999Blob Blob, @Nullable Player Leader, long Previous) {
        ScarletState State = ScarletState.get(World.getServer());
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        long Now = World.getGameTime();
        State.phase(ScarletState.Phase.Rite, Now);
        Data.startRite(Now, Throne, Blob.getUUID(), Previous);
        Blob.setNoAi(true);
        Blob.setNoGravity(true);
        Blob.setInvulnerable(true);
        Blob.setGlowingTag(true);
        ScarletArena Where = ScarletArena.of(World, Throne);
        ScarletRiteFxPayload Fx = new ScarletRiteFxPayload(Throne, Where.Seats(), Now);
        for (ServerPlayer Listener : World.players()) {
            if (Listener.blockPosition().distSqr(Throne) < 256 * 256) {
                PacketDistributor.sendToPlayer(Listener, Fx);
            }
        }
        World.playSound(null, Throne, ScarletKingRegistry.ChoirLow.get(), SoundSource.HOSTILE, 8.0f, 0.5f);
        World.playSound(null, Throne, ScarletKingRegistry.Rumble.get(), SoundSource.HOSTILE, 8.0f, 0.5f);
        DiscordApocalypse.scarletRite(World.getServer(), Leader, Throne);
    }

    public static void tick(ServerLevel World) {
        if (World != World.getServer().overworld()) {
            return;
        }
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        ScarletState State = ScarletState.get(World.getServer());
        if (ScarletThroneRise.active(World.getServer())) {
            Waiting.values().removeIf(Each -> Each.Expires() < World.getGameTime());
            return;
        }
        if (Data.riteStart() >= 0L) {
            if (State.phase() != ScarletState.Phase.Rite) {
                Data.endRite();
                return;
            }
            sequence(World, Data, World.getGameTime() - Data.riteStart());
        } else if (State.phase() == ScarletState.Phase.Rite) {
            ScarletKing King = king(World, Data);
            if (King != null) {
                start(World, Data, King);
            } else {
                reset(World, State.throne());
            }
        } else if (State.phase() == ScarletState.Phase.Fight && World.getGameTime() % 10L == 0L) {
            watch(World, Data, State);
        }
        Waiting.values().removeIf(Each -> Each.Expires() < World.getGameTime());
    }

    private static void sequence(ServerLevel World, ScarletKingData Data, long Elapsed) {
        BlockPos Throne = Data.riteThrone();
        Vec3 Centre = Vec3.atBottomCenterOf(ScarletArena.dais(World, Throne));
        if (Elapsed >= SplitStart && Elapsed < KingAt && Elapsed % 6L == 0L) {
            for (int Crack = 0; Crack < 7; Crack++) {
                double Angle = Crack * Math.PI * 2.0 / 7.0 + Elapsed * 0.01;
                double Reach = 2.0 + (Elapsed - SplitStart) * 0.12;
                Vec3 At = ScarletKing.ground(World, Centre.add(Math.sin(Angle) * Reach, 0.0, -Math.cos(Angle) * Reach));
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, World.getBlockState(BlockPos.containing(At).below())), At.x, At.y + 0.1, At.z, 12, 0.6, 0.1,
                        0.6, 0.1);
                World.sendParticles(ParticleTypes.LAVA, At.x, At.y + 0.2, At.z, 1, 0.3, 0.0, 0.3, 0.0);
            }
            if (Elapsed % 24L == 0L) {
                World.playSound(null, Throne, ScarletKingRegistry.Rumble.get(), SoundSource.HOSTILE, 6.0f, 0.4f + World.random.nextFloat() * 0.2f);
            }
        }
        Entity Found = Data.blob() == null ? null : World.getEntity(Data.blob());
        if (Elapsed == KingAt) {
            for (ScarletKing Stale : World.getEntitiesOfClass(ScarletKing.class, new AABB(Throne).inflate(ScarletArena.FightRadius + 32.0, 64.0, ScarletArena.FightRadius + 32.0))) {
                Stale.clearBar();
                Stale.discard();
            }
            ScarletKing King = ScarletKingRegistry.KingType.get().create(World);
            if (King != null) {
                Vec3 At = ScarletKing.ground(World, Centre.add(0.0, 2.0, 0.0));
                King.moveTo(At.x, At.y, At.z, 180.0f, 0.0f);
                King.setYBodyRot(180.0f);
                King.setYHeadRot(180.0f);
                King.generation(Data.generation());
                King.place(ScarletArena.of(World, Throne));
                World.addFreshEntity(King);
                Data.king(King.getUUID());
                World.playSound(null, Throne, ScarletKingRegistry.Roar.get(), SoundSource.HOSTILE, 10.0f, 0.5f);
                World.playSound(null, Throne, ScarletKingRegistry.Growl.get(), SoundSource.HOSTILE, 10.0f, 0.5f);
            }
        }
        if (Found != null && Elapsed > KingAt && Elapsed < DissolveAt) {
            Vec3 Goal = Centre.add(0.0, 12.0, -3.0);
            Vec3 Next = Found.position().lerp(Goal, 0.04);
            Found.setPos(Next.x, Next.y, Next.z);
            World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.62f, 0.45f), 1.5f), Next.x, Next.y + 0.3, Next.z, 4, 0.3, 0.3, 0.3, 0.01);
        }
        if (Elapsed == DissolveAt) {
            if (Found != null) {
                Vec3 At = Found.position();
                World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.65f, 0.5f), 2.5f), At.x, At.y, At.z, 80, 1.0, 1.0, 1.0, 0.2);
                World.sendParticles(ParticleTypes.END_ROD, At.x, At.y, At.z, 40, 0.8, 0.8, 0.8, 0.15);
                Found.discard();
            }
            Data.consumed(true);
            World.playSound(null, Throne, ScarletKingRegistry.Bind.get(), SoundSource.HOSTILE, 8.0f, 0.7f);
            World.playSound(null, Throne, ScarletKingRegistry.BindChime.get(), SoundSource.HOSTILE, 6.0f, 0.5f);
            ScarletKing King = king(World, Data);
            if (King != null) {
                King.beginBinding();
            }
        }
        if (Elapsed > DissolveAt && Elapsed < FightAt) {
            ScarletKing King = king(World, Data);
            if (King != null && Elapsed % 2L == 0L) {
                Vec3 From = Centre.add(0.0, 12.0, -3.0);
                for (int Band = 0; Band < ScarletArena.Count; Band++) {
                    Vec3 To = King.bindPoint(Band, 0.0f);
                    Vec3 At = From.lerp(To, (Elapsed - DissolveAt) / (double) (FightAt - DissolveAt));
                    World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.4f), 1.6f), At.x, At.y, At.z, 2, 0.15, 0.15, 0.15, 0.0);
                }
            }
        }
        if (Elapsed >= FightAt) {
            ScarletKing King = king(World, Data);
            if (King == null) {
                reset(World, Throne);
                return;
            }
            start(World, Data, King);
        }
    }

    private static void start(ServerLevel World, ScarletKingData Data, ScarletKing King) {
        Data.endRite();
        King.settle(World);
        King.beginFight(players(World, King.arena() == null ? King.blockPosition() : King.arena().Throne()));
        ScarletState.get(World.getServer()).phase(ScarletState.Phase.Fight, World.getGameTime());
    }

    public static int players(ServerLevel World, BlockPos Throne) {
        double Reach = ScarletKingConfig.playerRadius();
        int Count = 0;
        for (ServerPlayer Each : World.players()) {
            if (Each.isAlive() && !Each.isSpectator() && Each.blockPosition().distSqr(Throne) < Reach * Reach) {
                Count++;
            }
        }
        return Math.max(1, Count);
    }

    @Nullable
    public static ScarletKing king(ServerLevel World, ScarletKingData Data) {
        if (Data.king() == null) {
            return null;
        }
        return World.getEntity(Data.king()) instanceof ScarletKing Found && Found.isAlive() ? Found : null;
    }

    private static void watch(ServerLevel World, ScarletKingData Data, ScarletState State) {
        BlockPos Throne = State.throne();
        double Reach = ScarletKingConfig.playerRadius();
        boolean Anyone = false;
        for (ServerPlayer Each : World.players()) {
            if (Each.isAlive() && !Each.isSpectator() && Each.blockPosition().distSqr(Throne) < Reach * Reach) {
                Anyone = true;
                break;
            }
        }
        long Now = World.getGameTime();
        if (!Anyone) {
            if (Data.emptySince() < 0L) {
                Data.emptySince(Now);
            } else if (Now - Data.emptySince() >= ScarletKingConfig.resetTicks()) {
                reset(World, Throne);
            }
            return;
        }
        Data.emptySince(-1L);
        if (king(World, Data) == null) {
            if (Data.missingSince() < 0L) {
                Data.missingSince(Now);
            } else if (Now - Data.missingSince() >= ScarletKingConfig.resetTicks()) {
                reset(World, Throne);
            }
        } else {
            Data.missingSince(-1L);
        }
    }

    public static void reset(ServerLevel World, BlockPos Throne) {
        ScarletState State = ScarletState.get(World.getServer());
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        ScarletKing King = king(World, Data);
        if (King != null) {
            Vec3 At = King.position();
            World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 10.0, At.z, 80, 4.0, 8.0, 4.0, 0.05);
            King.clearBar();
            King.discard();
        }
        Data.nextGeneration();
        Data.king(null);
        Data.endRite();
        Data.emptySince(-1L);
        Data.missingSince(-1L);
        ScarletKingFall.clearArena(World, Throne);
        ScarletThrone.restoreAnchors(World, Throne);
        ScarletCourtHook Hook = ScarletKing.court();
        if (Hook != null) {
            Hook.reset(World);
        }
        if (Data.consumed() || Data.blob() != null && !(World.getEntity(Data.blob()) instanceof Scp999Blob)) {
            Vec3 At = ScarletKing.ground(World, Vec3.atBottomCenterOf(ScarletArena.dais(World, Throne)).add(0.0, 2.0, 0.0));
            Scp999Blob Reformed = Scp999Registry.Blob.get().spawn(World, BlockPos.containing(At), MobSpawnType.EVENT);
            if (Reformed != null) {
                Reformed.setPersistenceRequired();
                World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.62f, 0.45f), 2.0f), At.x, At.y + 0.5, At.z, 60, 0.8, 0.8, 0.8, 0.1);
            }
        } else if (Data.blob() != null && World.getEntity(Data.blob()) instanceof Scp999Blob Waiting0) {
            Waiting0.setNoAi(false);
            Waiting0.setNoGravity(false);
            Waiting0.setInvulnerable(false);
            Waiting0.setGlowingTag(false);
        }
        Data.consumed(false);
        long Now = World.getGameTime();
        long Restored = Math.max(Data.xkTick(), Now - (XkClock - MinimumLeft));
        State.phase(ScarletState.Phase.Xk2, Math.min(Now, Restored));
        for (ServerPlayer Listener : World.players()) {
            if (Listener.blockPosition().distSqr(Throne) < 256 * 256) {
                Listener.sendSystemMessage(Component.translatable("message.scp_unleashed.scarlet_king.reset").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }
    }

    public static void forget() {
        Waiting.clear();
    }
}

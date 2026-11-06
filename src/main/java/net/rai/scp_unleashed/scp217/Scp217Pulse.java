package net.rai.scp_unleashed.scp217;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityItem;
import net.rai.scp_unleashed.scp217.network.Scp217PulsePayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp217Pulse {
    public static final int CooldownTicks = 900;
    public static final int GlowTicks = 200;
    public static final float WaveSpeed = 1.6f;
    public static final double[] Radii = {0.0, 0.0, 32.0, 48.0};
    public static final int MaxMarked = 256;
    public static final int BurstBits = 10;
    public static final float ReadyVolume = 0.35f;
    public static final float ReadyPitch = 1.3f;
    public static final String Prefix = "message.scp_unleashed.scp217.";

    private Scp217Pulse() {
    }

    public enum Outcome {
        Sent,
        Cooling,
        Unable
    }

    public record Result(Outcome Kind, List<Mob> Marked, int Remaining) {
        public boolean sent() {
            return Kind == Outcome.Sent;
        }
    }

    public static double radiusOf(int Stage) {
        return Radii[Mth.clamp(Stage, 1, Radii.length) - 1];
    }

    public static boolean canPulse(LivingEntity Subject) {
        return Subject.isAlive() && !Subject.isSpectator() && Scp217Clockwork.isPerfect(Subject) && Scp217Clockwork.stageOf(Subject) >= Scp217Clockwork.PulseStage;
    }

    public static int cooldown(LivingEntity Subject) {
        Long Ready = Subject.getExistingDataOrNull(Scp217Registry.PulseReady.get());
        return Ready == null ? 0 : (int) Math.max(0L, Ready - Subject.level().getGameTime());
    }

    public static Result request(ServerPlayer User) {
        if (!canPulse(User)) {
            return new Result(Outcome.Unable, List.of(), 0);
        }
        int Left = cooldown(User);
        if (Left > 0) {
            User.displayClientMessage(Component.translatable(Prefix + "pulse_cooling", (Left + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return new Result(Outcome.Cooling, List.of(), Left);
        }
        ServerLevel World = User.serverLevel();
        double Radius = radiusOf(Scp217Clockwork.stageOf(User));
        Vec3 Centre = User.position();
        List<Mob> Marked = locate(World, Centre, Radius, User);
        User.setData(Scp217Registry.PulseReady.get(), World.getGameTime() + CooldownTicks);
        List<Integer> Targets = new ArrayList<>();
        for (Mob Found : Marked) {
            Targets.add(Found.getId());
        }
        PacketDistributor.sendToPlayer(User, new Scp217PulsePayload(Centre.x, Centre.y, Centre.z, (float) Radius, Targets));
        double Chest = User.getY() + User.getBbHeight() * 0.6;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp217Registry.PulseLayers) {
            World.playSound(null, User.getX(), Chest, User.getZ(), Layer.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        World.sendParticles(Scp217Ambience.Brass, User.getX(), Chest, User.getZ(), BurstBits, 0.3, 0.3, 0.3, 0.12);
        User.displayClientMessage(Component.translatable(Prefix + "pulse_sent", Marked.size()).withStyle(ChatFormatting.GOLD), true);
        return new Result(Outcome.Sent, Marked, CooldownTicks);
    }

    public static List<Mob> locate(ServerLevel World, Vec3 Centre, double Radius, @Nullable Entity Except) {
        double Reach = Radius * Radius;
        List<Mob> Found = new ArrayList<>(World.getEntitiesOfClass(Mob.class, new AABB(Centre, Centre).inflate(Radius),
                Target -> Target != Except && Target.isAlive() && Target.distanceToSqr(Centre) <= Reach));
        Found.sort(Comparator.comparingDouble(Target -> Target.distanceToSqr(Centre)));
        return List.copyOf(Found.size() > MaxMarked ? Found.subList(0, MaxMarked) : Found);
    }

    public static void tick(ServerLevel World, LivingEntity Subject) {
        if (!(Subject instanceof ServerPlayer User)) {
            return;
        }
        Long Ready = User.getExistingDataOrNull(Scp217Registry.PulseReady.get());
        if (Ready == null || World.getGameTime() < Ready) {
            return;
        }
        User.removeData(Scp217Registry.PulseReady.get());
        if (canPulse(User)) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp217Registry.ClickLayers) {
                User.playNotifySound(Layer.get(), SoundSource.PLAYERS, ReadyVolume, ReadyPitch);
            }
        }
    }

    public static void awaken(LivingEntity Subject) {
        if (Subject instanceof ServerPlayer User) {
            User.displayClientMessage(Component.translatable(Prefix + "pulse_awake", Component.keybind(ToolAbilityItem.AbilityKey)).withStyle(ChatFormatting.GOLD), false);
        }
    }
}

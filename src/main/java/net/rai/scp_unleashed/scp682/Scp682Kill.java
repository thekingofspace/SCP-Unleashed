package net.rai.scp_unleashed.scp682;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.health.LethalDamage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class Scp682Kill {
    public static final int TeleportAt = Scp682.CommandVerdict;
    public static final int StrikeAt = Scp682.CommandVerdict + 4;

    private Scp682Kill() {
    }

    public static boolean involves(Collection<? extends Entity> Targets) {
        for (Entity Each : Targets) {
            if (Each instanceof Scp682) {
                return true;
            }
        }
        return false;
    }

    public static int handle(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        ServerPlayer Issuer = Source.source instanceof ServerPlayer User ? User : null;
        List<Entity> Others = new ArrayList<>();
        for (Entity Each : Targets) {
            if (Each instanceof Scp682 Reptile) {
                Reptile.answerKill(Issuer);
            } else {
                Others.add(Each);
            }
        }
        for (Entity Each : Others) {
            Each.kill();
        }
        if (Others.size() == 1) {
            Component Name = Others.get(0).getDisplayName();
            Source.sendSuccess(() -> Component.translatable("commands.kill.success.single", Name), true);
        } else if (Others.size() > 1) {
            int Count = Others.size();
            Source.sendSuccess(() -> Component.translatable("commands.kill.success.multiple", Count), true);
        }
        return Others.size();
    }

    public static void tick(ServerLevel World, Scp682 Reptile) {
        int Age = Reptile.commandAge() + 1;
        Reptile.setCommandAge(Age);
        Reptile.getNavigation().stop();
        Vec3 Back = back(Reptile);
        if (Age <= Scp682.CommandRise && Age % 4 == 1) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COMMAND_BLOCK.defaultBlockState()), Back.x, Back.y, Back.z, 8, 0.25, 0.1, 0.25, 0.08);
        }
        if (Age == Scp682.CommandRise) {
            World.sendParticles(ParticleTypes.ENCHANT, Back.x, Back.y + 0.6, Back.z, 30, 0.3, 0.3, 0.3, 0.6);
        }
        UUID Victim = Reptile.commandVictim();
        if (Victim != null && Age >= TeleportAt && Age < Scp682.CommandSinkStart) {
            ServerPlayer User = World.getServer().getPlayerList().getPlayer(Victim);
            if (User == null || !User.isAlive()) {
                Reptile.setCommandAge(Math.max(Age, Scp682.CommandSinkStart - 1));
            } else if (Age == TeleportAt && !User.isCreative() && !User.isSpectator()) {
                summon(World, Reptile, User);
            } else if ((Age == TeleportAt && (User.isCreative() || User.isSpectator())) || Age >= StrikeAt && !User.isChangingDimension()) {
                strike(World, Reptile, User);
            }
        }
        if (Age == Scp682.CommandSinkStart) {
            Reptile.endCommandShow();
            Scp682Sounds.play(World, Back, List.of(Scp682Registry.CommandSink), 1.2f, 0.9f);
        }
        if (Age >= Scp682.CommandEnd) {
            Reptile.finishCommand();
        }
    }

    public static Vec3 back(Scp682 Reptile) {
        return Reptile.position().add(0.0, Reptile.getBbHeight() * 1.05, 0.0);
    }

    public static Vec3 jaws(Scp682 Reptile) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, Reptile.getYRot());
        return Reptile.position().add(Facing.scale(Reptile.reach() - 0.4));
    }

    private static void summon(ServerLevel World, Scp682 Reptile, ServerPlayer User) {
        Vec3 Spot = jaws(Reptile);
        float Yaw = Mth.wrapDegrees(Reptile.getYRot() + 180.0f);
        World.sendParticles(ParticleTypes.REVERSE_PORTAL, User.getX(), User.getY() + 1.0, User.getZ(), 40, 0.3, 0.6, 0.3, 0.05);
        User.teleportTo(World, Spot.x, Reptile.getY(), Spot.z, Yaw, 10.0f);
        User.setDeltaMovement(Vec3.ZERO);
        User.hurtMarked = true;
        World.sendParticles(ParticleTypes.REVERSE_PORTAL, Spot.x, Reptile.getY() + 1.0, Spot.z, 40, 0.3, 0.6, 0.3, 0.05);
    }

    private static void strike(ServerLevel World, Scp682 Reptile, ServerPlayer User) {
        DamageSource Verdict = World.damageSources().source(Scp682Registry.CommandDamage, Reptile);
        if (!User.isCreative() && !User.isSpectator()) {
            Reptile.triggerAnim("action", "bite");
            Scp682Sounds.play(World, User.position(), Scp682Registry.BiteLayers, 1.4f, Reptile.voicePitch());
        } else {
            Scp682Sounds.play(World, User.position(), List.of(Scp682Registry.CommandClank), 1.2f, 0.7f);
        }
        User.hurt(Verdict, LethalDamage.against(User));
        Reptile.setCommandAge(Math.max(Reptile.commandAge(), Scp682.CommandSinkStart - 1));
    }
}

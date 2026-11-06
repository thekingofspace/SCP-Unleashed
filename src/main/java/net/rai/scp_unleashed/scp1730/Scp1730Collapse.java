package net.rai.scp_unleashed.scp1730;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp1730.block.Scp1730ThresherBlockEntity;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.network.Scp1730ShakePayload;

public final class Scp1730Collapse {
    public static final int RampTicks = 1200;
    public static final int ShakeEvery = 10;
    public static final int RumbleEvery = 60;
    public static final int AlarmEvery = 100;
    public static final int DebrisEvery = 14;
    public static final float DebrisDamage = 2.0f;
    public static final float DebrisChance = 0.25f;

    private Scp1730Collapse() {
    }

    public static boolean canPull(ServerLevel World, BlockPos Pos) {
        if (!Scp1730World.inside(World)) {
            return false;
        }
        Scp1730State State = Scp1730State.get(World.getServer());
        if (State.corePulled()) {
            return false;
        }
        return State.guardianDead() || World.getEntitiesOfClass(Scp1730Guardian.class, new net.minecraft.world.phys.AABB(Pos).inflate(64.0), Scp1730Guardian::isAlive).isEmpty()
                && Scp1730State.get(World.getServer()).populated(Scp1730State.DeepZone);
    }

    public static void pull(ServerLevel World, BlockPos Pos, Scp1730ThresherBlockEntity Thresher, ServerPlayer User) {
        Scp1730State State = Scp1730State.get(World.getServer());
        Thresher.setPulled(true);
        State.startCollapse(World.getGameTime());
        ItemStack Core = new ItemStack(Scp1730Registry.Core.get());
        if (!User.getInventory().add(Core)) {
            User.drop(Core, false);
        }
        World.playSound(null, Pos, Scp1730Registry.ThresherPull.get(), SoundSource.BLOCKS, 3.0f, 0.7f);
        for (ServerPlayer Each : World.players()) {
            if (Scp1730World.inside(Each)) {
                Scp1730Destination.witness(Each, State.run());
                Each.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.unstable").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
                PacketDistributor.sendToPlayer(Each, new Scp1730ShakePayload(0.6f, 40));
            }
        }
    }

    public static void guardianFell(ServerLevel World, Scp1730Guardian Guardian) {
        if (!Scp1730World.inside(World)) {
            return;
        }
        Scp1730State.get(World.getServer()).setGuardianDead();
    }

    public static float intensity(long Elapsed) {
        return Mth.clamp(0.35f + Elapsed / (float) RampTicks * 0.65f, 0.35f, 1.0f);
    }

    public static void tick(ServerLevel World, Scp1730State State) {
        long Elapsed = World.getGameTime() - State.collapseStart();
        float Strength = intensity(Elapsed);
        for (ServerPlayer User : World.players()) {
            if (!Scp1730World.inside(User) || User.isSpectator()) {
                continue;
            }
            Scp1730Destination.witness(User, State.run());
            if (Elapsed % ShakeEvery == 0) {
                PacketDistributor.sendToPlayer(User, new Scp1730ShakePayload(Strength, ShakeEvery + 4));
            }
            if (Elapsed % RumbleEvery == 0) {
                World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp1730Registry.CollapseRumble.get(), SoundSource.AMBIENT, 1.2f + Strength, 0.6f + World.random.nextFloat() * 0.2f);
            }
            if (Elapsed % AlarmEvery == 0) {
                World.playSound(null, User.getX(), User.getY() + 3.0, User.getZ(), Scp1730Registry.Alarm.get(), SoundSource.AMBIENT, 1.0f, 0.8f);
            }
            if ((Elapsed + User.getId()) % DebrisEvery == 0) {
                debris(World, User, Strength);
            }
        }
    }

    private static void debris(ServerLevel World, ServerPlayer User, float Strength) {
        double X = User.getX() + (World.random.nextDouble() - 0.5) * 6.0;
        double Z = User.getZ() + (World.random.nextDouble() - 0.5) * 6.0;
        double Y = User.getY() + 3.2;
        World.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.GRAY_CONCRETE_POWDER.defaultBlockState()), X, Y, Z, 14, 1.2, 0.2, 1.2, 0.0);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), X, Y, Z, 6, 0.6, 0.1, 0.6, 0.1);
        World.playSound(null, X, Y, Z, Scp1730Registry.CollapseDebris.get(), SoundSource.AMBIENT, 0.8f, 0.8f + World.random.nextFloat() * 0.3f);
        if (World.random.nextFloat() < DebrisChance * Strength) {
            User.hurt(new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730Registry.DebrisDamage)), DebrisDamage);
        }
    }
}

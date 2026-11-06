package net.rai.scp_unleashed.sulfur.acid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyDrops;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.SulfurTags;
import org.joml.Vector3f;

import java.util.List;
import java.util.regex.Pattern;

public final class AcidContact {
    public static final float Damage = 2.0f;
    public static final int ArmourWear = 2;
    public static final int MeltTicks = 40;
    public static final int ContactGrace = 10;
    public static final int SmokeChance = 3;
    public static final double SurfaceSlack = 0.01;
    public static final String StartKey = "ScpAcidMeltStart";
    public static final String SeenKey = "ScpAcidMeltSeen";
    public static final DustParticleOptions Fizz = new DustParticleOptions(new Vector3f(0.80f, 0.86f, 0.22f), 0.9f);
    private static final Pattern AnomalyIds = Pattern.compile("^(scp\\d+[a-z]?|z\\d{4}|daevite)_");
    private static final EquipmentSlot[] ArmourSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private AcidContact() {
    }

    public static DamageSource source(Level World) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SulfurRegistry.AcidDamage));
    }

    public static boolean isAcidProof(ItemStack Stack) {
        if (Stack.isEmpty() || Stack.is(SulfurTags.AcidProof) || AnomalyDrops.isAnomalous(Stack)) {
            return true;
        }
        ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Stack.getItem());
        return Scp_unleashed.ModId.equals(Id.getNamespace()) && AnomalyIds.matcher(Id.getPath()).find();
    }

    public static boolean touches(Level World, BlockPos Pos, Entity Target) {
        FluidState Liquid = World.getFluidState(Pos);
        return !Liquid.isEmpty() && Liquid.is(SulfurTags.Acid) && Target.getBoundingBox().minY < Pos.getY() + Liquid.getHeight(World, Pos) + SurfaceSlack;
    }

    public static void contact(ServerLevel World, BlockPos Pos, Entity Target) {
        if (!Target.isAlive() || !touches(World, Pos, Target)) {
            return;
        }
        if (Target instanceof ItemEntity Dropped) {
            melt(World, Dropped);
        } else if (Target instanceof LivingEntity Living) {
            burn(World, Living);
        }
    }

    public static boolean burn(ServerLevel World, LivingEntity Living) {
        if (Living instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return false;
        }
        if (!Living.hurt(source(World), Damage)) {
            return false;
        }
        corrode(Living);
        double Spread = Living.getBbWidth() * 0.4;
        World.sendParticles(Fizz, Living.getX(), Living.getY() + 0.2, Living.getZ(), 6, Spread, 0.15, Spread, 0.0);
        World.sendParticles(ParticleTypes.SMOKE, Living.getX(), Living.getY() + 0.3, Living.getZ(), 2, Spread, 0.1, Spread, 0.01);
        play(World, Living.position(), SulfurRegistry.SizzleLayers, 0.45f, 0.95f + World.getRandom().nextFloat() * 0.2f);
        return true;
    }

    public static void corrode(LivingEntity Living) {
        for (EquipmentSlot Slot : ArmourSlots) {
            ItemStack Worn = Living.getItemBySlot(Slot);
            if (!Worn.isEmpty() && Worn.isDamageableItem() && !isAcidProof(Worn)) {
                Worn.hurtAndBreak(ArmourWear, Living, Slot);
            }
        }
    }

    public static boolean melt(ServerLevel World, ItemEntity Dropped) {
        if (isAcidProof(Dropped.getItem())) {
            return false;
        }
        CompoundTag Data = Dropped.getPersistentData();
        long Now = World.getGameTime();
        long Seen = Data.getLong(SeenKey);
        if (!Data.contains(StartKey) || Now - Seen > ContactGrace) {
            Data.putLong(StartKey, Now);
        }
        Data.putLong(SeenKey, Now);
        long Elapsed = Now - Data.getLong(StartKey);
        if (Elapsed >= MeltTicks) {
            World.sendParticles(Fizz, Dropped.getX(), Dropped.getY() + 0.15, Dropped.getZ(), 14, 0.18, 0.12, 0.18, 0.0);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, Dropped.getX(), Dropped.getY() + 0.2, Dropped.getZ(), 3, 0.1, 0.05, 0.1, 0.02);
            play(World, Dropped.position(), SulfurRegistry.MeltLayers, 0.6f, 0.9f + World.getRandom().nextFloat() * 0.2f);
            Dropped.discard();
            return true;
        }
        if (Seen != Now && World.getRandom().nextInt(SmokeChance) == 0) {
            World.sendParticles(Fizz, Dropped.getX(), Dropped.getY() + 0.15, Dropped.getZ(), 3, 0.12, 0.06, 0.12, 0.0);
            World.sendParticles(ParticleTypes.SMOKE, Dropped.getX(), Dropped.getY() + 0.2, Dropped.getZ(), 1, 0.05, 0.02, 0.05, 0.01);
        }
        return false;
    }

    public static void play(ServerLevel World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }
}

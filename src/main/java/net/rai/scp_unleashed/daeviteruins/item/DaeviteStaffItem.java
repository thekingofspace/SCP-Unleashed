package net.rai.scp_unleashed.daeviteruins.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;
import net.rai.scp_unleashed.daeviteruins.entity.DaeviteEmberBolt;
import net.rai.scp_unleashed.daeviteruins.entity.DaeviteFlare;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DaeviteStaffItem extends Item {
    public enum Spell {
        Ember(40, 24, 0.15, 0.25),
        Flare(32, 70, 0.2, 0.25),
        Blink(24, 80, 0.2, 0.3),
        Ward(28, 100, 0.2, 0.25);

        public final int Uses;
        public final int Cooldown;
        public final double Fizzle;
        public final double Wear;

        Spell(int Uses, int Cooldown, double Fizzle, double Wear) {
            this.Uses = Uses;
            this.Cooldown = Cooldown;
            this.Fizzle = Fizzle;
            this.Wear = Wear;
        }
    }

    public static final double BlinkReach = 6.0;
    public static final double WardRadius = 3.5;
    public static final float WardDamage = 2.0f;
    public static final int WardBurn = 60;
    public static final double WardPush = 0.8;
    private final Spell Kind;

    public DaeviteStaffItem(Spell Kind, Properties Settings) {
        super(Settings);
        this.Kind = Kind;
    }

    public Spell spell() {
        return Kind;
    }

    public static double fizzleChance(Spell Kind, ItemStack Stack) {
        double Worn = Stack.isDamageableItem() && Stack.getMaxDamage() > 0 ? Stack.getDamageValue() / (double) Stack.getMaxDamage() : 0.0;
        return Mth.clamp(Kind.Fizzle + Kind.Wear * Worn, 0.0, 0.95);
    }

    @Override
    public boolean isRepairable(ItemStack Stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(Stack);
        }
        if (World instanceof ServerLevel Server) {
            boolean Fizzled = World.getRandom().nextDouble() < fizzleChance(Kind, Stack);
            if (!Fizzled) {
                Fizzled = !cast(Server, User);
            }
            if (Fizzled) {
                fizzle(Server, User);
            }
            Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
            User.getCooldowns().addCooldown(this, Fizzled ? Kind.Cooldown / 2 : Kind.Cooldown);
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide());
    }

    private static Vec3 tip(Player User) {
        return User.getEyePosition().add(User.getLookAngle().scale(0.8)).add(0.0, -0.25, 0.0);
    }

    private boolean cast(ServerLevel World, Player User) {
        Vec3 Tip = tip(User);
        switch (Kind) {
            case Ember -> {
                DaeviteEmberBolt Bolt = new DaeviteEmberBolt(World, User, Tip);
                Bolt.shootFromRotation(User, User.getXRot(), User.getYRot(), 0.0f, 0.95f, 3.5f);
                World.addFreshEntity(Bolt);
                World.playSound(null, User.getX(), User.getY(), User.getZ(), DaeviteRuinsRegistry.StaffCast.get(), SoundSource.PLAYERS, 0.8f,
                        1.0f + World.getRandom().nextFloat() * 0.2f);
                return true;
            }
            case Flare -> {
                DaeviteFlare Flare = new DaeviteFlare(World, User, Tip);
                Flare.shootFromRotation(User, Math.min(User.getXRot() - 30.0f, -20.0f), User.getYRot(), 0.0f, 0.75f, 2.0f);
                World.addFreshEntity(Flare);
                World.playSound(null, User.getX(), User.getY(), User.getZ(), DaeviteRuinsRegistry.StaffCast.get(), SoundSource.PLAYERS, 0.7f, 0.75f);
                return true;
            }
            case Blink -> {
                return blink(World, User);
            }
            default -> {
                ward(World, User);
                return true;
            }
        }
    }

    private boolean blink(ServerLevel World, Player User) {
        if (User.isPassenger() || !(User instanceof ServerPlayer Traveller)) {
            return false;
        }
        Vec3 Look = User.getLookAngle();
        Vec3 Flat = new Vec3(Look.x, 0.0, Look.z);
        if (Flat.lengthSqr() < 1.0e-4) {
            return false;
        }
        Flat = Flat.normalize();
        Vec3 Start = User.position().add(0.0, 0.6, 0.0);
        BlockHitResult Hit = World.clip(new ClipContext(Start, Start.add(Flat.scale(BlinkReach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
        double Reach = Hit.getType() == BlockHitResult.Type.MISS ? BlinkReach : Math.max(0.0, Hit.getLocation().distanceTo(Start) - 0.7);
        for (double Distance = Reach; Distance >= 2.0; Distance -= 0.5) {
            Vec3 Target = User.position().add(Flat.scale(Distance));
            BlockPos Base = BlockPos.containing(Target);
            for (int Lift : new int[]{0, 1, -1}) {
                BlockPos Feet = Base.above(Lift);
                if (standable(World, Feet)) {
                    Vec3 From = User.position();
                    Traveller.teleportTo(Target.x, Feet.getY(), Target.z);
                    Traveller.resetFallDistance();
                    World.sendParticles(ParticleTypes.REVERSE_PORTAL, From.x, From.y + 1.0, From.z, 24, 0.3, 0.6, 0.3, 0.05);
                    World.sendParticles(ParticleTypes.FLAME, From.x, From.y + 0.6, From.z, 10, 0.3, 0.4, 0.3, 0.01);
                    World.sendParticles(ParticleTypes.REVERSE_PORTAL, Target.x, Feet.getY() + 1.0, Target.z, 24, 0.3, 0.6, 0.3, 0.05);
                    World.playSound(null, From.x, From.y, From.z, DaeviteRuinsRegistry.Blink.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                    World.playSound(null, Target.x, Feet.getY(), Target.z, DaeviteRuinsRegistry.Blink.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean standable(ServerLevel World, BlockPos Feet) {
        return World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty() && World.getBlockState(Feet.above()).getCollisionShape(World, Feet.above()).isEmpty()
                && World.getFluidState(Feet).isEmpty() && World.getBlockState(Feet.below()).isFaceSturdy(World, Feet.below(), Direction.UP);
    }

    private void ward(ServerLevel World, Player User) {
        AABB Area = User.getBoundingBox().inflate(WardRadius, 1.0, WardRadius);
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found != User && Found.isAlive())) {
            if (Target instanceof OwnableEntity Pet && Pet.getOwner() == User || Target.isAlliedTo(User)) {
                continue;
            }
            if (Target instanceof Player Other && (Other.isCreative() || Other.isSpectator())) {
                continue;
            }
            if (Target.distanceTo(User) > WardRadius + 0.5) {
                continue;
            }
            Target.hurt(World.damageSources().source(DaeviteRuinsRegistry.WardDamage, User, User), WardDamage);
            Target.igniteForTicks(WardBurn);
            Vec3 Away = Target.position().subtract(User.position());
            Target.knockback(WardPush, -Away.x, -Away.z);
        }
        for (int Step = 0; Step < 32; Step++) {
            double Angle = Step * Math.PI * 2 / 32;
            double X = User.getX() + Math.cos(Angle) * WardRadius * 0.85;
            double Z = User.getZ() + Math.sin(Angle) * WardRadius * 0.85;
            World.sendParticles(ParticleTypes.FLAME, X, User.getY() + 0.2, Z, 1, 0.05, 0.05, 0.05, 0.01);
            if (Step % 4 == 0) {
                World.sendParticles(ParticleTypes.LAVA, X, User.getY() + 0.3, Z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        World.playSound(null, User.getX(), User.getY(), User.getZ(), DaeviteRuinsRegistry.WardPulse.get(), SoundSource.PLAYERS, 0.9f, 0.9f);
    }

    private void fizzle(ServerLevel World, Player User) {
        Vec3 Tip = tip(User);
        World.sendParticles(ParticleTypes.SMOKE, Tip.x, Tip.y, Tip.z, 12, 0.12, 0.12, 0.12, 0.02);
        World.sendParticles(ParticleTypes.SMALL_FLAME, Tip.x, Tip.y, Tip.z, 3, 0.08, 0.08, 0.08, 0.01);
        World.playSound(null, User.getX(), User.getY(), User.getZ(), DaeviteRuinsRegistry.StaffFizzle.get(), SoundSource.PLAYERS, 0.8f,
                0.9f + World.getRandom().nextFloat() * 0.3f);
        if (Kind == Spell.Ward) {
            for (Entity Near : World.getEntities(User, User.getBoundingBox().inflate(1.8), Found -> Found instanceof LivingEntity && Found.isAlive())) {
                Vec3 Away = Near.position().subtract(User.position());
                ((LivingEntity) Near).knockback(0.3, -Away.x, -Away.z);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".details").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("tooltip.scp_unleashed.daevite_relic.staff").withStyle(ChatFormatting.DARK_RED));
        }
    }
}

package net.rai.scp_unleashed.marksman;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.guns.GunDamage;
import net.rai.scp_unleashed.guns.GunGlass;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.marksman.network.CoinRicochetPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CoinRicochet {
    public static final double BaseRadius = 0.8;
    public static final double NearRadius = 0.3;
    public static final double NearSlope = 0.25;
    public static final double ConeSlope = 0.045;
    public static final double NearestAlong = 0.3;
    public static final double TrailAllowance = 0.5;
    public static final double SeekRange = 32.0;
    public static final int MaxChain = 5;
    public static final float BaseBonus = 1.5F;
    public static final float ChainBonus = 0.25F;
    public static final double EffectRange = 128.0;
    public static final float PingJitter = 0.02F;
    public static final double StrayCarry = 0.6;
    public static final double StrayScatter = 0.9;
    public static final double StrayLift = 0.35;

    private CoinRicochet() {
    }

    @Nullable
    public static Vec3 deflect(ServerLevel World, ServerPlayer User, GunKind Kind, Vec3 Eye, Vec3 Heading, Vec3 Reach) {
        if (!(User.getMainHandItem().getItem() instanceof MarksmanRevolverItem)) {
            return null;
        }
        CoinEntity Struck = pick(World, User, Eye, Heading, Eye.distanceTo(Reach));
        if (Struck == null) {
            return null;
        }
        Vec3 Point = Struck.centre();
        resolve(World, User, Kind, Struck, Heading);
        return Point;
    }

    @Nullable
    private static CoinEntity pick(ServerLevel World, ServerPlayer User, Vec3 Eye, Vec3 Heading, double Limit) {
        Vec3 Look = User.getLookAngle();
        double Widest = BaseRadius + ConeSlope * Limit;
        AABB Area = new AABB(Eye, Eye.add(Heading.scale(Limit))).minmax(new AABB(Eye, Eye.add(Look.scale(Limit)))).inflate(Widest);
        CoinEntity Best = null;
        double BestScore = 1.0;
        for (CoinEntity Coin : World.getEntitiesOfClass(CoinEntity.class, Area.inflate(CoinEntity.HistoryTicks * TrailAllowance), Candidate -> !Candidate.isSpent())) {
            for (Vec3 Centre : Coin.recentCentres()) {
                double Score = Math.min(closeness(Eye, Heading, Centre, Limit), closeness(Eye, Look, Centre, Limit));
                if (Score > BestScore || !clear(World, Eye, Centre)) {
                    continue;
                }
                Best = Coin;
                BestScore = Score;
            }
        }
        return Best;
    }

    private static double closeness(Vec3 Eye, Vec3 Ray, Vec3 Centre, double Limit) {
        Vec3 Offset = Centre.subtract(Eye);
        double Along = Offset.dot(Ray);
        if (Along < NearestAlong || Along > Limit + BaseRadius) {
            return Double.MAX_VALUE;
        }
        double Across = Offset.subtract(Ray.scale(Along)).length();
        return Across / allowance(Along);
    }

    public static double allowance(double Along) {
        return Math.min(BaseRadius, NearRadius + NearSlope * Along) + ConeSlope * Along;
    }

    private static boolean clear(ServerLevel World, Vec3 From, Vec3 To) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    private static void resolve(ServerLevel World, ServerPlayer User, GunKind Kind, CoinEntity First, Vec3 Incoming) {
        List<Vec3> Path = new ArrayList<>();
        CoinEntity Current = First;
        Vec3 Heading = Incoming;
        int Outcome = CoinRicochetPayload.Stray;
        int Face = Direction.UP.get3DDataValue();
        int Links = 0;
        while (true) {
            Vec3 At = Current.centre();
            Current.spend();
            Path.add(At);
            GunSounds.play(World, null, At.x, At.y, At.z, CoinSounds.ricochet(Links), 1.0F, PingJitter);
            Links++;
            Foe Found = nearestEnemy(World, User, At);
            double Reach = Found == null ? SeekRange : At.distanceTo(Found.Aim());
            CoinEntity Next = Links < MaxChain ? nearestCoin(World, At, Reach) : null;
            if (Next != null) {
                Heading = Next.centre().subtract(At).normalize();
                Current = Next;
                continue;
            }
            if (Found != null) {
                LivingEntity Target = Found.Target();
                Vec3 Towards = Found.Aim().subtract(At).normalize();
                boolean Landed = GunDamage.hurt(World, User, Target, Kind, damage(Kind, Links), Towards);
                boolean Killed = !Target.isAlive() || Target.isDeadOrDying();
                Outcome = !Landed ? CoinRicochetPayload.Shrugged : Killed ? CoinRicochetPayload.Kill : CoinRicochetPayload.Wound;
                Path.add(Found.Aim());
                break;
            }
            Vec3 Away = strayDirection(Heading, World.getRandom());
            Vec3 End = At.add(Away.scale(SeekRange));
            BlockHitResult Surface = World.clip(new ClipContext(At, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (Surface.getType() != HitResult.Type.MISS) {
                End = Surface.getLocation();
                Outcome = CoinRicochetPayload.StrayBlock;
                Face = Surface.getDirection().get3DDataValue();
                strike(World, User, Surface);
                GunGlass.impact(World, User, Surface, damage(Kind, Links));
            }
            Path.add(End);
            break;
        }
        Vec3 Origin = Path.get(0);
        PacketDistributor.sendToPlayersNear(World, null, Origin.x, Origin.y, Origin.z, EffectRange, new CoinRicochetPayload(User.getId(), Path, Outcome, Face));
    }

    public static float damage(GunKind Kind, int Links) {
        return Kind.getStats().getDamage() * (BaseBonus + ChainBonus * Math.max(0, Links - 1));
    }

    private static void strike(ServerLevel World, ServerPlayer User, BlockHitResult Surface) {
        BlockPos Pos = Surface.getBlockPos();
        BlockState Struck = World.getBlockState(Pos);
        SoundType Sounds = Struck.getSoundType(World, Pos, User);
        World.playSound(null, Pos, Sounds.getHitSound(), SoundSource.BLOCKS, Sounds.getVolume() * 0.7F, Sounds.getPitch() * 1.25F);
        World.gameEvent(GameEvent.PROJECTILE_LAND, Surface.getLocation(), GameEvent.Context.of(User));
    }

    private static Vec3 strayDirection(Vec3 Heading, RandomSource Random) {
        Vec3 Scatter = new Vec3(Random.nextGaussian(), Random.nextGaussian(), Random.nextGaussian());
        Scatter = Scatter.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : Scatter.normalize();
        Vec3 Away = Heading.scale(StrayCarry).add(Scatter.scale(StrayScatter)).add(0.0, StrayLift, 0.0);
        return Away.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : Away.normalize();
    }

    @Nullable
    private static CoinEntity nearestCoin(ServerLevel World, Vec3 At, double Reach) {
        CoinEntity Best = null;
        double BestDistance = Reach * Reach;
        for (CoinEntity Coin : World.getEntitiesOfClass(CoinEntity.class, new AABB(At, At).inflate(Reach), Candidate -> !Candidate.isSpent())) {
            double Distance = Coin.centre().distanceToSqr(At);
            if (Distance < BestDistance && clear(World, At, Coin.centre())) {
                Best = Coin;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    @Nullable
    private static Foe nearestEnemy(ServerLevel World, ServerPlayer User, Vec3 At) {
        Entity Ride = User.getRootVehicle();
        List<LivingEntity> Candidates = World.getEntitiesOfClass(LivingEntity.class, new AABB(At, At).inflate(SeekRange),
                Candidate -> Candidate.getRootVehicle() != Ride && isEnemy(User, Candidate));
        Candidates.sort(Comparator.comparingDouble(Candidate -> Candidate.getBoundingBox().getCenter().distanceToSqr(At)));
        for (LivingEntity Candidate : Candidates) {
            Vec3 Aim = visiblePoint(World, At, Candidate);
            if (Aim != null && Aim.distanceToSqr(At) <= SeekRange * SeekRange) {
                return new Foe(Candidate, Aim);
            }
        }
        return null;
    }

    @Nullable
    private static Vec3 visiblePoint(ServerLevel World, Vec3 At, LivingEntity Target) {
        AABB Box = Target.getBoundingBox();
        Vec3 Centre = Box.getCenter();
        for (Vec3 Point : List.of(Centre, new Vec3(Centre.x, Target.getEyeY(), Centre.z), new Vec3(Centre.x, Box.minY + Box.getYsize() * 0.2, Centre.z))) {
            if (clear(World, At, Point)) {
                return Point;
            }
        }
        return null;
    }

    public static boolean isEnemy(ServerPlayer User, LivingEntity Target) {
        if (Target == User || !Target.isAlive() || Target.isSpectator() || Target instanceof ArmorStand) {
            return false;
        }
        if (Target instanceof OwnableEntity Pet && User.getUUID().equals(Pet.getOwnerUUID())) {
            return false;
        }
        if (User.isAlliedTo(Target)) {
            return false;
        }
        if (Target instanceof Player Other) {
            MinecraftServer Server = User.getServer();
            return Server != null && Server.isPvpAllowed() && !Other.isCreative() && User.canHarmPlayer(Other) && User.getLastHurtByMob() == Other;
        }
        if (Target instanceof Mob Hunter && Hunter.getTarget() == User) {
            return true;
        }
        if (Target instanceof NeutralMob Angry && User.getUUID().equals(Angry.getPersistentAngerTarget())) {
            return true;
        }
        if (User.getLastHurtByMob() == Target) {
            return true;
        }
        return Target instanceof Enemy || Target.getType().getCategory() == MobCategory.MONSTER;
    }

    private record Foe(LivingEntity Target, Vec3 Aim) {
    }
}

package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DaughterThorns extends CourtDaughter {
    public static final int Walls = MoveA;
    public static final int Roots = MoveB;
    public static final int Entangle = MoveC;
    public static final int Cage = MoveD;
    public static final int WallLife = 400;
    public static final int WallLifeBloom = 900;
    public static final int RootTelegraph = 24;
    public static final int MaxWalls = 15;
    public static final int MaxPatches = 6;
    public static final double CageHealth = 60.0;
    private final List<Vec3> Plan = new ArrayList<>();
    private final List<Vec3[]> Lines = new ArrayList<>();
    private Vec3 Circle = Vec3.ZERO;
    private int Rotation;

    public DaughterThorns(EntityType<? extends Monster> Type, Level World) {
        super(Type, World);
    }

    @Override
    public int index() {
        return 0;
    }

    @Override
    public String key() {
        return "thorns";
    }

    @Override
    protected BossEvent.BossBarColor barColour() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected double nearRange() {
        return 6.0;
    }

    @Override
    protected double farRange() {
        return 12.0;
    }

    @Override
    protected int moveLength(int Move) {
        return switch (Move) {
            case Walls -> 40;
            case Roots -> 40;
            case Entangle -> 32;
            case Cage -> 32;
            default -> 20;
        };
    }

    @Override
    protected int shiftLength() {
        return 60;
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Entrance -> "entrance";
            case Death -> "death";
            case Shift -> "bloom";
            case Walls -> "walls";
            case Roots -> "roots";
            case Entangle -> "entangle";
            case Cage -> "cage";
            default -> null;
        };
    }

    @Override
    protected void chooseMove(LivingEntity Target) {
        Rotation++;
        boolean Bloom = phaseTwo();
        int Move;
        if (Rotation % (Bloom ? 3 : 5) == 0 && level().getEntitiesOfClass(CourtProp.class, getBoundingBox().inflate(48.0), Each -> Each.kind() == CourtProp.PropKind.BriarCage).isEmpty()) {
            Move = Cage;
        } else {
            int[] Order = {Roots, Walls, Entangle, Roots, Entangle};
            Move = Order[Rotation % Order.length];
        }
        startMove(Move, Target);
        Cooldown = Bloom ? 34 : 50;
    }

    @Override
    protected void moveTick(int Action, int Elapsed, @Nullable LivingEntity Target) {
        ServerLevel World = (ServerLevel) level();
        face(Target);
        getNavigation().stop();
        switch (Action) {
            case Walls -> walls(World, Elapsed, Target);
            case Roots -> roots(World, Elapsed, Target);
            case Entangle -> entangle(World, Elapsed, Target);
            case Cage -> cage(World, Elapsed, Target);
            default -> {
            }
        }
    }

    private void walls(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        if (Elapsed == 1) {
            Plan.clear();
            Vec3 Aim = Target != null ? Target.position() : position().add(facing().scale(6.0));
            Vec3 Way = CourtFx.flat(Aim.subtract(position()));
            Vec3 Across = new Vec3(-Way.z, 0.0, Way.x);
            Vec3 Middle = Aim.add(Way.scale(2.5));
            int Count = 3 + Math.min(2, Players - 1) + (phaseTwo() ? 1 : 0);
            for (int Index = 0; Index < Count; Index++) {
                double Offset = (Index - (Count - 1) / 2.0) * 1.6;
                double Bend = Math.abs(Offset) * 0.35;
                Plan.add(Middle.add(Across.scale(Offset)).subtract(Way.scale(Bend)));
            }
            CourtFx.sound(World, position(), "thorns_idle", 3.0f, 0.8f);
        }
        if (Elapsed < 24 && Elapsed % 4 == 0) {
            for (Vec3 Spot : Plan) {
                World.sendParticles(CourtFx.DeepCrimson, Spot.x, Spot.y + 0.15, Spot.z, 4, 0.4, 0.05, 0.4, 0.0);
                CourtFx.crumble(World, CourtFx.solidAt(World, BlockPos.containing(Spot).below()), Spot, 2, 0.4, 0.05);
            }
        }
        if (Elapsed == 24) {
            CourtFx.sound(World, position(), "thorns_wall", 4.0f, 1.0f);
            int Existing = World.getEntitiesOfClass(CourtProp.class, getBoundingBox().inflate(64.0), Each -> Each.kind() == CourtProp.PropKind.ThornWall).size();
            for (Vec3 Spot : Plan) {
                if (Existing++ >= MaxWalls) {
                    break;
                }
                CourtProp Wall = CourtProp.spawn(World, ScarletCourtRegistry.ThornWallType.get(), BlockPos.containing(Spot), phaseTwo() ? WallLifeBloom : WallLife);
                if (Wall != null) {
                    Wall.owner(this);
                    Wall.face(getYRot());
                    for (LivingEntity Victim : CourtFx.victimsNear(World, Spot, 1.2)) {
                        CourtFx.push(Victim, Spot, 0.8, 0.35);
                    }
                }
            }
        }
    }

    private void roots(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        if (Elapsed == 1) {
            Lines.clear();
            Vec3 Aim = Target != null ? Target.position() : position().add(facing().scale(8.0));
            Vec3 Way = CourtFx.flat(Aim.subtract(position()));
            double[] Fan = phaseTwo() ? new double[]{-25.0, 0.0, 25.0} : new double[]{0.0};
            for (double Angle : Fan) {
                Vec3 Dir = CourtFx.turn(Way, Angle);
                Vec3 From = position().add(Dir.scale(1.5));
                Lines.add(new Vec3[]{From, From.add(Dir.scale(15.0))});
            }
            CourtFx.sound(World, position(), "thorns_roots", 2.5f, 0.7f);
        }
        if (Elapsed < RootTelegraph && Elapsed % 3 == 0) {
            double Reach = Elapsed / (double) RootTelegraph;
            for (Vec3[] Line : Lines) {
                Vec3 End = Line[0].add(Line[1].subtract(Line[0]).scale(Reach));
                CourtFx.line(World, CourtFx.DeepCrimson, Line[0].add(0.0, 0.1, 0.0), End.add(0.0, 0.1, 0.0), 0.8, 0.05);
                CourtFx.crumble(World, CourtFx.solidAt(World, BlockPos.containing(End).below()), End, 3, 0.2, 0.02);
            }
        }
        if (Elapsed == RootTelegraph) {
            CourtFx.sound(World, position(), "thorns_roots", 4.0f, 1.0f);
            for (Vec3[] Line : Lines) {
                Vec3 Way = Line[1].subtract(Line[0]);
                double Length = Way.length();
                for (double Step = 0.0; Step <= Length; Step += 1.0) {
                    Vec3 Spot = Line[0].add(Way.normalize().scale(Step));
                    CourtFx.crumble(World, Blocks.DARK_OAK_LOG.defaultBlockState(), Spot.add(0.0, 0.6, 0.0), 4, 0.2, 0.5);
                    World.sendParticles(CourtFx.Crimson, Spot.x, Spot.y + 1.0, Spot.z, 2, 0.15, 0.6, 0.15, 0.0);
                }
                for (LivingEntity Victim : CourtFx.victims(World, new net.minecraft.world.phys.AABB(Line[0], Line[1]).inflate(1.5, 2.0, 1.5))) {
                    if (CourtFx.segmentDistance(Victim.position(), Line[0], Line[1]) < 1.1 && CourtFx.strike(this, Victim, 8.0f)) {
                        Victim.setDeltaMovement(Victim.getDeltaMovement().add(0.0, 0.65, 0.0));
                        Victim.hurtMarked = true;
                    }
                }
            }
        }
    }

    private void entangle(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        if (Elapsed == 1) {
            Circle = Target != null ? Target.position() : position().add(facing().scale(5.0));
            CourtFx.sound(World, Circle, "thorns_idle", 2.0f, 1.2f);
        }
        if (Elapsed < 20 && Elapsed % 3 == 0) {
            CourtFx.ring(World, CourtFx.Crimson, Circle, 2.2, 24, 0.1);
            CourtFx.ring(World, CourtFx.DeepCrimson, Circle, 2.2 * (Elapsed / 20.0), 12, 0.1);
        }
        if (Elapsed == 20) {
            CourtFx.sound(World, Circle, "thorns_roots", 2.5f, 1.3f);
            CourtFx.crumble(World, Blocks.DARK_OAK_LOG.defaultBlockState(), Circle.add(0.0, 0.5, 0.0), 24, 1.4, 0.4);
            for (LivingEntity Victim : CourtFx.victimsNear(World, Circle, 2.4)) {
                if (Math.abs(Victim.getY() - Circle.y) < 2.0) {
                    Victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, true, true));
                    CourtFx.strike(this, Victim, 4.0f);
                }
            }
        }
    }

    private void cage(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        if (Elapsed == 1 && Target != null) {
            Circle = Target.position();
            CourtFx.sound(World, Circle, "thorns_cage", 2.0f, 0.8f);
        }
        if (Elapsed < 20 && Elapsed % 2 == 0) {
            double Angle = Elapsed * 0.6;
            for (int Arm = 0; Arm < 4; Arm++) {
                double A = Angle + Arm * Math.PI / 2.0;
                World.sendParticles(CourtFx.DeepCrimson, Circle.x + Math.cos(A) * 1.1, Circle.y + Elapsed * 0.12, Circle.z + Math.sin(A) * 1.1, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (Elapsed == 20 && Target != null && Target.isAlive() && Target.position().distanceTo(Circle) < 3.0) {
            cageAround(World, Target);
        }
    }

    public CourtProp cageAround(ServerLevel World, LivingEntity Target) {
        CourtProp Briar = CourtProp.spawn(World, ScarletCourtRegistry.BriarCageType.get(), Target.blockPosition(), 240);
        if (Briar != null) {
            Briar.moveTo(Target.getX(), Briar.getY(), Target.getZ(), Briar.getYRot(), 0.0f);
            double Health = CageHealth * (1.0 + 0.5 * Math.max(0, Players - 1));
            Briar.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Health);
            Briar.setHealth((float) Health);
            Briar.owner(this);
            Briar.captive(Target);
            CourtFx.sound(World, Target.position(), "thorns_cage", 3.0f, 1.0f);
        }
        return Briar;
    }

    @Override
    protected void shiftTick(int Elapsed) {
        ServerLevel World = (ServerLevel) level();
        getNavigation().stop();
        if (Elapsed == 1) {
            CourtFx.sound(World, position(), "thorns_bloom", 6.0f, 0.9f);
        }
        if (Elapsed % 5 == 0) {
            CourtFx.burst(World, CourtFx.Crimson, position().add(0.0, getBbHeight() * 0.9, 0.0), 12, 1.6, 0.02);
            CourtFx.burst(World, ParticleTypes.CHERRY_LEAVES, position().add(0.0, getBbHeight() * 0.9, 0.0), 4, 1.8, 0.0);
        }
        if (Elapsed == 40) {
            for (CourtProp Wall : World.getEntitiesOfClass(CourtProp.class, getBoundingBox().inflate(64.0), Each -> Each.kind() == CourtProp.PropKind.ThornWall)) {
                Wall.life(WallLifeBloom);
            }
        }
    }

    @Override
    protected void arenaTick(ServerLevel World) {
        if (tickCount % 200 != 100) {
            return;
        }
        int Count = World.getEntitiesOfClass(CourtProp.class, getBoundingBox().inflate(64.0), Each -> Each.kind() == CourtProp.PropKind.ThornPatch).size();
        if (Count < MaxPatches) {
            BlockPos Spot = CourtArena.scatter(World, Dais, 5.0, Math.max(6.0, CourtArena.floorRadius() * 0.5), random);
            CourtProp.spawn(World, ScarletCourtRegistry.ThornPatchType.get(), Spot, 300);
            CourtFx.sound(World, Vec3.atCenterOf(Spot), "crawler_thorns", 1.0f, 0.8f);
        }
    }

    @Override
    protected void dyingTick(ServerLevel World, int Elapsed) {
        if (Elapsed < 40 && Elapsed % 4 == 0) {
            CourtFx.crumble(World, Blocks.DARK_OAK_LOG.defaultBlockState(), position().add(0.0, 0.4, 0.0), 10, 1.6, 0.2);
        }
        if (Elapsed >= 30 && Elapsed % 3 == 0) {
            CourtFx.crumble(World, Blocks.BLACKSTONE.defaultBlockState(), position().add(0.0, getBbHeight() * (1.0 - (Elapsed - 30) / 50.0), 0.0), 12, 1.0, 0.4);
            CourtFx.burst(World, ParticleTypes.WHITE_ASH, position().add(0.0, getBbHeight() * 0.5, 0.0), 20, 1.4, 0.02);
        }
        if (Elapsed == 76) {
            CourtFx.burst(World, ParticleTypes.LARGE_SMOKE, position().add(0.0, 2.0, 0.0), 40, 1.6, 0.04);
            CourtFx.burst(World, CourtFx.Ash, position().add(0.0, 2.0, 0.0), 60, 1.8, 0.0);
        }
    }

    @Override
    protected void onDying(ServerLevel World) {
        for (CourtProp Prop : World.getEntitiesOfClass(CourtProp.class, getBoundingBox().inflate(64.0),
                Each -> Each.kind() == CourtProp.PropKind.ThornWall || Each.kind() == CourtProp.PropKind.BriarCage || Each.kind() == CourtProp.PropKind.ThornPatch)) {
            Prop.end();
        }
    }
}

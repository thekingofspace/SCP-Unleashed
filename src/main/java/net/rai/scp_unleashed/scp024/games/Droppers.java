package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class Droppers {
    public static final DustParticleOptions Warning = new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.1f), 1.2f);
    public static final double Gravity = 0.06;
    public static final int FuseTicks = 30;
    public static final double Arc = 4.0;

    private static final class Falling {
        final Entity Visual;
        final Vec3 From;
        final Vec3 To;
        final boolean Bomb;
        final int Flight;
        int Age;
        int Fuse = -1;
        double Speed;
        double Y;

        Falling(Entity Visual, Vec3 From, Vec3 To, boolean Bomb, int Flight) {
            this.Visual = Visual;
            this.From = From;
            this.To = To;
            this.Bomb = Bomb;
            this.Flight = Flight;
            this.Y = From.y;
        }
    }

    private final List<Falling> Active = new ArrayList<>();
    private final float AnvilDamage;
    private final float BombDamage;
    private final double BombRadius;

    public Droppers(float AnvilDamage, float BombDamage, double BombRadius) {
        this.AnvilDamage = AnvilDamage;
        this.BombDamage = BombDamage;
        this.BombRadius = BombRadius;
    }

    public int active() {
        return Active.size();
    }

    public void anvil(Scp024Session Session, Vec3 Floor, double Height) {
        Vec3 Top = Floor.add(0.0, Height, 0.0);
        Entity Visual = Scp024Hazards.display(Session.World, "minecraft:block_display", Top, Scp024Hazards.blockDisplay(Blocks.ANVIL.defaultBlockState(), 1.0f, 2));
        if (Visual == null) {
            return;
        }
        Session.spawn(Visual);
        Active.add(new Falling(Visual, Top, Floor, false, 0));
        Session.sound(Floor, Scp024Registry.NoteFlute.get(), 0.5f, 1.8f);
    }

    public void bomb(Scp024Session Session, Vec3 From, Vec3 Floor, int Flight) {
        Entity Visual = Scp024Hazards.display(Session.World, "minecraft:block_display", From, Scp024Hazards.blockDisplay(Blocks.TNT.defaultBlockState(), 0.8f, 2));
        if (Visual == null) {
            return;
        }
        Session.spawn(Visual);
        Active.add(new Falling(Visual, From, Floor, true, Flight));
        Session.sound(From, Scp024Registry.Click.get(), 0.9f, 0.6f);
    }

    private static void ring(Scp024Session Session, Vec3 Spot, double Radius) {
        for (int Step = 0; Step < 10; Step++) {
            double Angle = Step / 10.0 * Math.PI * 2.0;
            Session.World.sendParticles(Warning, Spot.x + Math.cos(Angle) * Radius, Spot.y + 0.1, Spot.z + Math.sin(Angle) * Radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public void tick(Scp024Session Session) {
        for (Falling Drop : new ArrayList<>(Active)) {
            if (Session.closed()) {
                return;
            }
            Drop.Age++;
            if (Drop.Visual.isRemoved()) {
                Active.remove(Drop);
                continue;
            }
            if (Drop.Bomb) {
                if (Drop.Fuse < 0) {
                    double Progress = Math.min(1.0, Drop.Age / (double) Drop.Flight);
                    Vec3 Spot = Drop.From.lerp(Drop.To, Progress).add(0.0, Arc * 4.0 * Progress * (1.0 - Progress), 0.0);
                    Drop.Visual.setPos(Spot.x, Spot.y, Spot.z);
                    if (Drop.Age % 3 == 0) {
                        ring(Session, Drop.To, 1.2);
                    }
                    if (Progress >= 1.0) {
                        Drop.Fuse = FuseTicks;
                        Session.sound(Drop.To, Scp024Registry.Fuse.get(), 0.9f, 1.0f);
                    }
                    continue;
                }
                Drop.Fuse--;
                Drop.Visual.setGlowingTag(Drop.Fuse % 6 < 3);
                Session.World.sendParticles(ParticleTypes.SMOKE, Drop.To.x, Drop.To.y + 0.9, Drop.To.z, 1, 0.05, 0.05, 0.05, 0.01);
                if (Drop.Fuse % 5 == 0) {
                    ring(Session, Drop.To, BombRadius * 0.8);
                }
                if (Drop.Fuse <= 0) {
                    Drop.Visual.discard();
                    Active.remove(Drop);
                    Session.blast(Drop.To, BombRadius, BombDamage);
                }
                continue;
            }
            Drop.Speed += Gravity;
            Drop.Y -= Drop.Speed;
            if (Drop.Age % 3 == 0) {
                ring(Session, Drop.To, 0.6);
            }
            if (Drop.Y > Drop.To.y) {
                Drop.Visual.setPos(Drop.To.x, Drop.Y, Drop.To.z);
                continue;
            }
            Drop.Visual.discard();
            Active.remove(Drop);
            land(Session, Drop.To);
        }
    }

    private void land(Scp024Session Session, Vec3 Spot) {
        BlockPos Floor = BlockPos.containing(Spot.x, Spot.y - 0.5, Spot.z);
        BlockState Under = Session.World.getBlockState(Floor);
        if (!Under.isAir()) {
            Session.World.levelEvent(2001, Floor, Block.getId(Under));
        }
        Session.sound(Spot, Scp024Registry.AnvilLand.get(), 1.0f, 0.85f + Session.Random.nextFloat() * 0.2f);
        ServerPlayer User = Session.player();
        if (User == null) {
            return;
        }
        AABB Strike = new AABB(Spot.x - 0.55, Spot.y - 0.2, Spot.z - 0.55, Spot.x + 0.55, Spot.y + 2.0, Spot.z + 0.55);
        if (User.getBoundingBox().intersects(Strike)) {
            User.hurt(Session.World.damageSources().source(DamageTypes.FALLING_ANVIL), AnvilDamage);
        } else if (User.getBoundingBox().inflate(0.8, 0.0, 0.8).intersects(Strike)) {
            Session.taunt("near");
        }
    }

    public void clear() {
        for (Falling Drop : Active) {
            Drop.Visual.discard();
        }
        Active.clear();
    }
}

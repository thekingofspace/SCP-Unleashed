package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ArrowStorm {
    public static final int Warn = 8;
    public static final int ArrowLife = 50;

    private record Shot(Vec3 From, Vec3 To, int At, float Speed, double Damage) {
    }

    private final List<Shot> Queued = new ArrayList<>();
    private final List<AbstractArrow> Flying = new ArrayList<>();

    public void queue(Scp024Session Session, Vec3 From, Vec3 To, int Delay, float Speed, double Damage) {
        Queued.add(new Shot(From, To, Session.ticks() + Delay, Speed, Damage));
        Session.sound(From, Scp024Registry.Click.get(), 0.6f, 1.6f);
    }

    public void aimed(Scp024Session Session, int Width, int Depth, double Height, float Speed, double Damage) {
        ServerPlayer User = Session.player();
        if (User == null) {
            return;
        }
        int Wall = Session.Random.nextInt(4);
        double Along = Session.Random.nextDouble();
        double X = Wall == 0 ? -0.4 : Wall == 1 ? Width - 0.6 : Along * Width;
        double Z = Wall == 2 ? -0.4 : Wall == 3 ? Depth - 0.6 : Along * Depth;
        Vec3 From = new Vec3(Session.Origin.getX() + X, Session.Origin.getY() + Height, Session.Origin.getZ() + Z);
        Vec3 Lead = User.position().add(User.getDeltaMovement().multiply(6.0, 0.0, 6.0)).add(0.0, 1.0, 0.0);
        queue(Session, From, Lead, Warn, Speed, Damage);
    }

    public void tick(Scp024Session Session) {
        if (Session.closed()) {
            return;
        }
        Iterator<Shot> Each = Queued.iterator();
        while (Each.hasNext()) {
            Shot Next = Each.next();
            int Left = Next.At() - Session.ticks();
            if (Left > 0) {
                if (Left % 3 == 0) {
                    Session.World.sendParticles(ParticleTypes.SMOKE, Next.From().x, Next.From().y, Next.From().z, 2, 0.05, 0.05, 0.05, 0.01);
                }
                continue;
            }
            Each.remove();
            Flying.add(Scp024Hazards.arrow(Session, Next.From(), Next.To(), Next.Speed(), Next.Damage()));
            Session.sound(Next.From(), Scp024Registry.Click.get(), 0.8f, 0.9f);
        }
        Iterator<AbstractArrow> Arrows = Flying.iterator();
        while (Arrows.hasNext()) {
            Entity Arrow = Arrows.next();
            if (Arrow.isRemoved() || Arrow.tickCount > ArrowLife) {
                Arrow.discard();
                Arrows.remove();
            }
        }
    }
}

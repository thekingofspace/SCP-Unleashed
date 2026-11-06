package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LightsOutGame implements Scp024Game {
    public static final int Seconds = 60;
    public static final double StalkerSpeed = 0.17;
    public static final double StalkerDamage = 3.0;
    public static final int GrowlEvery = 60;
    @Nullable
    private int[] Exit;
    @Nullable
    private Husk Stalker;

    @Override
    public String id() {
        return "lights_out";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.lightsOut(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public boolean lit() {
        return false;
    }

    @Override
    public void start(Scp024Session Session) {
        List<int[]> Corners = Session.marks("exit");
        int Pick = Session.Random.nextInt(Corners.size());
        Exit = Corners.get(Pick);
        Session.set(Exit[0], 0, Exit[2], Scp024Mat.GoldBlock);
        Session.set(Exit[0], 2, Exit[2], Scp024Mat.DimLight);
        int[] Lair = Corners.get(3 - Pick);
        Husk Hunter = new Husk(EntityType.HUSK, Session.World);
        Vec3 At = Session.centre(Lair);
        Hunter.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        Scp024Hazards.contestantFoe(Hunter, 20.0, StalkerDamage, StalkerSpeed);
        Hunter.setSilent(false);
        Session.spawn(Hunter);
        Stalker = Hunter;
        ServerPlayer User = Session.player();
        if (User != null) {
            User.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Seconds * 20 + 60, 0, false, false, true));
            Scp024Hazards.hunt(Hunter, User);
        }
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (User == null || Exit == null) {
            return Scp024Outcome.Continue;
        }
        if (Session.onCell(User, Exit[0], 0, Exit[2])) {
            return Scp024Outcome.Win;
        }
        if (Stalker != null && Stalker.isAlive()) {
            if (Session.ticks() % 20 == 0) {
                Scp024Hazards.hunt(Stalker, User);
            }
            if (Session.ticks() % GrowlEvery == 0) {
                Session.sound(Stalker.position(), SoundEvents.HUSK_AMBIENT, 1.0f, 0.6f);
            }
            if (Stalker.distanceTo(User) < 3.0) {
                Session.taunt("near");
            }
        }
        return Scp024Outcome.Continue;
    }

    @Override
    public void cleanup(Scp024Session Session) {
        ServerPlayer User = Session.anyPlayer();
        if (User != null) {
            User.removeEffect(MobEffects.DARKNESS);
        }
    }
}

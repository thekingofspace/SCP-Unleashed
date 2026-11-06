package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class RisingLavaGame implements Scp024Game {
    public static final int Seconds = 120;
    public static final int Size = 9;
    public static final int Grace = 100;
    public static final int RiseEvery = 30;
    public static final double Submerged = 0.6;
    private int Level;
    private int Top;

    @Override
    public String id() {
        return "rising_lava";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.risingLava(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        Level = 0;
        Top = Session.marks("finish").isEmpty() ? 34 : Session.marks("finish").get(0)[1] - 2;
    }

    private void rise(Scp024Session Session) {
        Level++;
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                if (Session.isOpen(X, Level, Z)) {
                    Session.set(X, Level, Z, Blocks.LAVA.defaultBlockState());
                }
            }
        }
        Vec3 Middle = Session.centre(Size / 2, Level, Size / 2);
        Session.sound(Middle, SoundEvents.LAVA_POP, 1.0f, 0.8f);
        Session.World.sendParticles(ParticleTypes.LAVA, Middle.x, Middle.y + 1.0, Middle.z, 6, 2.5, 0.1, 2.5, 0.0);
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (User == null) {
            return Scp024Outcome.Continue;
        }
        if (Session.onFinish(User)) {
            return Scp024Outcome.Win;
        }
        if (Session.ticks() >= Grace && (Session.ticks() - Grace) % RiseEvery == 0 && Level < Top) {
            rise(Session);
        }
        double Eyes = User.getEyeY() - Session.Origin.getY();
        if (Level > 0 && Eyes < Level + Submerged) {
            return Scp024Outcome.Lose;
        }
        double Gap = Session.height(User) - (Level + 1);
        if (Level > 0 && Gap < 1.5) {
            Session.taunt("near");
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.lava", Math.max(0, (int) Math.floor(Gap))));
        return Scp024Outcome.Continue;
    }
}

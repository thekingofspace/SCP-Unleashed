package net.rai.scp_unleashed.scp407.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.scp407.Scp407Fusion;
import net.rai.scp_unleashed.scp407.Scp407Hearing;
import net.rai.scp_unleashed.scp407.Scp407Session;
import net.rai.scp_unleashed.scp407.Scp407Sessions;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp407AnchorGameTests {
    public static final String Arena = Scp407GameTests.Arena;
    public static final double Lift = 1.9;
    public static final int SealLimit = 120;

    private Scp407AnchorGameTests() {
    }

    private static Scp407Session.Listener entry(Scp407Session Session, DClass Body) {
        return Session.Listeners.get(Scp407Hearing.keyOf(Body));
    }

    private static boolean sealed(Scp407Session Session, DClass Body) {
        Scp407Session.Listener Entry = entry(Session, Body);
        return Entry != null && Entry.Fused && Entry.PodStage >= Scp407Fusion.Stages;
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp407_anchor_midair")
    public static void listenerFusedMidJumpIsPoddedOnTheFloor(GameTestHelper Helper) {
        Scp407GameTests.room(Helper, 3, 3);
        SpeakerBlockEntity Speaker = Scp407GameTests.speaker(Helper, new BlockPos(2, 4, 6), Direction.EAST);
        DClass Body = DClassRegistry.DClassType.get().create(Helper.getLevel());
        Vec3 Spot = Helper.absoluteVec(new Vec3(6.5, 2, 6.5));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.identify(DClassTrait.Stoic, DClassCrime.Assault, 0);
        Helper.getLevel().addFreshEntity(Body);
        Scp407Session Session = Scp407GameTests.play(Helper, Speaker);
        int Floor = Helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        Helper.startSequence().thenExecuteAfter(12, () -> Scp407GameTests.skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
        }).thenWaitUntil(Scp407GameTests.within(Helper, 40, "The D-Class did not become a listener", () -> entry(Session, Body) != null && Body.onGround())).thenExecute(() -> {
            Body.teleportTo(Body.getX(), Body.getY() + Lift, Body.getZ());
            Body.setDeltaMovement(Vec3.ZERO);
            Scp407GameTests.skip(Helper, Session, Scp407Fusion.fuseAt(Session, entry(Session, Body)));
        }).thenWaitUntil(Scp407GameTests.within(Helper, SealLimit, "The D-Class was not sealed", () -> sealed(Session, Body))).thenExecute(() -> {
            Scp407Session.Listener Entry = entry(Session, Body);
            Helper.assertTrue(Entry.Anchor != null && Entry.Anchor.getY() == Floor, "The pod was anchored at height " + (Entry.Anchor == null ? "none" : Entry.Anchor.getY() - Floor)
                    + " above the floor");
            Helper.assertTrue(Math.abs(Entry.HoldY - Floor) < 1.0e-3, "The D-Class is held " + (Entry.HoldY - Floor) + " above the floor");
            Helper.assertTrue(Scp407Fusion.isHeld(Body) && Body.blockPosition().equals(Entry.Anchor), "The D-Class is not held in its pod");
            Scp407GameTests.clean(Helper);
        }).thenSucceed();
    }
}

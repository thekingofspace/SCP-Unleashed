package net.rai.scp_unleashed.scp149.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Goals;
import net.rai.scp_unleashed.scp149.Scp149Registry;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp149GameTests {
    public static final String Arena = "gear_arena";
    public static final int WatchTicks = 160;
    public static final int SeekAttempts = 400;

    private Scp149GameTests() {
    }

    private static DClass person(GameTestHelper Helper) {
        return Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(9.5, 1.0, 3.5));
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp149Fly.class);
        Helper.killAllEntitiesOfClass(DClass.class);
    }

    @GameTest(template = Arena, timeoutTicks = WatchTicks + 40, batch = "scp149_gorgedFliesLeavePeopleAlone")
    public static void gorgedFliesLeavePeopleAlone(GameTestHelper Helper) {
        DClass Body = person(Helper);
        Scp149Fly Fed = Helper.spawn(Scp149Registry.Fly.get(), new Vec3(3.5, 2.0, 3.5));
        Fed.setTarget(Body);
        Fed.feed(Helper.getLevel());
        Helper.assertTrue(Fed.getTarget() == null, "feeding did not make the fly let go of its prey");
        Helper.assertTrue(Fed.isGorged(), "a fed fly was not gorged");
        boolean[] Chased = new boolean[1];
        Helper.onEachTick(() -> Chased[0] |= Fed.getTarget() == Body);
        Helper.runAfterDelay(WatchTicks, () -> {
            try {
                Helper.assertFalse(Chased[0], "a gorged fly went back after a person");
                Helper.assertTrue(Body.getLastHurtByMob() != Fed, "a gorged fly bit a person");
            } finally {
                tidy(Helper);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp149_hungryFliesHuntPeople")
    public static void hungryFliesHuntPeople(GameTestHelper Helper) {
        DClass Body = person(Helper);
        Scp149Fly Fly = Helper.spawn(Scp149Registry.Fly.get(), new Vec3(3.5, 2.0, 3.5));
        try {
            Helper.assertFalse(Fly.isGorged(), "a new fly started out gorged");
            Helper.assertTrue(Scp149Fly.isPerson(Body), "the test D-Class does not count as a person");
            Helper.assertTrue(seeks(Fly), "a hungry fly would not pick a person in reach");
            Fly.feed(Helper.getLevel());
            Helper.assertFalse(seeks(Fly), "a gorged fly still picked a person");
        } finally {
            tidy(Helper);
        }
        Helper.succeed();
    }

    private static boolean seeks(Scp149Fly Fly) {
        Scp149Goals.Seek Probe = new Scp149Goals.Seek(Fly);
        for (int Attempt = 0; Attempt < SeekAttempts; Attempt++) {
            if (Probe.canUse()) {
                return true;
            }
        }
        return false;
    }
}

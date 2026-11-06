package net.rai.scp_unleashed.scp610.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.labware.LabContact;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.scp610.Scp610Apostle;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Hosts;
import net.rai.scp_unleashed.scp610.Scp610Infection;
import net.rai.scp_unleashed.scp610.Scp610Registry;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp610InfectionGameTests {
    public static final int Hits = 60;

    private Scp610InfectionGameTests() {
    }

    private static <T extends Mob> T still(GameTestHelper Helper, EntityType<T> Type, double X, double Z) {
        T Made = Helper.spawn(Type, new Vec3(X, Scp610TestKit.Ground, Z));
        Made.setNoAi(true);
        return Made;
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_infection")
    public static void infectionProgressesThroughStages(GameTestHelper Helper) {
        ServerPlayer User = Scp610TestKit.survivor(Helper, 4.5, Scp610TestKit.Ground, 4.5);
        Helper.assertTrue(Scp610Infection.infect(User), "a bare player could not catch SCP-610");
        Helper.assertTrue(Scp610Infection.stageOf(User) == 1, "a fresh infection is not stage 1");
        Scp610Infection.setElapsed(User, Scp610Infection.StageStarts[1] - 1);
        Scp610Infection.tick(User);
        Helper.assertTrue(Scp610Infection.stageOf(User) == 2, "the infection did not reach stage 2 on time");
        Helper.assertTrue(User.hasEffect(MobEffects.HUNGER), "stage 2 brought no hunger");
        Scp610Infection.setElapsed(User, Scp610Infection.StageStarts[2] - 1);
        Scp610Infection.tick(User);
        Helper.assertTrue(Scp610Infection.stageOf(User) == 3, "the infection did not reach stage 3 on time");
        Helper.assertTrue(User.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && User.hasEffect(MobEffects.WEAKNESS), "stage 3 is missing its slowness or weakness");
        Scp610Infection.setElapsed(User, Scp610Infection.StageStarts[3]);
        Helper.assertTrue(Scp610Infection.stageOf(User) == 4, "the infection did not reach stage 4");
        Scp610TestKit.dismiss(User);
        Scp610TestKit.clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_infection")
    public static void anUnsuitedCarrierSpreadsItToAnUnsuitedPlayer(GameTestHelper Helper) {
        ServerPlayer Carrier = Scp610TestKit.survivor(Helper, 8.5, Scp610TestKit.Ground, 8.5);
        ServerPlayer Victim = Scp610TestKit.survivor(Helper, 9.5, Scp610TestKit.Ground, 8.5);
        Scp610Infection.infect(Carrier);
        Scp610Infection.setStage(Carrier, 4);
        Scp610TestKit.touch(Victim, Carrier, LabContact.Strike, Hits);
        Helper.assertTrue(Scp610Infection.isInfected(Victim), "an unsuited carrier's blows never passed SCP-610 to an unsuited player");
        Villager Struck = still(Helper, EntityType.VILLAGER, 8.5, 10.5);
        Scp610TestKit.hit(Struck, Carrier, Hits);
        Helper.assertTrue(Scp610Infection.isInfected(Struck), "real blows from a carrier never passed SCP-610 on");
        ServerPlayer Neighbour = Scp610TestKit.survivor(Helper, 9.0, Scp610TestKit.Ground, 9.5);
        Scp610Infection.setStage(Carrier, 3);
        ServerLevel World = Helper.getLevel();
        for (int Cough = 0; Cough < 20 && !Scp610Infection.isInfected(Neighbour); Cough++) {
            Scp610Infection.cough(World, Carrier);
        }
        Helper.assertTrue(Scp610Infection.isInfected(Neighbour), "coughing never passed SCP-610 on");
        Scp610TestKit.dismiss(Carrier);
        Scp610TestKit.dismiss(Victim);
        Scp610TestKit.dismiss(Neighbour);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_infection")
    public static void aSuitedCarrierDoesNotSpreadIt(GameTestHelper Helper) {
        ServerPlayer Carrier = Scp610TestKit.survivor(Helper, 14.5, Scp610TestKit.Ground, 14.5);
        ServerPlayer Victim = Scp610TestKit.survivor(Helper, 15.5, Scp610TestKit.Ground, 14.5);
        Scp610Infection.infect(Carrier);
        Scp610Infection.setStage(Carrier, 4);
        Scp610TestKit.suit(Carrier);
        Scp610TestKit.touch(Victim, Carrier, LabContact.Strike, Hits);
        Scp610TestKit.touch(Victim, Carrier, LabContact.Touch, Hits);
        Helper.assertFalse(Scp610Infection.isInfected(Victim), "a fully suited carrier passed SCP-610 on by striking");
        Villager Struck = still(Helper, EntityType.VILLAGER, 14.5, 16.5);
        Scp610TestKit.hit(Struck, Carrier, Hits);
        Helper.assertFalse(Scp610Infection.isInfected(Struck), "a fully suited carrier's real blows passed SCP-610 on");
        for (int Cough = 0; Cough < 20; Cough++) {
            Scp610Infection.cough(Helper.getLevel(), Carrier);
        }
        Helper.assertFalse(Scp610Infection.isInfected(Victim), "a fully suited carrier passed SCP-610 on by coughing");
        Scp610TestKit.dismiss(Carrier);
        Scp610TestKit.dismiss(Victim);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_infection")
    public static void aFullSuitProtectsAndAGasMaskDoesNot(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Masked = Scp610TestKit.survivor(Helper, 20.5, Scp610TestKit.Ground, 20.5);
        ServerPlayer Suited = Scp610TestKit.survivor(Helper, 21.5, Scp610TestKit.Ground, 20.5);
        Scp610TestKit.mask(Masked);
        Scp610TestKit.suit(Suited);
        Vec3 Cloud = Helper.absoluteVec(new Vec3(21.0, Scp610TestKit.Ground + 1.0, 21.5));
        Scp610Infection.exposeAround(World, Cloud, LabExposure.NothingIgnored, 4.0);
        Helper.assertTrue(Scp610Infection.isInfected(Masked), "a gas mask alone kept SCP-610 out of a cloud");
        Helper.assertFalse(Scp610Infection.isInfected(Suited), "a full suit let a cloud through");
        ServerPlayer Carrier = Scp610TestKit.survivor(Helper, 22.5, Scp610TestKit.Ground, 20.5);
        Scp610Infection.infect(Carrier);
        Scp610Infection.setStage(Carrier, 4);
        Scp610TestKit.touch(Suited, Carrier, LabContact.Strike, Hits);
        Scp610TestKit.touch(Suited, Carrier, LabContact.Touch, Hits);
        Helper.assertFalse(Scp610Infection.isInfected(Suited), "a full suit let an infected player's blows through");
        ServerPlayer Bare = Scp610TestKit.survivor(Helper, 23.5, Scp610TestKit.Ground, 21.5);
        Scp610TestKit.touch(Bare, Carrier, LabContact.Strike, Hits);
        Helper.assertTrue(Scp610Infection.isInfected(Bare), "a bare player beside the suited one was never infected");
        Scp610TestKit.dismiss(Bare);
        Scp610Registry.organism().expose(World, Cloud, LabExposure.NothingIgnored, 4.0);
        Helper.assertFalse(Scp610Infection.isInfected(Suited), "a full suit let an organism release through");
        Scp610TestKit.dismiss(Masked);
        Scp610TestKit.dismiss(Suited);
        Scp610TestKit.dismiss(Carrier);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_infection")
    public static void deathByTheFleshRaisesAnApostle(GameTestHelper Helper) {
        ServerPlayer User = Scp610TestKit.survivor(Helper, 26.5, Scp610TestKit.Ground, 6.5);
        Scp610Infection.infect(User);
        Scp610Infection.setStage(User, 4);
        Vec3 At = User.position();
        Scp610Infection.finish(User);
        List<Scp610Apostle> Risen = Helper.getLevel().getEntitiesOfClass(Scp610Apostle.class, new AABB(At, At).inflate(4.0));
        Helper.assertTrue(Risen.size() == 1, "expected one apostle where the player fell, found " + Risen.size());
        Helper.assertFalse(Scp610Infection.isInfected(User), "the infection outlived its host");
        Scp610TestKit.dismiss(User);
        Scp610TestKit.clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_infection")
    public static void mobsTurnIntoTheirVariantsOrAGenericForm(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Villager Host = still(Helper, EntityType.VILLAGER, 4.5, 26.5);
        Helper.assertTrue(Scp610Infection.infect(Host), "a villager could not be infected");
        Scp610Infection.setElapsed(Host, Scp610Infection.MobConvertTicks - 1);
        Vec3 At = Host.position();
        Scp610Infection.tick(Host);
        Helper.assertTrue(Host.isRemoved(), "the villager was not consumed");
        List<Scp610Creature> Risen = World.getEntitiesOfClass(Scp610Creature.class, new AABB(At, At).inflate(3.0));
        Helper.assertTrue(Risen.size() == 1 && Risen.get(0).form() == Scp610Form.Villager, "the villager did not become the villager variant");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.PIG, 8.5, 26.5)) == Scp610Form.Pig, "a pig does not map to the pig variant");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.COW, 10.5, 26.5)) == Scp610Form.Cow, "a cow does not map to the cow variant");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.SHEEP, 12.5, 26.5)) == Scp610Form.Sheep, "a sheep does not map to the sheep variant");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.CHICKEN, 14.5, 26.5)) == Scp610Form.Chicken, "a chicken does not map to the chicken variant");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.HORSE, 16.5, 26.5)) == Scp610Form.Horse, "a horse does not map to the horse variant");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.RABBIT, 18.5, 26.5)) == Scp610Form.Crawler, "a rabbit does not fall back to the small form");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.ZOMBIE, 20.5, 26.5)) == Scp610Form.Shambler, "a zombie does not fall back to the medium form");
        Helper.assertTrue(Scp610Hosts.formFor(still(Helper, EntityType.POLAR_BEAR, 23.5, 26.5)) == Scp610Form.Hulk, "a polar bear does not fall back to the large form");
        Mob Fox = still(Helper, EntityType.FOX, 26.5, 26.5);
        Helper.assertTrue(Scp610Infection.infect(Fox), "a fox could not be infected");
        Vec3 FoxAt = Fox.position();
        Scp610Hosts.convert(World, Fox);
        List<Scp610Creature> Small = World.getEntitiesOfClass(Scp610Creature.class, new AABB(FoxAt, FoxAt).inflate(2.0));
        Helper.assertTrue(Small.size() == 1 && Small.get(0).form() == Scp610Form.Crawler, "a fox did not become the small generic form");
        Mob Golem = still(Helper, EntityType.IRON_GOLEM, 28.5, 20.5);
        Helper.assertFalse(Scp610Infection.infect(Golem), "an iron golem caught SCP-610");
        Scp610TestKit.clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_infection")
    public static void theCureOnlyWorksEarly(GameTestHelper Helper) {
        Villager Early = still(Helper, EntityType.VILLAGER, 4.5, 12.5);
        Scp610Infection.infect(Early);
        Helper.assertTrue(AnomalyCure.cure(Early, null), "a cure did nothing for a fresh infection");
        Helper.assertFalse(Scp610Infection.isInfected(Early), "a fresh infection survived the cure");
        ServerPlayer Late = Scp610TestKit.survivor(Helper, 6.5, Scp610TestKit.Ground, 12.5);
        Scp610Infection.infect(Late);
        Scp610Infection.setStage(Late, 3);
        AnomalyCure.cure(Late, null);
        Helper.assertTrue(Scp610Infection.isInfected(Late) && Scp610Infection.stageOf(Late) == 2, "a late infection was not knocked back exactly one stage");
        Scp610TestKit.dismiss(Late);
        Helper.succeed();
    }

    public static boolean infected(LivingEntity Subject) {
        return Scp610Infection.isInfected(Subject);
    }
}

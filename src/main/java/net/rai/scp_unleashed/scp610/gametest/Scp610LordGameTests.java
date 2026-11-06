package net.rai.scp_unleashed.scp610.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.scp610.PheromoneCommunicatorBlock;
import net.rai.scp_unleashed.scp610.PheromoneCommunicatorBlockEntity;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.scp610.Scp610Config;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Dialogue;
import net.rai.scp_unleashed.scp610.Scp610Faction;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Founding;
import net.rai.scp_unleashed.scp610.Scp610Growth;
import net.rai.scp_unleashed.scp610.Scp610HiveLord;
import net.rai.scp_unleashed.scp610.Scp610Hives;
import net.rai.scp_unleashed.scp610.Scp610Infection;
import net.rai.scp_unleashed.scp610.Scp610Lord;
import net.rai.scp_unleashed.scp610.Scp610Pact;
import net.rai.scp_unleashed.scp610.Scp610Registry;
import net.rai.scp_unleashed.scp610.Scp610Rules;
import net.rai.scp_unleashed.scp610.Scp610Spawning;
import net.rai.scp_unleashed.scp610.Scp610Spewer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp610LordGameTests {
    public static final int Depth = 14;

    private Scp610LordGameTests() {
    }

    private static FactionState state(GameTestHelper Helper) {
        return Scp610Hives.state(Helper.getLevel().getServer());
    }

    private static Scp610HiveLord formed(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        Scp610Config.overrideDepthForTests(Depth);
        ServerLevel World = Helper.getLevel();
        Scp610Spewer First = Scp610Founding.found(World, Scp610TestKit.ground(Helper, 16, 16), 0.0f);
        Scp610Spewer Second = Scp610Founding.found(World, Scp610TestKit.ground(Helper, 28, 28), 0.0f);
        Helper.assertTrue(First != null && Second != null, "the hives could not be founded");
        FactionState State = state(Helper);
        Scp610Faction.Instance.tick(World.getServer(), State);
        Helper.assertTrue(State.phase() == FactionPhase.Ascendant, "two hives did not lift the faction to its second stage");
        Helper.assertTrue(Scp610Lord.planned(State), "the deep chamber was not planned at the second stage");
        Scp610Lord.formNow(World, State);
        Scp610HiveLord Lord = Scp610Lord.lord(World.getServer(), State);
        Helper.assertTrue(Lord != null, "the deep chamber formed no eye");
        return Lord;
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_lord_form")
    public static void theLordChamberFormsOncePerFaction(GameTestHelper Helper) {
        Scp610HiveLord Lord = formed(Helper);
        ServerLevel World = Helper.getLevel();
        FactionState State = state(Helper);
        int Site = Scp610Lord.data(State).getInt("Site");
        Helper.assertTrue(Site == Scp610Hives.list(World.getServer()).get(0).id(), "the deep chamber is not under the oldest hive");
        Scp610Faction.Instance.tick(World.getServer(), State);
        Helper.assertFalse(Scp610Lord.plan(World, State), "a second deep chamber was planned");
        Helper.assertTrue(Scp610Lord.data(State).getInt("Site") == Site, "the deep chamber moved");
        List<Scp610HiveLord> Eyes = World.getEntitiesOfClass(Scp610HiveLord.class, Helper.getBounds().inflate(32.0));
        Helper.assertTrue(Eyes.size() == 1, "expected one eye, found " + Eyes.size());
        Helper.assertTrue(Scp610Lord.bounds(State).contains(Lord.position().add(0.0, Lord.getBbHeight() * 0.5, 0.0)), "the eye is not inside its chamber");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_lord_comm")
    public static void theCommunicatorOnlyWorksInTheChamber(GameTestHelper Helper) {
        formed(Helper);
        ServerLevel World = Helper.getLevel();
        FactionState State = state(Helper);
        BlockPos Centre = BlockPos.of(Scp610Lord.data(State).getLong("Centre"));
        BlockPos Inside = Centre;
        BlockPos Beside = Centre.east();
        BlockPos Outside = Scp610TestKit.ground(Helper, 4, 4);
        World.setBlock(Inside, Scp610Registry.Communicator.get().defaultBlockState(), 3);
        World.setBlock(Beside, Scp610Registry.Communicator.get().defaultBlockState(), 3);
        World.setBlock(Outside, Scp610Registry.Communicator.get().defaultBlockState(), 3);
        Helper.assertFalse(World.getBlockEntity(Inside) instanceof WireDevice, "the communicator still draws power");
        Helper.assertTrue(PheromoneCommunicatorBlock.listening(World, Inside), "an unpowered communicator in the chamber was not heard");
        Helper.assertTrue(PheromoneCommunicatorBlock.listening(World, Beside), "a second communicator in the chamber was not heard");
        Helper.assertFalse(PheromoneCommunicatorBlock.listening(World, Outside), "a communicator outside the chamber was heard");
        ((PheromoneCommunicatorBlockEntity) World.getBlockEntity(Inside)).sense(World, Inside, World.getBlockState(Inside));
        ((PheromoneCommunicatorBlockEntity) World.getBlockEntity(Outside)).sense(World, Outside, World.getBlockState(Outside));
        Helper.assertTrue(World.getBlockState(Inside).getValue(PheromoneCommunicatorBlock.Active), "the communicator in the chamber did not wake");
        Helper.assertFalse(World.getBlockState(Outside).getValue(PheromoneCommunicatorBlock.Active), "the communicator outside the chamber woke");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_dialogue")
    public static void theFleshSpeaksInMoodsWithoutRepeating(GameTestHelper Helper) {
        Scp610HiveLord Lord = formed(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState State = state(Helper);
        BlockPos Centre = BlockPos.of(Scp610Lord.data(State).getLong("Centre"));
        World.setBlock(Centre, Scp610Registry.Communicator.get().defaultBlockState(), 3);
        ServerPlayer Speaker = Scp610TestKit.survivor(Helper, 4.5, Scp610TestKit.Ground, 20.5);
        long Now = World.getGameTime();
        Helper.assertTrue(Scp610Dialogue.First.equals(Scp610Dialogue.mood(Speaker, State, Now)), "a stranger was not met with first contact");
        Scp610Dialogue.open(Speaker, Centre);
        Helper.assertTrue(Scp610Dialogue.Offer.equals(Scp610Dialogue.mood(Speaker, State, Now)), "a returning speaker was not offered the deal");
        UUID Listener = UUID.randomUUID();
        for (Map.Entry<String, Integer> Pool : Scp610Dialogue.Counts.entrySet()) {
            int Count = Pool.getValue();
            int Previous = -1;
            Set<Integer> Seen = new HashSet<>();
            for (int Draw = 0; Draw < Count * 3; Draw++) {
                int Pick = Scp610Dialogue.draw(Listener, Pool.getKey(), Count, Speaker.getRandom());
                Helper.assertTrue(Pick != Previous, "the line " + Pool.getKey() + "." + Pick + " repeated");
                Helper.assertTrue(Pick >= 0 && Pick < Count, "the line " + Pool.getKey() + "." + Pick + " does not exist");
                Seen.add(Pick);
                if (Draw % Count == Count - 1) {
                    Helper.assertTrue(Seen.size() == Count, "a round of " + Pool.getKey() + " skipped lines");
                    Seen.clear();
                }
                Previous = Pick;
            }
        }
        List<String> Struck = Scp610Dialogue.answer(Speaker, Centre, Scp610Dialogue.Terms);
        Helper.assertTrue(Scp610Pact.active(State), "asking the flesh to stop made no pact");
        Helper.assertTrue(Struck.size() == 2 && Struck.get(1).endsWith("terms_price|" + Scp610Pact.TributeMeat + "|3"), "the terms did not state the tribute: " + Struck);
        Helper.assertTrue(Scp610Dialogue.Held.equals(Scp610Dialogue.mood(Speaker, State, Now)), "a fresh pact was not held");
        List<String> Asked = Scp610Dialogue.answer(Speaker, Centre, Scp610Dialogue.Price);
        Helper.assertTrue(Asked.size() == 2 && Asked.get(1).contains("price_left|" + Scp610Pact.TributeMeat + "|3"), "the price was not stated: " + Asked);
        List<String> Short = Scp610Dialogue.answer(Speaker, Centre, Scp610Dialogue.Tribute);
        Helper.assertTrue(Short.get(0).contains(Scp610Dialogue.Lacking), "an empty-handed offer was taken");
        Scp610Pact.data(State).putLong("Since", Now - 100L);
        Speaker.getInventory().add(new ItemStack(Items.BEEF, Scp610Pact.TributeMeat));
        List<String> Fed = Scp610Dialogue.answer(Speaker, Centre, Scp610Dialogue.Tribute);
        Helper.assertTrue(Fed.get(0).contains(Scp610Dialogue.Taken), "a full offering was refused: " + Fed);
        Helper.assertTrue(Scp610Dialogue.Sated.equals(Scp610Dialogue.mood(Speaker, State, Now)), "a fed hive was not sated");
        Scp610Pact.data(State).putLong("NextTribute", Now + 100L);
        Helper.assertTrue(Scp610Dialogue.Due.equals(Scp610Dialogue.mood(Speaker, State, Now)), "a due tribute did not make the flesh hungry");
        Scp610Pact.breakPact(Server, State, Scp610Pact.Breach.Tribute);
        Helper.assertTrue(Scp610Dialogue.Broken.equals(Scp610Dialogue.mood(Speaker, State, Now)), "a broken pact did not change the mood");
        List<String> Sulk = Scp610Dialogue.answer(Speaker, Centre, Scp610Dialogue.Terms);
        Helper.assertTrue(Sulk.get(0).contains(Scp610Dialogue.Refused) && !Scp610Pact.active(State), "a fresh breach did not refuse a new pact");
        Scp610Pact.data(State).putLong("BrokenAt", Now - Scp610Dialogue.SulkTicks - 1L);
        Scp610Dialogue.answer(Speaker, Centre, Scp610Dialogue.Terms);
        Helper.assertTrue(Scp610Pact.active(State), "the flesh never bargained again after a breach");
        Lord.invulnerableTime = 0;
        Lord.hurt(World.damageSources().magic(), 2.0f);
        Helper.assertTrue(Scp610Dialogue.Hurt.equals(Scp610Dialogue.mood(Speaker, State, Now)), "a hurt eye did not change the mood");
        Scp610TestKit.dismiss(Speaker);
        Scp610Pact.clear(State);
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 200, batch = "scp610_lord_pact")
    public static void aDealHaltsGrowthButNotHostility(GameTestHelper Helper) {
        formed(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState State = state(Helper);
        ServerPlayer Signer = Scp610TestKit.survivor(Helper, 4.5, Scp610TestKit.Ground, 20.5);
        Helper.assertTrue(Scp610Pact.make(State, Signer, World.getGameTime()), "the deal could not be made");
        Helper.assertTrue(Scp610Growth.halted(Server, State), "growth is not halted under the deal");
        Helper.assertTrue(Scp610Founding.allowed(World, Scp610TestKit.ground(Helper, 4, 28)) == Scp610Founding.Verdict.Pact, "new hives may still be founded under the deal");
        int Before = Scp610Hives.totalFlesh(Server);
        Scp610Creature Unit = Scp610Spawning.raise(World, Scp610Form.Shambler, Helper.absoluteVec(new Vec3(6.5, Scp610TestKit.Ground, 20.5)), 0.0f, false);
        Helper.assertTrue(Unit != null && Scp610Creature.isQuarry(Unit, Signer), "the flesh stopped hunting the signer");
        Villager Near = Helper.spawn(EntityType.VILLAGER, new Vec3(8.5, Scp610TestKit.Ground, 20.5));
        Near.setNoAi(true);
        Scp610Infection.exposeAround(World, Near.position().add(0.0, 1.0, 0.0), LabExposure.NothingIgnored, 2.0);
        Helper.assertTrue(Scp610Infection.isInfected(Near), "the deal stopped the infection");
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Scp610Hives.totalFlesh(Server) == Before, "the flesh grew under the deal");
            Scp610TestKit.dismiss(Signer);
            Scp610Pact.clear(State);
            Scp610TestKit.restore(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 400, batch = "scp610_lord_break")
    public static void breakingTheDealResumesGrowth(GameTestHelper Helper) {
        Scp610HiveLord Lord = formed(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState State = state(Helper);
        ServerPlayer Signer = Scp610TestKit.survivor(Helper, 4.5, Scp610TestKit.Ground, 20.5);
        Scp610Pact.make(State, Signer, World.getGameTime());
        Lord.invulnerableTime = 0;
        Lord.hurt(World.damageSources().playerAttack(Signer), 4.0f);
        Helper.assertFalse(Scp610Pact.active(State), "striking the eye did not break the deal");
        Helper.assertTrue(Scp610Pact.surgeScale(State, World.getGameTime()) > 1.0, "breaking the deal brought no surge");
        Helper.assertFalse(Scp610Growth.halted(Server, State), "growth is still halted after the deal broke");
        int Before = Scp610Hives.totalFlesh(Server);
        Helper.runAfterDelay(300, () -> {
            Helper.assertTrue(Scp610Hives.totalFlesh(Server) > Before, "the flesh did not resume growing");
            Scp610TestKit.dismiss(Signer);
            Scp610Pact.clear(State);
            Scp610TestKit.restore(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_lord_persist")
    public static void theDealSurvivesASaveAndLoad(GameTestHelper Helper) {
        formed(Helper);
        ServerLevel World = Helper.getLevel();
        FactionState State = state(Helper);
        ServerPlayer Signer = Scp610TestKit.survivor(Helper, 4.5, Scp610TestKit.Ground, 20.5);
        Scp610Pact.make(State, Signer, World.getGameTime());
        Scp610Hives.flush(World.getServer());
        CompoundTag Saved = State.save();
        FactionState Loaded = FactionState.load(Saved);
        Helper.assertTrue(Loaded != null && Scp610Pact.active(Loaded), "the deal was lost on reload");
        Helper.assertTrue(Signer.getUUID().equals(Scp610Pact.signer(Loaded)), "the deal's signer was lost on reload");
        Helper.assertTrue(Scp610Pact.nextTribute(Loaded) == Scp610Pact.nextTribute(State), "the tribute date changed on reload");
        Helper.assertTrue(Scp610Lord.formed(Loaded) && Scp610Lord.uuid(Loaded) != null, "the eye's record was lost on reload");
        Scp610TestKit.dismiss(Signer);
        Scp610Pact.clear(State);
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    public static boolean rule(MinecraftServer Server) {
        return Scp610Rules.spreads(Server);
    }
}

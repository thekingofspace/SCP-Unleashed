package net.rai.scp_unleashed.radio.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.radio.HandheldRadioItem;
import net.rai.scp_unleashed.radio.RadioBleed;
import net.rai.scp_unleashed.radio.RadioBleedKind;
import net.rai.scp_unleashed.radio.RadioCarry;
import net.rai.scp_unleashed.radio.RadioMastBlock;
import net.rai.scp_unleashed.radio.RadioNetwork;
import net.rai.scp_unleashed.radio.RadioRegistry;
import net.rai.scp_unleashed.radio.RadioRelay;
import net.rai.scp_unleashed.radio.RadioTowerBlockEntity;
import net.rai.scp_unleashed.radio.RadioTowerData;
import net.rai.scp_unleashed.radio.RadioTowerStatus;
import net.rai.scp_unleashed.radio.VoiceConfig;
import net.rai.scp_unleashed.scp079.Scp079Block;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079RadioBleed;
import net.rai.scp_unleashed.scp079.Scp079Registry;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class RadioGameTests {
    public static final String Arena = "radio_arena";
    public static final RadioNetwork.Ranges Limits = new RadioNetwork.Ranges(64, 64, 32, 128, 64);

    private RadioGameTests() {
    }

    private static RadioTestPlayer player(GameTestHelper Helper, Vec3 Relative, String Name) {
        ServerLevel World = Helper.getLevel();
        RadioTestPlayer User = new RadioTestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Relative);
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, List<? extends ServerPlayer> Users) {
        for (ServerPlayer User : Users) {
            RadioBleed.forgetPlayer(User.getUUID());
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static RadioTowerData.Tower tower(BlockPos Pos, int Masts, boolean Powered, int Channel, List<RadioBleedKind> Sources) {
        return new RadioTowerData.Tower(Pos, Masts, Powered, Channel, Sources);
    }

    private static List<RadioNetwork.Node> nodes(RadioTowerData.Tower... Towers) {
        return RadioNetwork.nodes(Limits, List.of(Towers));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "radio_rangeDirect")
    public static void rangeDirect(GameTestHelper Helper) {
        Vec3 From = new Vec3(1000.5, 70.0, 1000.5);
        Helper.assertTrue(RadioNetwork.strength(Limits, List.of(), From, From.add(40.0, 0.0, 0.0)) > 0.3, "Radios 40 blocks apart should hear each other directly");
        Helper.assertTrue(RadioNetwork.strength(Limits, List.of(), From, From.add(0.0, 30.0, 50.0)) >= 0.0, "Direct range is measured in three dimensions");
        Helper.assertTrue(RadioNetwork.strength(Limits, List.of(), From, From.add(70.0, 0.0, 0.0)) < 0.0, "Radios 70 blocks apart must not reach each other alone");
        double Edge = RadioNetwork.strength(Limits, List.of(), From, From.add(63.0, 0.0, 0.0));
        Helper.assertTrue(Edge >= 0.0 && Edge < 0.05, "A radio at the edge of its range should be very weak, got " + Edge);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "radio_rangeTower")
    public static void rangeTower(GameTestHelper Helper) {
        BlockPos Mast = new BlockPos(2000, 70, 2000);
        RadioTowerData.Tower One = tower(Mast, 1, true, 1, List.of());
        Vec3 Antenna = RadioNetwork.antenna(Mast, 1);
        Vec3 West = Antenna.add(-90.0, 0.0, 0.0);
        Vec3 East = Antenna.add(90.0, 0.0, 0.0);
        Helper.assertTrue(RadioNetwork.strength(Limits, List.of(), West, East) < 0.0, "Radios 180 blocks apart must not reach each other without a tower");
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(One), West, East) >= 0.0, "A powered tower with one mast should relay radios 90 blocks either side");
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(One), West, Antenna.add(110.0, 0.0, 0.0)) < 0.0, "A one mast tower must not reach a radio 110 blocks away");
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(tower(Mast, 4, true, 1, List.of())), West, Antenna.add(180.0, 0.0, 0.0)) >= 0.0,
                "A four mast tower should reach a radio 180 blocks away");
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(tower(Mast, 1, false, 1, List.of())), West, East) < 0.0, "An unpowered tower must not relay");
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(tower(Mast, 0, true, 1, List.of())), West, East) < 0.0, "A tower without a mast must not relay");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "radio_rangeChain")
    public static void rangeChain(GameTestHelper Helper) {
        BlockPos First = new BlockPos(3000, 70, 3000);
        RadioTowerData.Tower A = tower(First, 1, true, 1, List.of());
        RadioTowerData.Tower B = tower(First.offset(180, 0, 0), 1, true, 1, List.of());
        RadioTowerData.Tower C = tower(First.offset(360, 0, 0), 1, true, 1, List.of());
        Vec3 Start = RadioNetwork.antenna(First, 1).add(-80.0, 0.0, 0.0);
        Vec3 End = RadioNetwork.antenna(First, 1).add(440.0, 0.0, 0.0);
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(A, B, C), Start, End) >= 0.0, "A chain of three powered towers should carry a radio 520 blocks");
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(A, C), Start, End) < 0.0, "Removing the middle tower must break the chain");
        RadioTowerData.Tower Dark = tower(B.Pos(), 1, false, 1, List.of());
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(A, Dark, C), Start, End) < 0.0, "An unpowered middle tower must break the chain");
        RadioTowerData.Tower Tall = tower(First, 4, true, 1, List.of());
        RadioTowerData.Tower Short = tower(First.offset(300, 0, 0), 1, true, 1, List.of());
        Vec3 Near = RadioNetwork.antenna(First, 4).add(-50.0, 0.0, 0.0);
        Vec3 Far = RadioNetwork.antenna(Short.Pos(), 1).add(50.0, 0.0, 0.0);
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(Tall, Short), Near, Far) < 0.0, "Towers link only within the shorter of their two link ranges");
        RadioTowerData.Tower Taller = tower(Short.Pos(), 3, true, 1, List.of());
        Helper.assertTrue(RadioNetwork.strength(Limits, nodes(Tall, Taller), Near, Far) >= 0.0, "Two tall towers 300 blocks apart should link");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "radio_powerLoss")
    public static void powerLoss(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Base = new BlockPos(8, 1, 8);
        Helper.setBlock(Base, RadioRegistry.Tower.get().defaultBlockState());
        Helper.setBlock(Base.above(), RadioRegistry.Mast.get().defaultBlockState());
        BlockPos Absolute = Helper.absolutePos(Base);
        RadioTowerBlockEntity Tower = (RadioTowerBlockEntity) World.getBlockEntity(Absolute);
        Helper.assertTrue(Tower != null, "The booster tower has no block entity");
        RadioNetwork.Ranges Live = VoiceConfig.ranges();
        Vec3 Antenna = RadioNetwork.antenna(Absolute, 1);
        double Reach = Live.coverage(1) - 6.0;
        Vec3 West = Antenna.add(-Reach, 0.0, 0.0);
        Vec3 East = Antenna.add(Reach, 0.0, 0.0);
        boolean[] Feeding = {true};
        Helper.onEachTick(() -> {
            if (Feeding[0]) {
                Tower.insertPower(Math.max(1, Tower.getPowerDemand()));
            }
        });
        Helper.runAtTickTime(30, () -> {
            Helper.assertTrue(Tower.isPowered(), "A fed tower with one mast should be on the air");
            Helper.assertTrue(Tower.status() == RadioTowerStatus.Online, "The tower status should read online");
            Helper.assertTrue(Tower.getPowerDemand() == VoiceConfig.demand(1), "A one mast tower should draw " + VoiceConfig.demand(1) + " SP/t, got " + Tower.getPowerDemand());
            RadioTowerData.Tower Record = RadioTowerData.get(World).find(Absolute);
            Helper.assertTrue(Record != null && Record.relays(), "The powered tower is not in the relay registry");
            Helper.assertTrue(RadioNetwork.strength(Live, RadioTowerData.nodes(World, Live), West, East) >= 0.0, "Radios on either side of the powered tower should connect");
            BlockState Lamp = World.getBlockState(Absolute.above());
            Helper.assertTrue(Lamp.getValue(RadioMastBlock.Top) && Lamp.getValue(RadioMastBlock.Lit), "The mast lamp should be lit on a powered tower");
            Feeding[0] = false;
        });
        Helper.runAtTickTime(80, () -> {
            Helper.assertFalse(Tower.isPowered(), "A tower that lost power should go off the air after its grace period");
            Helper.assertTrue(RadioNetwork.strength(Live, RadioTowerData.nodes(World, Live), West, East) < 0.0, "The link must break when the tower loses power");
            Helper.assertFalse(World.getBlockState(Absolute.above()).getValue(RadioMastBlock.Lit), "The mast lamp should go dark without power");
            Helper.setBlock(Base.above(), Blocks.AIR.defaultBlockState());
            Helper.setBlock(Base, Blocks.AIR.defaultBlockState());
            Helper.assertTrue(RadioTowerData.get(World).find(Absolute) == null, "Breaking the tower must remove it from the registry");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "radio_textFallback")
    public static void textFallback(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        RadioTestPlayer Sender = player(Helper, new Vec3(3.5, 1.0, 3.5), "radio_sender");
        RadioTestPlayer Same = player(Helper, new Vec3(13.5, 1.0, 3.5), "radio_same");
        RadioTestPlayer Other = player(Helper, new Vec3(3.5, 1.0, 13.5), "radio_other");
        RadioTestPlayer Distant = player(Helper, new Vec3(3.5 + 120.0, 1.0, 3.5), "radio_distant");
        RadioTestPlayer Silent = player(Helper, new Vec3(8.5, 1.0, 8.5), "radio_silent");
        RadioTestPlayer Bare = player(Helper, new Vec3(9.5, 1.0, 4.5), "radio_bare");
        List<RadioTestPlayer> Everyone = List.of(Sender, Same, Other, Distant, Silent, Bare);
        try {
            Sender.setItemInHand(InteractionHand.MAIN_HAND, RadioCarry.configured(3, true));
            Same.getInventory().setItem(4, RadioCarry.configured(3, true));
            Other.getInventory().setItem(0, RadioCarry.configured(4, true));
            Distant.setItemInHand(InteractionHand.OFF_HAND, RadioCarry.configured(3, true));
            Silent.getInventory().setItem(1, RadioCarry.configured(3, false));
            List<RadioRelay.Hearer> Heard = RadioRelay.hearers(World, Sender, RadioRelay.mouth(Sender), 3, Limits, List.of());
            Helper.assertTrue(Heard.size() == 1 && Heard.get(0).Listener() == Same, "Only the same channel radio in range should hear, got " + names(Heard));
            boolean Cancelled = NeoForge.EVENT_BUS.post(new ServerChatEvent(Sender, "!hello team", Component.literal("!hello team"))).isCanceled();
            Helper.assertTrue(Cancelled, "Radio chat must be taken out of public chat");
            Helper.assertTrue(Same.heard("hello team"), "The same channel listener did not get the radio message");
            Helper.assertFalse(Other.heard("hello team"), "A listener on another channel got the radio message");
            Helper.assertFalse(Distant.heard("hello team"), "A listener out of range got the radio message");
            Helper.assertFalse(Silent.heard("hello team"), "A switched off radio got the radio message");
            Helper.assertFalse(Bare.heard("hello team"), "A player without a radio got the radio message");
            Helper.assertTrue(Sender.heard("hello team"), "The sender should see their own radio message");
            boolean Public = NeoForge.EVENT_BUS.post(new ServerChatEvent(Sender, "plain words", Component.literal("plain words"))).isCanceled();
            Helper.assertFalse(Public, "Chat without the prefix must stay public");
            Bare.setItemInHand(InteractionHand.MAIN_HAND, RadioCarry.configured(3, false));
            boolean OffRadio = NeoForge.EVENT_BUS.post(new ServerChatEvent(Bare, "!not sent", Component.literal("!not sent"))).isCanceled();
            Helper.assertFalse(OffRadio, "A switched off radio must not take chat");
            Helper.assertTrue(RadioCarry.wrapChannel(RadioCarry.MaxChannel + 1) == RadioCarry.MinChannel && RadioCarry.wrapChannel(RadioCarry.MinChannel - 1) == RadioCarry.MaxChannel,
                    "Channel tuning must wrap around");
            HandheldRadioItem.tune(Sender, Sender.getMainHandItem(), 1);
            Helper.assertTrue(RadioCarry.channel(Sender.getMainHandItem()) == 4, "Tuning up should move the radio to channel 4");
        } finally {
            dismiss(Helper, Everyone);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "radio_memeticBroadcast")
    public static void memeticBroadcast(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Base = Helper.absolutePos(new BlockPos(8, 1, 8));
        RadioTowerData.Tower Source = tower(Base, 1, true, 7, List.of(RadioBleedKind.Clank));
        RadioTestPlayer Tuned = player(Helper, new Vec3(12.5, 1.0, 8.5), "radio_tuned");
        RadioTestPlayer Untuned = player(Helper, new Vec3(4.5, 1.0, 8.5), "radio_untuned");
        RadioTestPlayer Off = player(Helper, new Vec3(8.5, 1.0, 12.5), "radio_off");
        RadioTestPlayer Far = player(Helper, new Vec3(8.5 + 130.0, 1.0, 8.5), "radio_far");
        List<RadioTestPlayer> Everyone = List.of(Tuned, Untuned, Off, Far);
        try {
            for (RadioTestPlayer Each : Everyone) {
                RadioBleed.forgetPlayer(Each.getUUID());
            }
            Tuned.getInventory().setItem(0, RadioCarry.configured(7, true));
            Untuned.getInventory().setItem(0, RadioCarry.configured(8, true));
            Off.getInventory().setItem(0, RadioCarry.configured(7, false));
            Far.getInventory().setItem(0, RadioCarry.configured(7, true));
            List<RadioRelay.Hearer> Reached = RadioBleed.pulse(World, Source, RadioBleedKind.Clank, Limits, nodes(Source));
            Helper.assertTrue(Reached.size() == 1 && Reached.get(0).Listener() == Tuned, "Only the tuned listener in range should be reached, got " + names(Reached));
            Helper.assertTrue(Tuned.hasEffect(MobEffects.DARKNESS), "The tuned listener did not get the mild effect");
            Helper.assertFalse(Untuned.hasEffect(MobEffects.DARKNESS) || Off.hasEffect(MobEffects.DARKNESS) || Far.hasEffect(MobEffects.DARKNESS),
                    "A listener who was not tuned and in range got the effect");
            Helper.assertTrue(RadioBleed.isHaunted(Tuned.getUUID(), World.getGameTime()), "The tuned listener's channel should be distorted after the broadcast");
            Helper.assertTrue(RadioBleed.pulse(World, Source, RadioBleedKind.Refrain, Limits, nodes(Source)).isEmpty(), "The listener cooldown did not hold");
            Helper.assertFalse(Tuned.hasEffect(MobEffects.CONFUSION), "A second broadcast inside the cooldown applied its effect");
        } finally {
            dismiss(Helper, Everyone);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "radio_machineBroadcast")
    public static void machineBroadcast(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Origin = Helper.absolutePos(new BlockPos(5, 1, 5));
        BlockState Master = Scp079Registry.Computer.get().defaultBlockState().setValue(Scp079Block.Facing, Direction.NORTH).setValue(Scp079Block.Part, Scp079Block.MasterPart);
        World.setBlock(Origin, Master, 3);
        World.setBlock(Scp079Block.cellPos(Origin, Direction.NORTH, Scp079Block.ScreenPart), Master.setValue(Scp079Block.Part, Scp079Block.ScreenPart), 3);
        Scp079BlockEntity Computer = (Scp079BlockEntity) World.getBlockEntity(Origin);
        Helper.assertTrue(Computer != null, "The SCP-079 computer has no block entity");
        RadioTestPlayer Tuned = player(Helper, new Vec3(12.5, 1.0, 12.5), "radio_machine_tuned");
        RadioTestPlayer Untuned = player(Helper, new Vec3(12.5 + 130.0, 1.0, 3.5), "radio_machine_far");
        RadioTestPlayer Bare = player(Helper, new Vec3(10.5, 1.0, 3.5), "radio_machine_bare");
        List<RadioTestPlayer> Everyone = List.of(Tuned, Untuned, Bare);
        BlockPos TowerPos = Origin.offset(0, 0, 2);
        RadioTowerData Records = RadioTowerData.get(World);
        try {
            for (RadioTestPlayer Each : Everyone) {
                RadioBleed.forgetPlayer(Each.getUUID());
            }
            Tuned.getInventory().setItem(0, RadioCarry.configured(5, true));
            Untuned.getInventory().setItem(0, RadioCarry.configured(6, true));
            Bare.getInventory().setItem(0, RadioCarry.configured(6, false));
            Helper.assertTrue(Scp079RadioBleed.attempt(World, Computer, World.getRandom(), RadioBleedKind.Corrupt, null) == null, "A plain SCP-079 must never touch the radios");
            Records.put(tower(TowerPos, 1, true, 1, List.of()));
            Helper.assertTrue(Scp079RadioBleed.attempt(World, Computer, World.getRandom(), RadioBleedKind.Corrupt, null) == null, "SCP-079 without its upgrade broadcast over a tower");
            Records.remove(TowerPos);
            Helper.assertTrue(Computer.installUpgrade(), "The upgrade could not be installed");
            Helper.assertTrue(Scp079RadioBleed.attempt(World, Computer, World.getRandom(), RadioBleedKind.Corrupt, null) == null, "An upgraded SCP-079 with no tower broadcast anyway");
            Records.put(tower(TowerPos.offset(0, 0, 4), 1, true, 1, List.of()));
            Helper.assertTrue(Scp079RadioBleed.attempt(World, Computer, World.getRandom(), RadioBleedKind.Corrupt, null) == null, "A tower out of reach of SCP-079 connected it");
            Records.remove(TowerPos.offset(0, 0, 4));
            Records.put(tower(TowerPos, 1, false, 1, List.of()));
            Helper.assertTrue(Scp079RadioBleed.attempt(World, Computer, World.getRandom(), RadioBleedKind.Corrupt, null) == null, "An unpowered tower connected SCP-079");
            Records.put(tower(TowerPos, 1, true, 1, List.of()));
            Scp079RadioBleed.Result Done = Scp079RadioBleed.attempt(World, Computer, World.getRandom(), RadioBleedKind.Corrupt, null);
            Helper.assertTrue(Done != null, "An upgraded and connected SCP-079 did not broadcast");
            Helper.assertTrue(Done.Channel() == 5, "SCP-079 should pick the channel that has a listener, picked " + Done.Channel());
            Helper.assertTrue(Done.Reached().size() == 1 && Done.Reached().get(0).Listener() == Tuned, "Only the tuned listener should be reached, got " + names(Done.Reached()));
            Helper.assertTrue(Tuned.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The corrupted transmission did not apply its mild effect");
            Helper.assertTrue(RadioBleed.isCorrupt(Tuned.getUUID(), World.getGameTime()), "The tuned listener's channel should be corrupted");
            Helper.assertTrue(Tuned.heard("radio.scp_unleashed.machine.line."), "The corrupted transmission did not send a line");
            Helper.assertFalse(Untuned.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || Bare.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "A listener who was not tuned and in range got the effect");
            Scp079RadioBleed.Result Again = Scp079RadioBleed.attempt(World, Computer, World.getRandom(), RadioBleedKind.Glitch, 5);
            Helper.assertTrue(Again != null && Again.Reached().isEmpty(), "The listener cooldown did not hold against SCP-079");
        } finally {
            Records.remove(TowerPos);
            Records.remove(TowerPos.offset(0, 0, 4));
            dismiss(Helper, Everyone);
            World.setBlock(Scp079Block.cellPos(Origin, Direction.NORTH, Scp079Block.ScreenPart), Blocks.AIR.defaultBlockState(), 3);
            World.setBlock(Origin, Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity Dropped : World.getEntitiesOfClass(ItemEntity.class, new AABB(Origin).inflate(4.0))) {
                Dropped.discard();
            }
        }
        Helper.succeed();
    }

    private static List<String> names(List<RadioRelay.Hearer> Hearers) {
        List<String> Names = new ArrayList<>();
        for (RadioRelay.Hearer Each : Hearers) {
            Names.add(Each.Listener().getGameProfile().getName());
        }
        return Names;
    }
}

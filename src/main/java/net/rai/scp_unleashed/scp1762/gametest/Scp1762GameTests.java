package net.rai.scp_unleashed.scp1762.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp1762.Scp1762BoxBlock;
import net.rai.scp_unleashed.scp1762.Scp1762BoxBlockEntity;
import net.rai.scp_unleashed.scp1762.Scp1762BoxItem;
import net.rai.scp_unleashed.scp1762.Scp1762Chat;
import net.rai.scp_unleashed.scp1762.Scp1762Dragon;
import net.rai.scp_unleashed.scp1762.Scp1762Kind;
import net.rai.scp_unleashed.scp1762.Scp1762NoteItem;
import net.rai.scp_unleashed.scp1762.Scp1762Registry;
import net.rai.scp_unleashed.scp1762.Scp1762Script;
import net.rai.scp_unleashed.scp1762.Scp1762Stage;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;
import net.rai.scp_unleashed.z0003.gametest.Z0003TestPlayer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1762GameTests {
    public static final String Arena = "scp1762_arena";
    public static final BlockPos Spot = new BlockPos(12, 2, 12);
    public static final String Phrase = "They are no longer here";

    private Scp1762GameTests() {
    }

    private static Scp1762BoxBlockEntity box(GameTestHelper Helper, int Stage) {
        Helper.setBlock(Spot, Scp1762Registry.Box.get().defaultBlockState());
        Scp1762BoxBlockEntity Box = (Scp1762BoxBlockEntity) Helper.getBlockEntity(Spot);
        Box.setStage(Stage);
        return Box;
    }

    private static Z0003TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = new Z0003TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, Z0003TestPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static List<Scp1762Dragon> dragons(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp1762Dragon.class, Helper.getBounds().inflate(16.0), Entity::isAlive);
    }

    private static List<ItemEntity> notes(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Helper.getBounds().inflate(16.0), Found -> Found.getItem().getItem() instanceof Scp1762NoteItem);
    }

    private static void clean(GameTestHelper Helper) {
        for (Scp1762Dragon Each : dragons(Helper)) {
            Each.discard();
        }
        for (ItemEntity Each : notes(Helper)) {
            Each.discard();
        }
    }

    private static void say(Z0003TestPlayer User, String Text) {
        NeoForge.EVENT_BUS.post(new ServerChatEvent(User, Text, Component.literal(Text)));
    }

    private static boolean open(GameTestHelper Helper) {
        return Scp1762BoxBlock.isOpen(Helper.getBlockState(Spot));
    }

    private static boolean insideBox(Scp1762BoxBlockEntity Box, Vec3 Where) {
        Vec3 Centre = Box.centre();
        return Math.abs(Where.x - Centre.x) <= 0.5 && Math.abs(Where.z - Centre.z) <= 0.5 && Where.y >= Centre.y - 0.01 && Where.y <= Centre.y + Scp1762Script.Rim;
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1762_opensStageZeroCount")
    public static void opensStageZeroCount(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Box.open(Helper.getLevel(), null);
        Helper.assertTrue(open(Helper), "The lid did not come off");
        Helper.onEachTick(() -> {
            for (Scp1762Dragon Each : dragons(Helper)) {
                if (Each.tickCount <= 1) {
                    Helper.assertTrue(insideBox(Box, Each.position()), Each.Kind + " appeared outside the box at " + Each.position());
                    Helper.assertTrue(Each.script() == Scp1762Script.Emerge, Each.Kind + " did not start its emerge clip");
                }
            }
        });
        Helper.runAfterDelay(Scp1762BoxBlockEntity.PourTicks / 2, () -> {
            int Halfway = dragons(Helper).size();
            Helper.assertTrue(Halfway > 0 && Halfway < Box.planned(), "Dragons did not pour out gradually: " + Halfway + " of " + Box.planned() + " out at the halfway mark");
        });
        Helper.runAfterDelay(Scp1762BoxBlockEntity.PourTicks + 5, () -> {
            int Count = dragons(Helper).size();
            Helper.assertTrue(Count >= Scp1762Stage.FirstMin && Count <= Scp1762Stage.FirstMax, "Stage 0 released " + Count + " dragons");
            Helper.assertTrue(Count == Box.out().size(), "The box tracks " + Box.out().size() + " dragons but " + Count + " are out");
            Set<Scp1762Kind> Seen = EnumSet.noneOf(Scp1762Kind.class);
            for (Scp1762Dragon Each : dragons(Helper)) {
                Seen.add(Each.Kind);
            }
            Helper.assertTrue(Seen.size() == Scp1762Kind.values().length, "Not every kind came out: " + Seen);
            Helper.assertTrue(notes(Helper).isEmpty(), "Stage 0 gave a note");
            clean(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1762_lockedWhileOpen")
    public static void lockedWhileOpen(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Z0003TestPlayer User = player(Helper, 14.5, 12.5, "scp1762_closer");
        Box.open(Helper.getLevel(), User);
        Helper.runAfterDelay(50, () -> {
            try {
                Box.interact(Helper.getLevel(), User);
                Box.interact(Helper.getLevel(), User);
                Helper.assertTrue(open(Helper), "The lid closed during the locked period");
                Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Open, "The box left the open phase early");
                Helper.assertTrue(Box.guarded(), "The box can be broken while dragons are out");
                BlockPos Absolute = Helper.absolutePos(Spot);
                Vec3 Opening = Box.centre().add(0.0, 0.25, 0.0);
                Helper.assertFalse(Helper.getBlockState(Spot).getCollisionShape(Helper.getLevel(), Absolute).toAabbs().stream().anyMatch(Part -> Part.move(Absolute).contains(Opening)),
                        "The open box is solid over its opening");
                Helper.assertTrue(Box.timer() >= Scp1762BoxBlockEntity.LockMin - 60 && Box.timer() <= Scp1762BoxBlockEntity.LockMax, "Lock timer out of range: " + Box.timer());
            } finally {
                dismiss(Helper, User);
                clean(Helper);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp1762_returnAndClose")
    public static void returnAndClose(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Box.open(Helper.getLevel(), null);
        Map<UUID, Vec3> Last = new HashMap<>();
        Helper.onEachTick(() -> {
            Set<UUID> Present = new HashSet<>();
            for (Scp1762Dragon Each : dragons(Helper)) {
                Present.add(Each.getUUID());
                Last.put(Each.getUUID(), Each.position());
            }
            for (Map.Entry<UUID, Vec3> Entry : List.copyOf(Last.entrySet())) {
                if (!Present.contains(Entry.getKey())) {
                    Helper.assertTrue(insideBox(Box, Entry.getValue()), "A dragon vanished outside the box at " + Entry.getValue());
                    Last.remove(Entry.getKey());
                }
            }
        });
        Helper.runAfterDelay(50, () -> Box.setTimer(1));
        Helper.runAfterDelay(56, () -> Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Returning, "The dragons did not start home when the timer ran out"));
        Helper.runAfterDelay(80, () -> Helper.assertTrue(open(Helper), "The lid closed before the dragons were home"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Closed, "Still returning: " + Box.out().size() + " out");
            Helper.assertFalse(open(Helper), "The lid did not close");
            Helper.assertTrue(dragons(Helper).isEmpty(), dragons(Helper).size() + " dragons left outside");
            Helper.assertTrue(Box.stage() == Scp1762Stage.First, "A natural return changed stage 0 to " + Box.stage());
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp1762_staysInRadius")
    public static void staysInRadius(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Z0003TestPlayer User = player(Helper, 20.5, 20.5, "scp1762_watcher");
        Box.open(Helper.getLevel(), User);
        Vec3 Centre = Box.centre();
        Helper.onEachTick(() -> {
            for (Scp1762Dragon Each : dragons(Helper)) {
                double Dx = Each.getX() - Centre.x;
                double Dz = Each.getZ() - Centre.z;
                Helper.assertTrue(Math.sqrt(Dx * Dx + Dz * Dz) <= Scp1762BoxBlockEntity.Radius + 0.6, Each.Kind + " left the radius at " + Each.position());
                Helper.assertTrue(Each.getY() <= Centre.y + Scp1762BoxBlockEntity.Ceiling + 0.6, Each.Kind + " flew too high at " + Each.position());
            }
        });
        Helper.runAfterDelay(280, () -> {
            int Count = dragons(Helper).size();
            dismiss(Helper, User);
            clean(Helper);
            Helper.assertTrue(Count >= Scp1762Stage.FirstMin, "Dragons went missing: " + Count);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 360, batch = "scp1762_drakesGrounded")
    public static void drakesGrounded(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Box.open(Helper.getLevel(), null);
        Map<UUID, Integer> Air = new HashMap<>();
        int Settle = Scp1762BoxBlockEntity.PourTicks + 40;
        Helper.onEachTick(() -> {
            for (Scp1762Dragon Each : dragons(Helper)) {
                if (Each.Kind != Scp1762Kind.Drake || Each.scripted() || Each.climbing()) {
                    Air.remove(Each.getUUID());
                    continue;
                }
                boolean Down = Each.onGround() || Each.groundGap() <= 0.05;
                int Count = Down ? 0 : Air.getOrDefault(Each.getUUID(), 0) + 1;
                Air.put(Each.getUUID(), Count);
                Helper.assertTrue(Count <= 30, "A drake has been off the ground for " + Count + " ticks at " + Each.position() + ", gap " + Each.groundGap());
            }
        });
        for (int Check : new int[] {Settle, Settle + 80, Settle + 160, Settle + 240}) {
            Helper.runAfterDelay(Check, () -> {
                int Drakes = 0;
                for (Scp1762Dragon Each : dragons(Helper)) {
                    if (Each.Kind != Scp1762Kind.Drake || Each.scripted() || Each.climbing() || Each.getDeltaMovement().y > 0.0) {
                        continue;
                    }
                    Drakes++;
                    double Gap = Each.groundGap();
                    Helper.assertTrue(Each.onGround() || Gap <= 0.05 || Each.getDeltaMovement().y < -0.05,
                            "A drake hovers " + String.format("%.3f", Gap) + " above the floor at " + Each.position());
                }
                Helper.assertTrue(Drakes > 0 || Check > Settle, "No drakes came out to check");
            });
        }
        Helper.runAfterDelay(Settle + 250, () -> {
            clean(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1762_harmless")
    public static void harmless(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Z0003TestPlayer User = player(Helper, 13.5, 13.5, "scp1762_swatter");
        Box.open(Helper.getLevel(), User);
        Helper.runAfterDelay(50, () -> {
            try {
                List<Scp1762Dragon> Out = dragons(Helper);
                Helper.assertTrue(!Out.isEmpty(), "No dragons to swat");
                for (Scp1762Dragon Each : Out) {
                    boolean Hurt = Each.hurt(Helper.getLevel().damageSources().playerAttack(User), 20.0f);
                    Helper.assertFalse(Hurt, Each.Kind + " took damage");
                    Each.hurt(Helper.getLevel().damageSources().inFire(), 20.0f);
                    Each.hurt(Helper.getLevel().damageSources().explosion(null, null), 20.0f);
                    Helper.assertTrue(Each.isAlive() && Each.getHealth() == Each.getMaxHealth(), Each.Kind + " was hurt");
                    Helper.assertTrue(Each.getTarget() == null, Each.Kind + " targeted someone");
                }
            } catch (RuntimeException Problem) {
                dismiss(Helper, User);
                clean(Helper);
                throw Problem;
            }
        });
        Helper.runAfterDelay(110, () -> {
            float Health = User.getHealth();
            dismiss(Helper, User);
            int Alive = dragons(Helper).size();
            clean(Helper);
            Helper.assertTrue(Health == User.getMaxHealth(), "A dragon hurt the player");
            Helper.assertTrue(Alive >= Scp1762Stage.FirstMin, "Dragons disappeared after being hit: " + Alive);
            Helper.succeed();
        });
    }

    private static void watchHomecoming(GameTestHelper Helper, Scp1762BoxBlockEntity Box) {
        Map<UUID, Vec3> Last = new HashMap<>();
        Helper.onEachTick(() -> {
            Set<UUID> Present = new HashSet<>();
            for (Scp1762Dragon Each : dragons(Helper)) {
                Present.add(Each.getUUID());
                Last.put(Each.getUUID(), Each.position());
            }
            for (Map.Entry<UUID, Vec3> Entry : List.copyOf(Last.entrySet())) {
                if (!Present.contains(Entry.getKey())) {
                    Helper.assertTrue(insideBox(Box, Entry.getValue()), "A dragon vanished outside the box at " + Entry.getValue());
                    Last.remove(Entry.getKey());
                }
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp1762_phraseRecalls")
    public static void phraseRecalls(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Z0003TestPlayer User = player(Helper, 16.5, 12.5, "scp1762_speaker");
        Box.open(Helper.getLevel(), User);
        int[] Before = {0};
        boolean[] Flew = {false};
        watchHomecoming(Helper, Box);
        Helper.runAfterDelay(50, () -> {
            Before[0] = dragons(Helper).size();
            say(User, "they ARE no longer here...  ");
            Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Recall, "The phrase did not bring the red smoke");
            Helper.assertTrue(dragons(Helper).size() == Before[0], "The phrase made dragons vanish on the spot");
        });
        Helper.runAfterDelay(56, () -> {
            Helper.assertTrue(dragons(Helper).size() > Before[0] / 2, "Most dragons vanished instead of flying home: " + dragons(Helper).size() + " of " + Before[0]);
            Helper.assertTrue(open(Helper), "The lid closed before the dragons were home");
        });
        Helper.onEachTick(() -> {
            if (Box.phase() == Scp1762BoxBlockEntity.Recall && dragons(Helper).stream().anyMatch(Each -> Each.homing() && Each.airborne())) {
                Flew[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Closed && !open(Helper), "The lid did not close after the dragons came home");
            Helper.assertTrue(dragons(Helper).isEmpty(), "Dragons stayed out after the smoke");
            Helper.assertTrue(Flew[0], "No dragon flew back into the box");
            Helper.assertTrue(Box.stage() == Scp1762Stage.Thanks, "The phrase moved the box to stage " + Box.stage());
            dismiss(Helper, User);
            clean(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1000, batch = "scp1762_phraseOnce")
    public static void phraseOnce(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Z0003TestPlayer User = player(Helper, 15.5, 12.5, "scp1762_repeater");
        ServerLevel World = Helper.getLevel();
        Box.open(World, User);
        Helper.startSequence()
                .thenIdle(50)
                .thenExecute(() -> {
                    say(User, Phrase);
                    say(User, Phrase);
                    say(User, Phrase);
                    Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Recall, "The phrase did not answer");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Closed, "Still recalling"))
                .thenExecute(() -> {
                    Helper.assertTrue(Box.stage() == Scp1762Stage.Thanks, "Saying the phrase three times moved the box to stage " + Box.stage());
                    say(User, Phrase);
                    Helper.assertTrue(Box.stage() == Scp1762Stage.Thanks && Box.phase() == Scp1762BoxBlockEntity.Closed, "The phrase reached a closed box");
                    Box.open(World, User);
                })
                .thenIdle(50)
                .thenExecute(() -> {
                    Helper.assertFalse(Box.listening(), "The stage 1 box still listens for the phrase");
                    say(User, Phrase);
                    Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Open, "The phrase cut the stage 1 visit short");
                    Helper.assertTrue(Box.stage() == Scp1762Stage.Thanks, "The phrase sped stage 1 along to stage " + Box.stage());
                    dismiss(Helper, User);
                    clean(Helper);
                })
                .thenSucceed();
    }

    private static void useOn(Z0003TestPlayer User, BlockPos Absolute) {
        User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(Absolute).add(0.0, 0.2, 0.0), Direction.UP, Absolute, false));
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1762_reviveNeedsPaper")
    public static void reviveNeedsPaper(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Empty);
        Z0003TestPlayer User = player(Helper, 14.5, 12.5, "scp1762_empty_handed");
        ServerLevel World = Helper.getLevel();
        try {
            Helper.assertTrue(Scp1762Chat.reviving("  Here be dragons!"), "The revival words did not match");
            say(User, "Here be dragons.");
            Helper.assertFalse(Box.armed(User.getUUID(), World.getGameTime()), "The words armed the box without paper");
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 3));
            useOn(User, Helper.absolutePos(Spot));
            Helper.assertTrue(Box.stage() == Scp1762Stage.Empty, "Paper alone revived the box");
            Helper.assertTrue(User.getMainHandItem().getCount() == 3, "Paper alone was used up");
            Helper.assertTrue(dragons(Helper).isEmpty(), "Something came out of the empty box");
        } finally {
            dismiss(Helper, User);
        }
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(dragons(Helper).isEmpty(), "Dragons came out without the words");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1762_reviveArms")
    public static void reviveArms(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Empty);
        Z0003TestPlayer User = player(Helper, 15.5, 13.5, "scp1762_folder");
        Z0003TestPlayer Other = player(Helper, 13.5, 11.5, "scp1762_onlooker");
        ServerLevel World = Helper.getLevel();
        try {
            User.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.PAPER, 2));
            say(User, "here BE dragons!!");
            long Now = World.getGameTime();
            Helper.assertTrue(Box.armed(User.getUUID(), Now), "The words with paper in hand did not arm the box");
            Helper.assertFalse(Box.armed(Other.getUUID(), Now), "The box armed for someone who said nothing");
            Helper.assertTrue(Box.armed(User.getUUID(), Now + Scp1762BoxBlockEntity.ArmTicks - 1), "The arming wore off too soon");
            Helper.assertFalse(Box.armed(User.getUUID(), Now + Scp1762BoxBlockEntity.ArmTicks + 1), "The arming never wears off");
            Helper.assertTrue(Box.stage() == Scp1762Stage.Empty && !open(Helper), "The words alone changed the box");
            Box.setArmedUntil(Now - 1);
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 2));
            useOn(User, Helper.absolutePos(Spot));
            Helper.assertTrue(Box.stage() == Scp1762Stage.Empty, "Paper revived the box after the arming wore off");
            Helper.assertTrue(User.getMainHandItem().getCount() == 2, "Paper was used after the arming wore off");
        } finally {
            dismiss(Helper, User);
            dismiss(Helper, Other);
        }
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(dragons(Helper).isEmpty(), "Dragons came out after the arming wore off");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1762_revives")
    public static void revives(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Empty);
        Z0003TestPlayer User = player(Helper, 16.5, 12.5, "scp1762_believer");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 3));
        say(User, "Here be dragons");
        useOn(User, Helper.absolutePos(Spot));
        Helper.assertTrue(Box.stage() == Scp1762Stage.First, "Paper after the words did not revive the box: stage " + Box.stage());
        Helper.assertTrue(open(Helper) && Box.phase() == Scp1762BoxBlockEntity.Open, "The lid did not open by itself");
        Helper.assertTrue(User.getMainHandItem().getCount() == 2, "The revival used " + (3 - User.getMainHandItem().getCount()) + " paper");
        Helper.assertFalse(Box.armed(User.getUUID(), Helper.getLevel().getGameTime()), "The box stayed armed after reviving");
        Helper.runAfterDelay(Scp1762BoxBlockEntity.PourTicks + 5, () -> {
            int Count = dragons(Helper).size();
            boolean Listens = Box.listening();
            dismiss(Helper, User);
            clean(Helper);
            Helper.assertTrue(Count >= Scp1762Stage.FirstMin && Count <= Scp1762Stage.FirstMax, "The revived box released " + Count + " dragons");
            Helper.assertTrue(Listens, "The revived box does not listen for the peace phrase");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp1762_reviveOnlyEmpty")
    public static void reviveOnlyEmpty(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Goodbye);
        Z0003TestPlayer User = player(Helper, 14.5, 12.5, "scp1762_early");
        ServerLevel World = Helper.getLevel();
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PAPER, 3));
        say(User, "here be dragons");
        Helper.assertFalse(Box.armed(User.getUUID(), World.getGameTime()), "The words armed a box that still holds dragons");
        useOn(User, Helper.absolutePos(Spot));
        Helper.assertTrue(Box.stage() == Scp1762Stage.Goodbye, "Paper changed a stage 2 box to stage " + Box.stage());
        Helper.assertTrue(User.getMainHandItem().getCount() == 3, "Paper was used on a stage 2 box");
        Helper.assertTrue(open(Helper) && Box.planned() == Scp1762Stage.GoodbyeCount, "Paper on a stage 2 box did not just open it normally");
        Helper.runAfterDelay(50, () -> {
            clean(Helper);
            BlockPos SparePos = Spot.offset(-3, 0, 0);
            Helper.setBlock(SparePos, Scp1762Registry.Box.get().defaultBlockState());
            Scp1762BoxBlockEntity Spare = (Scp1762BoxBlockEntity) Helper.getBlockEntity(SparePos);
            Spare.setStage(Scp1762Stage.Empty);
            Spare.interact(World, User);
            try {
                Helper.assertTrue(Scp1762BoxBlock.isOpen(Helper.getBlockState(SparePos)), "The empty box did not open");
                say(User, "here be dragons");
                Helper.assertFalse(Spare.armed(User.getUUID(), World.getGameTime()), "The words armed an open box");
            } finally {
                dismiss(Helper, User);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp1762_stageOneNote")
    public static void stageOneNote(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Thanks);
        Z0003TestPlayer User = player(Helper, 15.5, 12.5, "scp1762_reader");
        Box.open(Helper.getLevel(), User);
        Helper.runAfterDelay(Scp1762BoxBlockEntity.NoteDelay + 22, () -> {
            int Count = dragons(Helper).size();
            List<ItemEntity> Found = notes(Helper);
            dismiss(Helper, User);
            clean(Helper);
            Helper.assertTrue(Count >= Scp1762Stage.ThanksMin && Count <= Scp1762Stage.ThanksMax, "Stage 1 released " + Count + " dragons");
            Helper.assertTrue(Found.size() == 1, "Stage 1 gave " + Found.size() + " notes");
            Helper.assertTrue(Scp1762NoteItem.line(Found.get(0).getItem()) == 0, "Stage 1 gave the wrong note");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 1100, batch = "scp1762_stageOneReturnsToTwo")
    public static void stageOneReturnsToTwo(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Thanks);
        Box.open(Helper.getLevel(), null);
        Helper.runAfterDelay(50, () -> Box.setTimer(1));
        Helper.runAfterDelay(56, () -> Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Returning, "Stage 1 did not return normally"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Closed, "Still returning");
            Helper.assertTrue(Box.stage() == Scp1762Stage.Goodbye, "The natural return left the box at stage " + Box.stage());
            clean(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp1762_stageTwoGoodbye")
    public static void stageTwoGoodbye(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Goodbye);
        Box.open(Helper.getLevel(), null);
        Helper.runAfterDelay(Scp1762BoxBlockEntity.NoteDelay + 22, () -> {
            int Count = dragons(Helper).size();
            List<ItemEntity> Found = notes(Helper);
            clean(Helper);
            Helper.assertTrue(Count == Scp1762Stage.GoodbyeCount, "Stage 2 released " + Count + " dragons");
            Helper.assertTrue(Found.size() == 1 && Scp1762NoteItem.line(Found.get(0).getItem()) == 1, "Stage 2 did not give the goodbye note");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp1762_stageTwoRecallsToThree")
    public static void stageTwoRecallsToThree(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Goodbye);
        Box.open(Helper.getLevel(), null);
        watchHomecoming(Helper, Box);
        Helper.runAfterDelay(50, () -> Box.setTimer(1));
        Helper.runAfterDelay(53, () -> {
            Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Recall, "Stage 2 did not end in red smoke");
            Helper.assertTrue(dragons(Helper).size() == Scp1762Stage.GoodbyeCount, "The goodbye made dragons vanish on the spot");
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Closed && !open(Helper), "The lid did not close after the last smoke");
            Helper.assertTrue(dragons(Helper).isEmpty(), "Dragons stayed out");
            Helper.assertTrue(Box.stage() == Scp1762Stage.Empty, "Stage 2 ended at stage " + Box.stage());
            clean(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp1762_stageThreeFree")
    public static void stageThreeFree(GameTestHelper Helper) {
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.Empty);
        Z0003TestPlayer User = player(Helper, 14.5, 12.5, "scp1762_opener");
        ServerLevel World = Helper.getLevel();
        Box.interact(World, User);
        Helper.runAfterDelay(50, () -> {
            try {
                Helper.assertTrue(open(Helper), "Stage 3 did not open");
                Helper.assertTrue(dragons(Helper).isEmpty() && notes(Helper).isEmpty(), "Something came out of the empty box");
                Helper.assertFalse(Box.guarded(), "The empty box is still guarded");
                Box.interact(World, User);
                Helper.assertFalse(open(Helper), "Stage 3 did not close freely");
                Box.interact(World, User);
                Helper.assertTrue(open(Helper), "Stage 3 did not open again");
                Box.interact(World, User);
                Helper.assertFalse(open(Helper), "Stage 3 did not close again");
                Helper.assertTrue(Box.stage() == Scp1762Stage.Empty, "Stage 3 changed to " + Box.stage());
            } finally {
                dismiss(Helper, User);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp1762_stageSurvivesBreak")
    public static void stageSurvivesBreak(GameTestHelper Helper) {
        box(Helper, Scp1762Stage.Goodbye);
        ServerLevel World = Helper.getLevel();
        BlockPos Absolute = Helper.absolutePos(Spot);
        World.destroyBlock(Absolute, true);
        Helper.runAfterDelay(2, () -> {
            List<ItemEntity> Drops = World.getEntitiesOfClass(ItemEntity.class, Helper.getBounds().inflate(2.0), Found -> Scp1762BoxItem.is(Found.getItem()));
            Helper.assertTrue(Drops.size() == 1, "The box dropped " + Drops.size() + " items");
            ItemStack Carried = Drops.get(0).getItem().copy();
            Drops.get(0).discard();
            Helper.assertTrue(Scp1762BoxItem.stage(Carried) == Scp1762Stage.Goodbye, "The dropped box forgot its stage: " + Scp1762BoxItem.stage(Carried));
            Z0003TestPlayer User = player(Helper, 10.5, 10.5, "scp1762_carrier");
            String Result;
            try {
                User.setItemInHand(InteractionHand.MAIN_HAND, Carried);
                BlockPos Floor = Absolute.below();
                Result = String.valueOf(User.getMainHandItem().useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false))));
            } finally {
                dismiss(Helper, User);
            }
            Helper.assertTrue(World.getBlockEntity(Absolute) instanceof Scp1762BoxBlockEntity Placed && Placed.stage() == Scp1762Stage.Goodbye,
                    "The placed box lost its stage: " + Result + ", " + World.getBlockState(Absolute) + ", " + (World.getBlockEntity(Absolute) instanceof Scp1762BoxBlockEntity Seen ? Seen.stage() : -1)
                            + ", found at " + BlockPos.betweenClosedStream(Absolute.offset(-3, -2, -3), Absolute.offset(3, 3, 3)).filter(Pos -> World.getBlockState(Pos).is(Scp1762Registry.Box.get())).map(Pos -> Pos.subtract(Absolute).toShortString()).toList());
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1762_phraseNeedsOpenBox")
    public static void phraseNeedsOpenBox(GameTestHelper Helper) {
        Helper.assertTrue(Scp1762Chat.matches(" They are no longer here. "), "The plain phrase did not match");
        Helper.assertTrue(Scp1762Chat.matches("THEY ARE NO LONGER HERE!?"), "Shouting the phrase did not match");
        Helper.assertFalse(Scp1762Chat.matches("they are no longer here, right"), "Extra words matched");
        Helper.assertFalse(Scp1762Chat.matches("they are here"), "A different sentence matched");
        Scp1762BoxBlockEntity Box = box(Helper, Scp1762Stage.First);
        Z0003TestPlayer Near = player(Helper, 14.5, 12.5, "scp1762_near");
        Helper.assertFalse(Scp1762Chat.heard(Near, Phrase), "The phrase answered with no open box");
        Helper.assertTrue(Box.stage() == Scp1762Stage.First && Box.phase() == Scp1762BoxBlockEntity.Closed, "The closed box reacted");
        Box.open(Helper.getLevel(), null);
        Helper.runAfterDelay(45, () -> {
            Near.moveTo(Box.centre().x + 17.5, Box.centre().y, Box.centre().z, 0.0f, 0.0f);
            try {
                Helper.assertFalse(Scp1762Chat.heard(Near, Phrase), "The phrase reached a box more than sixteen blocks away");
                Helper.assertTrue(Box.phase() == Scp1762BoxBlockEntity.Open, "A distant phrase changed the box");
                Near.setGameMode(GameType.SPECTATOR);
                Near.moveTo(Box.centre().x + 2.0, Box.centre().y, Box.centre().z, 0.0f, 0.0f);
                Helper.assertFalse(Scp1762Chat.heard(Near, Phrase), "A spectator's phrase was heard");
            } finally {
                dismiss(Helper, Near);
                clean(Helper);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp1762_strangerDelivers")
    public static void strangerDelivers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp1762Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-1762 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-1762 is not filed as Safe");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-1762 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp1762BoxItem.is(Stack) && Stack.getCount() == 1 && Scp1762BoxItem.stage(Stack) == Scp1762Stage.First, "The stranger's gift is not a fresh box: " + Stack);
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp1762Registry.DocumentId).isPresent(), "SCP-1762 has no arrival hook");
        Z0003TestPlayer Reader = player(Helper, 8.5, 8.5, "scp1762_reader");
        try {
            BlockPos Where = Helper.absolutePos(new BlockPos(8, 2, 8));
            AnomalySpawners.spawnerFor(Scp1762Registry.DocumentId).get().spawn(new AnomalySpawners.Discovery(World, Where, Reader, Scp1762Registry.DocumentId, Document, false));
            for (BlockPos Pos : BlockPos.betweenClosed(Where.offset(-16, -4, -16), Where.offset(16, 8, 16))) {
                Helper.assertFalse(World.getBlockState(Pos).is(Scp1762Registry.Box.get()), "The arrival hook placed the box in the world at " + Pos);
            }
        } finally {
            dismiss(Helper, Reader);
        }
        Helper.succeed();
    }
}

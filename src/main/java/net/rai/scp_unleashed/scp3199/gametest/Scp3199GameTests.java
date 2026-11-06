package net.rai.scp_unleashed.scp3199.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.scp3199.Scp3199;
import net.rai.scp_unleashed.scp3199.Scp3199ArrivalCondition;
import net.rai.scp_unleashed.scp3199.Scp3199Egg;
import net.rai.scp_unleashed.scp3199.Scp3199EggBlock;
import net.rai.scp_unleashed.scp3199.Scp3199EggBlockEntity;
import net.rai.scp_unleashed.scp3199.Scp3199Goals;
import net.rai.scp_unleashed.scp3199.Scp3199Nesting;
import net.rai.scp_unleashed.scp3199.Scp3199Population;
import net.rai.scp_unleashed.scp3199.Scp3199Registry;
import net.rai.scp_unleashed.scp3199.Scp3199Rules;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3199GameTests {
    public static final String Arena = "scp3199_arena";
    public static final String Cell = "scp3199_cell";
    public static final BlockPos Middle = new BlockPos(12, 2, 12);
    public static final BlockPos CellMiddle = new BlockPos(5, 2, 5);

    private Scp3199GameTests() {
    }

    private static Scp3199 creature(GameTestHelper Helper, BlockPos Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        Scp3199 Made = Scp3199Registry.Scp3199Type.get().create(World);
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Relative));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Made.setYBodyRot(0.0f);
        Made.setYHeadRot(0.0f);
        Made.setNoAi(Still);
        World.addFreshEntity(Made);
        return Made;
    }

    private static <T extends Entity> T spawn(GameTestHelper Helper, EntityType<T> Type, BlockPos Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        T Made = Type.create(World);
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Relative));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        if (Made instanceof Mob Body) {
            Body.setNoAi(Still);
            Body.setPersistenceRequired();
        }
        World.addFreshEntity(Made);
        return Made;
    }

    private static Scp538TestPlayer player(GameTestHelper Helper, BlockPos Relative, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Relative));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(Mode);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, Scp538TestPlayer User) {
        User.enlist(false);
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(24.0, 10.0, 24.0).inflate(4.0);
    }

    private static void sweep(GameTestHelper Helper) {
        for (Entity Each : Helper.getLevel().getEntitiesOfClass(Entity.class, area(Helper), Each -> !(Each instanceof ServerPlayer))) {
            Each.discard();
        }
    }

    private static void rules(GameTestHelper Helper, int Cap) {
        Scp3199Rules.setMaxPopulation(Helper.getLevel().getServer(), Cap);
    }

    private static void finish(GameTestHelper Helper) {
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        sweep(Helper);
        Helper.succeed();
    }

    private static Scp3199Egg egg(GameTestHelper Helper, BlockPos Relative) {
        return Scp3199Egg.laid(Helper.getLevel(), Vec3.atBottomCenterOf(Helper.absolutePos(Relative)), Vec3.ZERO, 0.0f, -1);
    }

    private static List<Scp3199Egg> eggs(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp3199Egg.class, area(Helper), Entity::isAlive);
    }

    private static List<Scp3199Egg> eggsOf(GameTestHelper Helper, Scp3199 Mother) {
        return Helper.getLevel().getEntitiesOfClass(Scp3199Egg.class, area(Helper), Found -> Found.isAlive() && Found.mother() == Mother.getId());
    }

    private static List<Scp3199> creatures(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp3199.class, area(Helper), Entity::isAlive);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3199_arrivalReader")
    public static void arrivalReader(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer Reader = player(Helper, Middle, "scp3199_reader", GameType.SURVIVAL);
        Helper.assertTrue(Scp3199ArrivalCondition.victim(Reader) == Reader, "a survival reader was not chosen as the arrival target");
        Optional<BlockPos> Found = Scp3199ArrivalCondition.hiddenSpot(World, Reader, 6, 10, World.getRandom());
        Helper.assertTrue(Found.isPresent(), "no arrival spot was found near the reader");
        double Gap = Math.sqrt(Found.get().distSqr(Reader.blockPosition()));
        Helper.assertTrue(Gap >= 4.0 && Gap <= 13.0, "the arrival spot is " + Gap + " blocks from the reader");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp3199Registry.DocumentId);
        Helper.assertTrue(Document != null, "the SCP-3199 document is missing");
        Helper.assertTrue(Document.Condition() instanceof Scp3199ArrivalCondition, "the SCP-3199 document does not use the arrival condition");
        boolean Placed = Scp3199ArrivalCondition.manifest(new AnomalySpawners.Discovery(World, Found.get(), Reader, Scp3199Registry.DocumentId, Document, true));
        Helper.assertTrue(Placed, "SCP-3199 did not arrive");
        List<Scp3199> Arrived = World.getEntitiesOfClass(Scp3199.class, new AABB(Found.get()).inflate(1.0), Entity::isAlive);
        Helper.assertTrue(Arrived.size() == 1, "exactly one SCP-3199 should arrive, found " + Arrived.size());
        dismiss(Helper, Reader);
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3199_arrivalFallback")
    public static void arrivalFallback(GameTestHelper Helper) {
        sweep(Helper);
        Scp538TestPlayer Reader = player(Helper, Middle, "scp3199_builder", GameType.CREATIVE);
        Scp538TestPlayer Near = player(Helper, Middle.offset(4, 0, 0), "scp3199_near", GameType.SURVIVAL);
        Scp538TestPlayer Far = player(Helper, Middle.offset(-9, 0, 0), "scp3199_far", GameType.SURVIVAL);
        Scp538TestPlayer Watcher = player(Helper, Middle.offset(0, 0, 2), "scp3199_watcher", GameType.SPECTATOR);
        ServerPlayer Chosen = Scp3199ArrivalCondition.victim(Reader);
        Helper.assertTrue(Chosen == Near, "a creative reader should hand the arrival to the nearest survival player, got " + (Chosen == null ? "nobody" : Chosen.getGameProfile().getName()));
        Near.setGameMode(GameType.CREATIVE);
        Chosen = Scp3199ArrivalCondition.victim(Reader);
        Helper.assertTrue(Chosen == Far, "the fallback skipped the remaining survival player");
        Far.setGameMode(GameType.SPECTATOR);
        Helper.assertTrue(Scp3199ArrivalCondition.victim(Reader) == null, "creative and spectator players should never be arrival targets");
        for (Scp538TestPlayer User : List.of(Reader, Near, Far, Watcher)) {
            dismiss(Helper, User);
        }
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp3199_aggression")
    public static void aggression(GameTestHelper Helper) {
        sweep(Helper);
        Scp3199 Hunter = creature(Helper, Middle, false);
        Pig Prey = spawn(Helper, EntityType.PIG, Middle.offset(4, 0, 0), true);
        Villager Person = spawn(Helper, EntityType.VILLAGER, Middle.offset(-6, 0, 3), true);
        Helper.assertTrue(Scp3199Goals.isPrey(Hunter, Prey), "pigs are not prey");
        Helper.assertTrue(Scp3199Goals.isPrey(Hunter, Person), "villagers are not prey");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Prey.getHealth() < Prey.getMaxHealth() || !Prey.isAlive(), "SCP-3199 has not hurt the pig yet");
            rules(Helper, Scp3199Rules.DefaultMaxPopulation);
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3199_creative")
    public static void creative(GameTestHelper Helper) {
        sweep(Helper);
        Scp3199 Hunter = creature(Helper, Middle, false);
        Scp538TestPlayer Builder = player(Helper, Middle.offset(2, 0, 0), "scp3199_creative", GameType.CREATIVE);
        Scp538TestPlayer Ghost = player(Helper, Middle.offset(-2, 0, 0), "scp3199_ghost", GameType.SPECTATOR);
        Helper.assertFalse(Scp3199Goals.isPrey(Hunter, Builder), "a creative player counts as prey");
        Helper.assertFalse(Hunter.canAttack(Builder), "SCP-3199 may attack a creative player");
        Helper.assertFalse(Scp3199Goals.isPrey(Hunter, Ghost), "a spectator counts as prey");
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Hunter.getTarget() != Builder && Hunter.getTarget() != Ghost, "SCP-3199 targeted a creative or spectator player");
            Helper.assertTrue(Builder.getHealth() >= Builder.getMaxHealth(), "SCP-3199 hurt a creative player");
            Builder.setGameMode(GameType.SURVIVAL);
            Helper.assertTrue(Scp3199Goals.isPrey(Hunter, Builder), "a survival player is not prey");
            dismiss(Helper, Builder);
            dismiss(Helper, Ghost);
            finish(Helper);
        });
    }

    private static Scp3199 standOn(GameTestHelper Helper, BlockPos Relative, double Height, float Yaw) {
        ServerLevel World = Helper.getLevel();
        Scp3199 Made = Scp3199Registry.Scp3199Type.get().create(World);
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Relative)).add(0.0, Height, 0.0);
        Made.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        Made.setYBodyRot(Yaw);
        Made.setYHeadRot(Yaw);
        Made.setNoAi(true);
        World.addFreshEntity(Made);
        return Made;
    }

    private static void layLanded(GameTestHelper Helper, Scp3199 Mother, String Case, double Low, double High) {
        List<Scp3199Egg> Laid = eggsOf(Helper, Mother);
        Helper.assertTrue(Laid.size() == 1, "laying " + Case + " produced " + Laid.size() + " eggs");
        Scp3199Egg Egg = Laid.get(0);
        Helper.assertTrue(Egg.landed(), "the egg laid " + Case + " never landed");
        double Rest = Egg.getY() - Helper.absolutePos(BlockPos.ZERO).getY();
        Helper.assertTrue(Rest >= Low - 1.0e-3 && Rest <= High + 1.0e-3, "the egg laid " + Case + " rests at " + Rest + ", expected " + Low + " to " + High);
        Helper.assertTrue(Egg.getBoundingBox().deflate(1.0e-3).maxY > Egg.getY() && Helper.getLevel().noCollision(Egg, Egg.getBoundingBox().deflate(1.0e-3)),
                "the egg laid " + Case + " is stuck inside a block");
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp3199_fillCap")
    public static void fillCap(GameTestHelper Helper) {
        sweep(Helper);
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        ServerLevel World = Helper.getLevel();
        Scp3199 Mother = creature(Helper, Middle, true);
        for (int Index = 0; Index < Scp3199Nesting.AreaCap - 1; Index++) {
            egg(Helper, Middle.offset(Index % 5 - 2, 0, Index / 5 - 4));
        }
        Helper.assertTrue(Scp3199Population.area(World, Mother.position()) == Scp3199Nesting.AreaCap, "the area count ignores egg entities");
        Helper.assertFalse(Scp3199Rules.breedingAllowed(World) && Scp3199Population.level(World) < Scp3199Population.creatures(World), "the level count ignores egg entities");
        Mother.nesting().settleNow(Mother);
        for (int Delay = 5; Delay <= Scp3199Nesting.LayTicks; Delay += 5) {
            Helper.runAfterDelay(Delay, () -> Helper.assertFalse(Mother.isLaying(), "SCP-3199 started laying at the area cap"));
        }
        Helper.runAfterDelay(Scp3199Nesting.LayTicks + 20, () -> {
            Helper.assertTrue(eggs(Helper).size() == Scp3199Nesting.AreaCap - 1, "an egg was laid past the area cap");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp3199_layAnywhere")
    public static void layAnywhere(GameTestHelper Helper) {
        sweep(Helper);
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        for (int X = 1; X <= 4; X++) {
            for (int Z = 1; Z <= 5; Z++) {
                Helper.setBlock(new BlockPos(X, 2, Z), Blocks.STONE_SLAB.defaultBlockState());
            }
        }
        Scp3199 OnSlab = standOn(Helper, new BlockPos(2, 2, 2), 0.5, 0.0f);
        for (int X = 7; X <= 10; X++) {
            for (int Z = 1; Z <= 5; Z++) {
                Helper.setBlock(new BlockPos(X, 2, Z), Blocks.WHITE_CARPET.defaultBlockState());
            }
        }
        Scp3199 OnCarpet = standOn(Helper, new BlockPos(8, 2, 2), 0.0625, 0.0f);
        BlockState Snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3);
        for (int X = 13; X <= 16; X++) {
            for (int Z = 1; Z <= 5; Z++) {
                Helper.setBlock(new BlockPos(X, 2, Z), Snow);
            }
        }
        Scp3199 OnSnow = standOn(Helper, new BlockPos(14, 2, 2), 0.25, 0.0f);
        for (int X = 1; X <= 3; X++) {
            for (int Z = 9; Z <= 11; Z++) {
                for (int Y = 2; Y <= 5; Y++) {
                    if (X != 2 || Z != 10) {
                        Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
        Scp3199 Boxed = standOn(Helper, new BlockPos(2, 2, 10), 0.0, 0.0f);
        for (int X = 7; X <= 10; X++) {
            for (int Y = 2; Y <= 4; Y++) {
                Helper.setBlock(new BlockPos(X, Y, 11), Blocks.STONE.defaultBlockState());
            }
        }
        Scp3199 Walled = standOn(Helper, new BlockPos(8, 2, 10), 0.0, 0.0f);
        Scp3199 Stacked = standOn(Helper, new BlockPos(14, 2, 10), 0.0, 0.0f);
        for (int Index = 0; Index < 3; Index++) {
            Vec3 Below = Scp3199Nesting.mouth(Stacked, 0.0f);
            Scp3199Egg.laid(Helper.getLevel(), new Vec3(Below.x + (Index - 1) * 0.2, Stacked.getY(), Below.z - 0.45), Vec3.ZERO, 0.0f, -1);
        }
        for (Scp3199 Mother : List.of(OnSlab, OnCarpet, OnSnow, Boxed, Walled, Stacked)) {
            Mother.nesting().settleNow(Mother);
        }
        Helper.runAfterDelay(2, () -> {
            for (Scp3199 Mother : List.of(OnSlab, OnCarpet, OnSnow, Boxed, Walled, Stacked)) {
                Helper.assertTrue(Mother.isLaying(), "SCP-3199 did not start laying at " + Mother.blockPosition().subtract(Helper.absolutePos(BlockPos.ZERO)));
            }
        });
        Helper.runAfterDelay(Scp3199Nesting.LayTicks + 10, () -> {
            layLanded(Helper, OnSlab, "on slabs", 2.5, 2.5);
            layLanded(Helper, OnCarpet, "on carpet", 2.0625, 2.0625);
            layLanded(Helper, OnSnow, "on snow layers", 2.25, 2.25);
            layLanded(Helper, Boxed, "boxed in", 2.0, 2.0);
            layLanded(Helper, Walled, "against a wall", 2.0, 2.0);
            layLanded(Helper, Stacked, "onto other eggs", 2.0, 2.0 + Scp3199Egg.Height);
            Helper.assertTrue(Helper.getLevel().getBlockState(Helper.absolutePos(new BlockPos(3, 2, 2))).is(Blocks.STONE_SLAB), "the egg replaced a block");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp3199_layNoMatterWhat")
    public static void layNoMatterWhat(GameTestHelper Helper) {
        sweep(Helper);
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        ServerLevel World = Helper.getLevel();
        Scp3199 Hurt = standOn(Helper, new BlockPos(4, 2, 4), 0.0, 0.0f);
        Scp3199 Flooded = standOn(Helper, new BlockPos(12, 2, 4), 0.0, 0.0f);
        Scp3199 Dying = standOn(Helper, new BlockPos(20, 2, 4), 0.0, 0.0f);
        for (Scp3199 Mother : List.of(Hurt, Flooded, Dying)) {
            Mother.nesting().settleNow(Mother);
        }
        Scp538TestPlayer[] Watcher = new Scp538TestPlayer[1];
        Helper.runAfterDelay(10, () -> Watcher[0] = player(Helper, new BlockPos(4, 2, 1), "scp3199_watcher", GameType.SURVIVAL));
        Helper.runAfterDelay(20, () -> {
            Hurt.invulnerableTime = 0;
            Hurt.hurt(World.damageSources().generic(), 2.0f);
            for (int X = 10; X <= 14; X++) {
                for (int Z = 2; Z <= 6; Z++) {
                    if (X != 12 || Z != 4) {
                        Helper.setBlock(new BlockPos(X, 2, Z), Blocks.WATER.defaultBlockState());
                    }
                }
            }
            Dying.invulnerableTime = 0;
            Dying.hurt(World.damageSources().generic(), 100.0f);
        });
        Helper.runAfterDelay(26, () -> Helper.assertTrue(Hurt.isLaying() && Flooded.isLaying(), "laying stopped part way through"));
        Helper.runAfterDelay(Scp3199Nesting.LayTicks + 10, () -> {
            Helper.assertTrue(eggsOf(Helper, Hurt).size() == 1, "a hurt SCP-3199 lost its egg");
            Helper.assertTrue(eggsOf(Helper, Flooded).size() == 1, "a flooded SCP-3199 lost its egg");
            Helper.assertTrue(eggsOf(Helper, Dying).size() == 1, "an SCP-3199 that died mid-clip lost its egg");
            dismiss(Helper, Watcher[0]);
            for (int X = 10; X <= 14; X++) {
                for (int Z = 2; Z <= 6; Z++) {
                    Helper.setBlock(new BlockPos(X, 2, Z), Blocks.AIR.defaultBlockState());
                }
            }
            finish(Helper);
        });
    }

    @GameTest(template = Cell, timeoutTicks = 260, batch = "scp3199_settleLay")
    public static void settleLay(GameTestHelper Helper) {
        sweep(Helper);
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        Scp3199 Mother = creature(Helper, CellMiddle, true);
        Mother.nesting().settleFor(Mother, Scp3199Nesting.SettleTicks - 40);
        Helper.runAfterDelay(30, () -> {
            Helper.assertFalse(Mother.isLaying(), "SCP-3199 started laying before its settle timer ran out");
            Helper.assertTrue(eggs(Helper).isEmpty(), "an egg appeared before the settle timer ran out");
        });
        Helper.runAfterDelay(60, () -> Helper.assertTrue(Mother.isLaying(), "SCP-3199 did not start laying once settled"));
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Mother.isLaying(), "SCP-3199 is still laying");
            layLanded(Helper, Mother, "in the cell", 2.0, 2.0);
            Scp3199Egg Egg = eggsOf(Helper, Mother).get(0);
            Helper.assertTrue(Egg.position().distanceTo(Mother.position()) < 1.6, "the egg landed far from SCP-3199");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3199_eggStays")
    public static void eggStays(GameTestHelper Helper) {
        sweep(Helper);
        Scp3199Egg Egg = Scp3199Egg.laid(Helper.getLevel(), Vec3.atBottomCenterOf(Helper.absolutePos(Middle)).add(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 0.05), 0.0f, -1);
        Helper.assertFalse(Egg.isInvisible(), "a falling egg is invisible");
        Helper.assertFalse(Egg.isPickable() || Egg.isPushable() || Egg.canBeCollidedWith() || Egg.shouldShowName(), "the egg can be targeted, pushed, collided with or named");
        Helper.runAfterDelay(30, () -> Helper.assertTrue(Egg.landed(), "the egg never landed"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Egg.landed() && Helper.getLevel().getGameTime() - Egg.landedAt() > Scp3199Egg.SettleTicks * 2, "the egg has not been down long enough");
            Helper.assertTrue(Egg.isAlive() && !Egg.isInvisible(), "the egg disappeared after landing");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3199_eggImmune")
    public static void eggImmune(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp3199Egg Egg = egg(Helper, Middle);
        Helper.runAfterDelay(5, () -> {
            Vec3 Before = Egg.position();
            FakePlayer Miner = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp3199_miner"));
            Miner.setGameMode(GameType.SURVIVAL);
            Helper.assertFalse(Egg.hurt(World.damageSources().playerAttack(Miner), 100.0f), "a player hurt the egg");
            Helper.assertFalse(Egg.hurt(World.damageSources().lava(), 100.0f), "lava hurt the egg");
            World.explode(null, Egg.getX(), Egg.getY() + 0.2, Egg.getZ(), 4.0f, Level.ExplosionInteraction.TNT);
            Egg.move(MoverType.PISTON, new Vec3(1.0, 0.0, 0.0));
            Helper.assertTrue(Egg.isAlive(), "the egg did not survive tools and explosions");
            Helper.assertTrue(Egg.position().distanceTo(Before) < 1.0e-3, "the egg was moved by a piston or explosion");
            FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp3199_pod"));
            ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
            Helper.assertTrue(StasisPods.refusal(User, Pod, Egg) != null, "a stasis pod accepts the egg");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3199_eggPodRefused")
    public static void eggPodRefused(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp3199Egg Egg = egg(Helper, Middle);
        Scp538TestPlayer User = player(Helper, Middle.offset(1, 0, 0), "scp3199_carrier", GameType.SURVIVAL);
        try {
            ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Abyssal));
            User.setItemInHand(InteractionHand.MAIN_HAND, Pod);
            Helper.assertTrue(StasisPods.refusal(User, Pod, Egg) != null, "a stasis pod accepts the egg");
            Helper.assertTrue(StasisPods.isVetoed(Egg), "the egg is not vetoed for pods, shop or raid captures");
            Helper.assertTrue(Egg.getType().is(StasisRegistry.Immune), "the egg is not tagged as stasis immune");
            User.interactOn(Egg, InteractionHand.MAIN_HAND);
            Helper.assertFalse(StasisPodItem.isOccupied(User.getMainHandItem()), "right-clicking the egg with a pod captured it");
            Helper.assertTrue(Egg.isAlive() && !Egg.isRemoved(), "a pod removed the egg");
            BlockPos Old = Helper.absolutePos(Middle.offset(-3, 0, 0));
            World.setBlock(Old, Scp3199Registry.Nest.get().defaultBlockState(), 3);
            User.gameMode.useItemOn(User, World, User.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(Old), Direction.UP, Old, false));
            Helper.assertFalse(StasisPodItem.isOccupied(User.getMainHandItem()), "using a pod on an old egg block captured it");
            Scp3199 Mother = creature(Helper, Middle.offset(4, 0, 0), true);
            Mother.nesting().settleNow(Mother);
            Helper.assertTrue(StasisPods.refusal(User, Pod, Mother) == null, "a pod refuses a calm SCP-3199");
            Mother.setLaying(true);
            Helper.assertTrue(StasisPods.refusal(User, Pod, Mother) != null, "a pod takes SCP-3199 with an egg on the way");
            Mother.setLaying(false);
        } finally {
            dismiss(Helper, User);
        }
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3199_fasterBreeding")
    public static void fasterBreeding(GameTestHelper Helper) {
        sweep(Helper);
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        Helper.assertTrue(Scp3199Egg.HatchTicks * 4 <= 6000, "eggs hatch no faster than before: " + Scp3199Egg.HatchTicks + " ticks");
        Helper.assertTrue(Scp3199.GrowTicks * 3 <= 24000, "hatchlings mature no faster than before: " + Scp3199.GrowTicks + " ticks");
        Helper.assertTrue(Scp3199Rules.DefaultMaxPopulation == 32, "the population cap changed");
        Scp3199Egg Egg = egg(Helper, Middle);
        Scp3199 Young = creature(Helper, Middle.offset(5, 0, 0), false);
        Young.makeJuvenile();
        int[] Start = {0, 0};
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Egg.landed(), "the egg has not landed"))
                .thenExecute(() -> {
                    Helper.assertTrue(Egg.hatchLeft() <= Scp3199Egg.HatchTicks && Egg.hatchLeft() > Scp3199Egg.HatchTicks - 40, "a fresh egg does not start at the new hatch time: " + Egg.hatchLeft());
                    Start[0] = Egg.hatchLeft();
                    Start[1] = Young.growth();
                })
                .thenIdle(100)
                .thenExecute(() -> {
                    int Counted = Start[0] - Egg.hatchLeft();
                    Helper.assertTrue(Counted >= 95 && Counted <= 105, "the egg counted down " + Counted + " ticks in 100");
                    int Grown = Young.growth() - Start[1];
                    Helper.assertTrue(Grown >= 80 && Grown <= 120, "the hatchling grew " + Grown + " ticks in 100");
                    Helper.assertFalse(Young.isAdult(), "the hatchling grew up at once");
                    Young.discard();
                    finish(Helper);
                });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3199_acidEgg")
    public static void acidEgg(GameTestHelper Helper) {
        sweep(Helper);
        Scp3199Egg Beside = egg(Helper, Middle);
        Helper.setBlock(Middle.offset(1, 0, 0), SulfurRegistry.Acid.get().defaultFluidState().createLegacyBlock());
        Scp3199Egg Inside = egg(Helper, Middle.offset(-5, 0, 0));
        Helper.setBlock(Middle.offset(-5, 0, 0), SulfurRegistry.Acid.get().defaultFluidState().createLegacyBlock());
        Scp3199Egg Watered = egg(Helper, new BlockPos(2, 2, 2));
        Helper.setBlock(new BlockPos(3, 2, 2), Blocks.WATER.defaultBlockState());
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Beside.isAlive(), "acid beside an egg has not destroyed it");
            Helper.assertFalse(Inside.isAlive(), "acid around an egg has not destroyed it");
            Helper.assertTrue(Watered.isAlive(), "water destroyed an egg");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3199_hatch")
    public static void hatch(GameTestHelper Helper) {
        sweep(Helper);
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        Scp3199Egg Egg = egg(Helper, Middle);
        Egg.setHatch(0);
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Egg.isAlive(), "the egg has not hatched");
            List<Scp3199> Young = creatures(Helper);
            Helper.assertTrue(Young.size() == 1, "hatching made " + Young.size() + " instances");
            Helper.assertFalse(Young.get(0).isAdult(), "the hatchling is already full grown");
            Helper.assertTrue(Young.get(0).getBbHeight() < Scp3199.Height * 0.8f, "the hatchling is full size");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3199_convertBlock")
    public static void convertBlock(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(Middle, Scp3199Registry.Nest.get().defaultBlockState());
        BlockPos Old = Helper.absolutePos(Middle);
        Helper.assertTrue(World.getBlockEntity(Old) instanceof Scp3199EggBlockEntity, "the old egg block has no block entity");
        ((Scp3199EggBlockEntity) World.getBlockEntity(Old)).setHatch(1234);
        Helper.succeedWhen(() -> {
            Helper.assertFalse(World.getBlockState(Old).is(Scp3199Registry.Nest.get()), "the old egg block was not converted");
            List<Scp3199Egg> Found = eggs(Helper);
            Helper.assertTrue(Found.size() == 1, "the old egg block became " + Found.size() + " egg entities");
            Scp3199Egg Egg = Found.get(0);
            Helper.assertTrue(Egg.position().distanceTo(Vec3.atBottomCenterOf(Old)) < 0.05, "the converted egg is not where the block was");
            Helper.assertTrue(Egg.hatchLeft() > 1200 && Egg.hatchLeft() <= 1234, "the converted egg lost its hatch timer: " + Egg.hatchLeft());
            Helper.assertTrue(Egg.landed() && !Egg.isInvisible(), "the converted egg is not settled and visible");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3199_eggRenderer")
    public static void eggRenderer(GameTestHelper Helper) {
        String Text;
        try (InputStream Stream = Scp3199GameTests.class.getResourceAsStream("/net/rai/scp_unleashed/scp3199/client/Scp3199ClientEvents.class")) {
            Helper.assertTrue(Stream != null, "the SCP-3199 client events class is missing");
            Text = new String(Stream.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
        Helper.assertTrue(Text.contains("registerEntityRenderer") && Text.contains("Scp3199EggRenderer") && Text.contains("EggType"),
                "the egg entity renderer is not registered on the client");
        Helper.assertTrue(Text.contains("RegisterAdditional") && Text.contains("FreshModel"), "the fresh egg model is not registered on the client");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp3199_populationCap")
    public static void populationCap(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        creature(Helper, Middle.offset(6, 0, 6), true);
        rules(Helper, Scp3199Population.level(World));
        Scp3199Egg Egg = egg(Helper, Middle);
        Egg.setHatch(0);
        Helper.assertFalse(Scp3199Rules.breedingAllowed(World), "breeding is allowed at the population cap");
        Helper.assertTrue(Scp3199Population.level(World) == 2, "the level count does not count the egg");
        Helper.runAfterDelay(Scp3199Egg.WaitTicks * 2 + 10, () -> {
            Helper.assertTrue(Egg.isAlive(), "an egg hatched at the population cap");
            rules(Helper, 0);
            Helper.assertFalse(Scp3199Rules.breedingAllowed(World), "a cap of 0 still allows breeding");
        });
        Helper.runAfterDelay(Scp3199Egg.WaitTicks * 4 + 20, () -> {
            Helper.assertTrue(Egg.isAlive(), "an egg hatched with the cap at 0");
            rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        });
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Egg.isAlive(), "the egg did not hatch once the cap was raised");
            finish(Helper);
        });
    }

    @GameTest(template = Cell, timeoutTicks = 400, batch = "scp3199_flooded")
    public static void flooded(GameTestHelper Helper) {
        sweep(Helper);
        rules(Helper, Scp3199Rules.DefaultMaxPopulation);
        Helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER.defaultBlockState());
        Scp3199Egg Soaked = egg(Helper, new BlockPos(2, 2, 2));
        Scp3199Egg Wet = egg(Helper, new BlockPos(8, 2, 2));
        Helper.setBlock(new BlockPos(8, 2, 3), Blocks.WATER.defaultBlockState());
        Soaked.setHatch(0);
        Wet.setHatch(0);
        for (int X = 1; X <= 9; X++) {
            for (int Z = 5; Z <= 9; Z++) {
                Helper.setBlock(new BlockPos(X, 2, Z), Blocks.WATER.defaultBlockState());
            }
        }
        Scp3199 Swimmer = creature(Helper, new BlockPos(5, 2, 7), true);
        Swimmer.nesting().settleNow(Swimmer);
        Helper.runAfterDelay(Scp3199Nesting.LayTicks + Scp3199Egg.WaitTicks * 2 + 20, () -> {
            Helper.assertTrue(Soaked.isAlive(), "a submerged egg hatched");
            Helper.assertTrue(Wet.isAlive(), "an egg beside water hatched");
            Helper.assertFalse(Swimmer.isLaying(), "SCP-3199 laid while standing in water");
            Helper.assertTrue(eggs(Helper).size() == 2, "an egg was laid in the flooded cell");
            Helper.assertTrue(creatures(Helper).size() == 1, "something hatched in the flooded cell");
            for (int X = 1; X <= 9; X++) {
                for (int Z = 1; Z <= 9; Z++) {
                    Helper.setBlock(new BlockPos(X, 2, Z), Blocks.AIR.defaultBlockState());
                }
            }
            finish(Helper);
        });
    }

    @GameTest(template = Cell, timeoutTicks = 300, batch = "scp3199_noDrown")
    public static void noDrown(GameTestHelper Helper) {
        sweep(Helper);
        for (int X = 1; X <= 9; X++) {
            for (int Z = 1; Z <= 9; Z++) {
                for (int Y = 2; Y <= 5; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.WATER.defaultBlockState());
                }
            }
        }
        Scp3199 Swimmer = creature(Helper, new BlockPos(5, 2, 5), true);
        Swimmer.setAirSupply(0);
        Helper.assertTrue(Swimmer.canBreatheUnderwater(), "SCP-3199 cannot breathe underwater");
        Helper.runAfterDelay(200, () -> {
            Helper.assertTrue(Swimmer.isAlive() && Swimmer.getHealth() >= Swimmer.getMaxHealth(), "SCP-3199 drowned");
            for (int X = 1; X <= 9; X++) {
                for (int Z = 1; Z <= 9; Z++) {
                    for (int Y = 2; Y <= 5; Y++) {
                        Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp3199_dies")
    public static void dies(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp3199 Target = creature(Helper, Middle, true);
        Helper.assertTrue(Target.getMaxHealth() == (float) Scp3199.MaxHealth, "SCP-3199 has the wrong health");
        Target.hurt(World.damageSources().generic(), 15.0f);
        float Left = Target.getHealth();
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Target.getHealth() <= Left, "SCP-3199 regenerated");
            Target.invulnerableTime = 0;
            Target.hurt(World.damageSources().generic(), 40.0f);
            Helper.assertTrue(Target.isDeadOrDying(), "SCP-3199 survived enough damage to kill it");
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Target.isRemoved(), "the SCP-3199 body was not removed");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3199_stasis")
    public static void stasis(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp3199 Target = creature(Helper, Middle, true);
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp3199_stasis"));
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Target) == null, "a stasis pod refuses SCP-3199");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Target), "a stasis pod failed to capture SCP-3199");
        Helper.assertTrue(Target.isRemoved(), "the captured SCP-3199 is still in the world");
        Scp3199 Laying = creature(Helper, Middle.offset(4, 0, 0), true);
        Laying.setLaying(true);
        Helper.assertTrue(StasisPods.refusal(User, new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint)), Laying) != null, "a pod can take SCP-3199 mid-lay");
        Laying.setLaying(false);
        finish(Helper);
    }

}

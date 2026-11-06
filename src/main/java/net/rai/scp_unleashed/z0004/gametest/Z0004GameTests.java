package net.rai.scp_unleashed.z0004.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.mining.shop.ShopCatalog;
import net.rai.scp_unleashed.mining.shop.ShopEntry;
import net.rai.scp_unleashed.mining.shop.ShopKind;
import net.rai.scp_unleashed.mining.shop.ShopService;
import net.rai.scp_unleashed.z0004.Z0004ProcuredCondition;
import net.rai.scp_unleashed.z0004.Z0004Captain;
import net.rai.scp_unleashed.z0004.Z0004DeckSlot;
import net.rai.scp_unleashed.z0004.Z0004ChestBlock;
import net.rai.scp_unleashed.z0004.Z0004ChestBlockEntity;
import net.rai.scp_unleashed.z0004.Z0004Fight;
import net.rai.scp_unleashed.z0004.Z0004Pirate;
import net.rai.scp_unleashed.z0004.Z0004Registry;
import net.rai.scp_unleashed.z0004.Z0004Ship;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Z0004GameTests {
    public static final String Sea = "z0004_sea";
    public static final String Shore = "z0004_shore";
    public static final String Deep = "z0004_deep";
    public static final BlockPos DeepChest = new BlockPos(3, 1, 24);
    public static final BlockPos SeaChest = new BlockPos(3, 7, 32);
    public static final BlockPos ShoreChest = new BlockPos(8, 1, 8);

    private Z0004GameTests() {
    }

    static Scp538TestPlayer player(GameTestHelper Helper, Vec3 Relative, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Relative);
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    static void dismiss(GameTestHelper Helper, ServerPlayer... Users) {
        for (ServerPlayer User : Users) {
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(64.0, 24.0, 64.0).inflate(24.0);
    }

    static <T extends Entity> List<T> present(GameTestHelper Helper, Class<T> Kind) {
        return Helper.getLevel().getEntitiesOfClass(Kind, area(Helper), Entity::isAlive);
    }

    static void sweep(GameTestHelper Helper) {
        for (Entity Each : Helper.getLevel().getEntitiesOfClass(Entity.class, area(Helper), Each -> !(Each instanceof ServerPlayer))) {
            Each.discard();
        }
    }

    static Z0004ChestBlockEntity chest(GameTestHelper Helper, BlockPos Relative, boolean Waterlogged) {
        Helper.setBlock(Relative, Z0004Registry.Chest.get().defaultBlockState().setValue(Z0004ChestBlock.Waterlogged, Waterlogged));
        return (Z0004ChestBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(Relative));
    }

    static Z0004Ship ship(GameTestHelper Helper, Vec3 Relative, float Yaw) {
        ServerLevel World = Helper.getLevel();
        Vec3 At = Helper.absoluteVec(Relative);
        int Surface = Helper.absolutePos(new BlockPos(0, 7, 0)).getY();
        Z0004Ship Made = Z0004Registry.Ship.get().create(World);
        Made.prepare(new Z0004Fight.Spot(new Vec3(At.x, Surface + Z0004Fight.SurfaceOffset, At.z), Yaw, Surface), null, null);
        World.addFreshEntity(Made);
        Made.surfaceNow();
        return Made;
    }

    static Z0004Pirate crew(GameTestHelper Helper, Vec3 Relative, int Variant) {
        ServerLevel World = Helper.getLevel();
        Z0004Pirate Made = Z0004Registry.Crew.get().create(World);
        Vec3 At = Helper.absoluteVec(Relative);
        Made.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        Made.finalizeSpawn(World, World.getCurrentDifficultyAt(Made.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        Made.setVariant(Variant);
        World.addFreshEntity(Made);
        return Made;
    }

    @GameTest(template = Shore, timeoutTicks = 60, batch = "z0004_land")
    public static void openingOnLandDoesNothing(GameTestHelper Helper) {
        Z0004ChestBlockEntity Chest = chest(Helper, ShoreChest, false);
        Scp538TestPlayer User = player(Helper, new Vec3(8.5, 1.0, 10.5), "z0004_land", GameType.SURVIVAL);
        try {
            Helper.assertFalse(Z0004Fight.largeWater(Helper.getLevel(), Chest.getBlockPos()), "dry land counted as a large body of water");
            Chest.open(Helper.getLevel(), User);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Idle, "opening on land changed the chest: stage " + Chest.stage());
            Helper.assertTrue(present(Helper, Z0004Ship.class).isEmpty(), "a ship appeared on land");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Sea, timeoutTicks = 60, batch = "z0004_summon")
    public static void openingInLargeWaterSummonsOnce(GameTestHelper Helper) {
        Z0004ChestBlockEntity Chest = chest(Helper, SeaChest, true);
        Scp538TestPlayer User = player(Helper, new Vec3(3.5, 8.0, 34.5), "z0004_summoner", GameType.SURVIVAL);
        try {
            ServerLevel World = Helper.getLevel();
            Helper.assertTrue(Z0004Fight.touchesWater(World, Chest.getBlockPos()), "the chest in the sea does not touch water");
            int Water = Z0004Fight.connectedWater(World, Chest.getBlockPos(), Z0004Fight.WaterNeeded);
            Helper.assertTrue(Water >= Z0004Fight.WaterNeeded, "the sea only counted " + Water + " water blocks");
            Optional<Z0004Fight.Spot> Spot = Z0004Fight.findSpot(World, Chest.getBlockPos());
            Helper.assertTrue(Spot.isPresent(), "no ship spot was found in the sea");
            double Distance = Spot.get().Position().subtract(Vec3.atCenterOf(Chest.getBlockPos())).horizontalDistance();
            Helper.assertTrue(Distance >= Z0004Fight.SpotMin - 1.0 && Distance <= Z0004Fight.SpotMax + 1.0, "the ship spot is " + Distance + " blocks away");
            Chest.open(World, User);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Active, "the chest did not start a fight: " + Chest.stage());
            List<Z0004Ship> Ships = present(Helper, Z0004Ship.class);
            Helper.assertTrue(Ships.size() == 1, "expected one ship, found " + Ships.size());
            Helper.assertTrue(Ships.get(0).getUUID().equals(Chest.shipId()) && Chest.participants().contains(User.getUUID()), "the fight is not linked to its ship and summoner");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Sea, timeoutTicks = 60, batch = "z0004_double")
    public static void noSecondSummonWhileActive(GameTestHelper Helper) {
        Z0004ChestBlockEntity Chest = chest(Helper, SeaChest, true);
        Scp538TestPlayer User = player(Helper, new Vec3(3.5, 8.0, 34.5), "z0004_double", GameType.SURVIVAL);
        try {
            ServerLevel World = Helper.getLevel();
            Chest.open(World, User);
            UUID First = Chest.fightId();
            Chest.open(World, User);
            Chest.open(World, User);
            Helper.assertTrue(First != null && First.equals(Chest.fightId()), "a second open replaced the fight");
            Helper.assertTrue(present(Helper, Z0004Ship.class).size() == 1, "opening again summoned another ship");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Sea, timeoutTicks = 400, batch = "z0004_cannons")
    public static void shipFiresAtPlayerInRange(GameTestHelper Helper) {
        Z0004Ship Ship = ship(Helper, new Vec3(32.0, 0.0, 32.0), 0.0f);
        Scp538TestPlayer User = player(Helper, new Vec3(48.0, 8.0, 32.0), "z0004_target", GameType.SURVIVAL);
        User.setInvulnerable(true);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Ship.shotsFired() > 0, "the ship has not fired yet, phase " + Ship.phase()))
                .thenExecute(() -> {
                    dismiss(Helper, User);
                    sweep(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Sea, timeoutTicks = 60, batch = "z0004_crew_spawn")
    public static void destroyingShipReleasesCrew(GameTestHelper Helper) {
        Z0004Ship Ship = ship(Helper, new Vec3(32.0, 0.0, 32.0), 90.0f);
        Scp538TestPlayer User = player(Helper, new Vec3(32.0, 8.0, 50.0), "z0004_sinker", GameType.SURVIVAL);
        try {
            Helper.assertTrue(Ship.parts().length >= 4, "the ship has too few hitboxes");
            boolean Hit = Ship.parts()[0].hurt(Helper.getLevel().damageSources().playerAttack(User), 1000.0f);
            Helper.assertTrue(Hit && Ship.isDeadOrDying(), "a heavy blow to the bow did not sink the ship");
            Helper.assertTrue(present(Helper, Z0004Pirate.class).isEmpty(), "the crew left the deck before falling");
            Ship.releaseCrew();
            Ship.releaseCrew();
            List<Z0004Pirate> Crew = present(Helper, Z0004Pirate.class);
            long Captains = Crew.stream().filter(Each -> Each instanceof Z0004Captain).count();
            Helper.assertTrue(Crew.size() == Z0004DeckSlot.All.size() && Captains == 1, "expected one crewman per deck slot, found " + Crew.size() + " with " + Captains + " captains");
            for (int Variant = 0; Variant < Z0004Pirate.Variants; Variant++) {
                int Wanted = Variant;
                Helper.assertTrue(Crew.stream().anyMatch(Each -> !Each.captain() && Each.variant() == Wanted), "variant " + Variant + " is missing");
            }
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Sea, timeoutTicks = 420, batch = "z0004_underwater")
    public static void crewBreatheAndShootUnderwater(GameTestHelper Helper) {
        Z0004Pirate Gunner = crew(Helper, new Vec3(30.5, 3.0, 30.5), Z0004Pirate.Pistol);
        Scp538TestPlayer User = player(Helper, new Vec3(30.5, 3.0, 34.5), "z0004_diver", GameType.SURVIVAL);
        User.setInvulnerable(true);
        Gunner.setTarget(User);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Gunner.shotsFired() > 0 && Gunner.isUnderWater(), "the gunner has not fired underwater yet"))
                .thenIdle(330)
                .thenExecute(() -> {
                    try {
                        Helper.assertTrue(Gunner.isAlive() && Gunner.canBreatheUnderwater(), "the gunner cannot breathe underwater");
                        Helper.assertTrue(Gunner.getHealth() >= Gunner.getMaxHealth(), "the gunner lost health underwater: " + Gunner.getHealth());
                    } finally {
                        dismiss(Helper, User);
                        sweep(Helper);
                    }
                }).thenSucceed();
    }

    @GameTest(template = Sea, timeoutTicks = 80, batch = "z0004_reward")
    public static void winningPaysOnceThenRests(GameTestHelper Helper) {
        Z0004ChestBlockEntity Chest = chest(Helper, SeaChest, true);
        Scp538TestPlayer User = player(Helper, new Vec3(3.5, 8.0, 34.5), "z0004_winner", GameType.SURVIVAL);
        try {
            ServerLevel World = Helper.getLevel();
            Chest.open(World, User);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Active, "no fight started");
            Z0004Ship Ship = (Z0004Ship) World.getEntity(Chest.shipId());
            Ship.surfaceNow();
            Ship.hurt(World.damageSources().playerAttack(User), 1000.0f);
            Ship.releaseCrew();
            Helper.assertTrue(Chest.boarding() && Chest.crew().size() == Z0004DeckSlot.All.size(), "the chest is not tracking the crew: " + Chest.crew().size());
            for (UUID Id : List.copyOf(Chest.crew())) {
                Entity Member = World.getEntity(Id);
                if (Member instanceof LivingEntity Living) {
                    Living.kill();
                }
            }
            Chest.check(World);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Won, "the fight was not won after the crew fell: " + Chest.stage());
            payOnce(Helper, World, Chest, User);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Idle && present(Helper, Z0004Ship.class).stream().noneMatch(Each -> !Each.isDeadOrDying()),
                    "the resting chest summoned again");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    static void payOnce(GameTestHelper Helper, ServerLevel World, Z0004ChestBlockEntity Chest, Scp538TestPlayer User) {
        User.getInventory().clearContent();
        Chest.open(World, User);
        long Stacks = User.getInventory().items.stream().filter(Stack -> !Stack.isEmpty()).count();
        Helper.assertTrue(User.getInventory().contains(new ItemStack(Z0004Registry.Cutlass.get())), "the reward had no cutlass");
        Helper.assertTrue(Stacks >= 5, "the reward was too small: " + Stacks);
        Helper.assertTrue(Chest.rewards() == 1 && Chest.stage() == Z0004ChestBlockEntity.Idle, "the reward was not counted once");
        Helper.assertTrue(Chest.restUntil() >= World.getGameTime() + Z0004Fight.CooldownTicks - 1, "the chest did not start resting");
        User.getInventory().clearContent();
        Chest.open(World, User);
        Helper.assertTrue(User.getInventory().isEmpty() && Chest.rewards() == 1, "the chest paid twice");
    }

    static void win(GameTestHelper Helper, ServerLevel World, Z0004ChestBlockEntity Chest, Scp538TestPlayer User) {
        Z0004Ship Ship = (Z0004Ship) World.getEntity(Chest.shipId());
        Ship.surfaceNow();
        Ship.hurt(World.damageSources().playerAttack(User), 1000.0f);
        Ship.releaseCrew();
        for (UUID Id : List.copyOf(Chest.crew())) {
            if (World.getEntity(Id) instanceof LivingEntity Living) {
                Living.kill();
            }
        }
        Chest.check(World);
        Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Won, "the fight was not won: " + Chest.stage());
    }

    @GameTest(template = Deep, timeoutTicks = 80, batch = "z0004_deep")
    public static void deepFloorChestSummonsAndPays(GameTestHelper Helper) {
        Z0004ChestBlockEntity Chest = chest(Helper, DeepChest, true);
        Scp538TestPlayer User = player(Helper, new Vec3(4.5, 2.0, 25.5), "z0004_deep_diver", GameType.SURVIVAL);
        try {
            ServerLevel World = Helper.getLevel();
            int Surface = Z0004Fight.surfaceAbove(World, Chest.getBlockPos());
            Helper.assertTrue(Surface - Chest.getBlockPos().getY() >= 25, "the chest is only " + (Surface - Chest.getBlockPos().getY()) + " blocks deep");
            Helper.assertTrue(Z0004Fight.largeWater(World, Chest.getBlockPos()), "deep water was not counted as a large body of water");
            Chest.open(World, User);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Active, "the deep chest did not start a fight");
            Z0004Ship Ship = (Z0004Ship) World.getEntity(Chest.shipId());
            Helper.assertTrue(Ship != null && Math.abs(Ship.getY() - (Surface + Z0004Fight.SurfaceOffset) + Z0004Ship.RiseDepth) < 0.01, "the ship did not rise from below the surface");
            for (int Index = 0; Index < 4; Index++) {
                Chest.check(World);
            }
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Active, "a diver at the deep chest counted as having left");
            win(Helper, World, Chest, User);
            payOnce(Helper, World, Chest, User);
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Shore, timeoutTicks = 60, batch = "z0004_pond")
    public static void smallPondDoesNothing(GameTestHelper Helper) {
        for (int X = 10; X <= 12; X++) {
            for (int Z = 10; Z <= 12; Z++) {
                Helper.setBlock(new BlockPos(X, 1, Z), net.minecraft.world.level.block.Blocks.WATER);
            }
        }
        Z0004ChestBlockEntity Chest = chest(Helper, new BlockPos(9, 1, 11), false);
        Scp538TestPlayer User = player(Helper, new Vec3(8.5, 1.0, 13.5), "z0004_ponder", GameType.SURVIVAL);
        try {
            ServerLevel World = Helper.getLevel();
            Helper.assertTrue(Z0004Fight.touchesWater(World, Chest.getBlockPos()), "the chest does not touch the pond");
            Helper.assertFalse(Z0004Fight.largeWater(World, Chest.getBlockPos()), "a small pond counted as a large body of water");
            Chest.open(World, User);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Idle && present(Helper, Z0004Ship.class).isEmpty(), "the pond summoned a ship");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Sea, timeoutTicks = 60, batch = "z0004_leave")
    public static void leavingEndsTheFightWithoutReward(GameTestHelper Helper) {
        Z0004ChestBlockEntity Chest = chest(Helper, SeaChest, true);
        Scp538TestPlayer User = player(Helper, new Vec3(3.5, 8.0, 34.5), "z0004_leaver", GameType.SURVIVAL);
        try {
            ServerLevel World = Helper.getLevel();
            Chest.open(World, User);
            Z0004Ship Ship = (Z0004Ship) World.getEntity(Chest.shipId());
            dismiss(Helper, User);
            for (int Index = 0; Index <= Z0004Fight.LeaveTicks / Z0004ChestBlockEntity.CheckInterval; Index++) {
                Chest.check(World);
            }
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Idle && Chest.rewards() == 0, "leaving did not end the fight: " + Chest.stage());
            Helper.assertTrue(Ship.phase() == Z0004Ship.Retreating, "the ship did not sink back");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Sea, timeoutTicks = 60, batch = "z0004_timeout")
    public static void timeoutEndsTheFightWithoutReward(GameTestHelper Helper) {
        Z0004ChestBlockEntity Chest = chest(Helper, SeaChest, true);
        Scp538TestPlayer User = player(Helper, new Vec3(3.5, 8.0, 34.5), "z0004_waiter", GameType.SURVIVAL);
        try {
            ServerLevel World = Helper.getLevel();
            Chest.open(World, User);
            Z0004Ship Ship = (Z0004Ship) World.getEntity(Chest.shipId());
            Chest.startedAt(World.getGameTime() - Z0004Fight.FightTicks - 5);
            Chest.check(World);
            Helper.assertTrue(Chest.stage() == Z0004ChestBlockEntity.Idle && Chest.rewards() == 0, "the fight did not time out: " + Chest.stage());
            Helper.assertTrue(Ship.phase() == Z0004Ship.Retreating, "the ship did not sink back after the timeout");
            Chest.forceStage(Z0004ChestBlockEntity.Idle, 0L);
            int Before = World.getEntitiesOfClass(ItemEntity.class, new AABB(Chest.getBlockPos()).inflate(3.0)).size();
            Helper.assertTrue(Before == 0, "a lost fight left loot behind");
        } finally {
            dismiss(Helper, User);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Sea, timeoutTicks = 200, batch = "z0004_creative")
    public static void creativePlayersAreIgnored(GameTestHelper Helper) {
        Z0004Ship Ship = ship(Helper, new Vec3(32.0, 0.0, 32.0), 0.0f);
        Z0004Pirate Fighter = crew(Helper, new Vec3(10.5, 8.0, 10.5), Z0004Pirate.Cutlass);
        Scp538TestPlayer Builder = player(Helper, new Vec3(46.0, 8.0, 32.0), "z0004_builder", GameType.CREATIVE);
        Scp538TestPlayer Ghost = player(Helper, new Vec3(12.5, 8.0, 10.5), "z0004_ghost", GameType.SPECTATOR);
        Helper.assertFalse(Z0004Fight.targetable(Builder) || Z0004Fight.targetable(Ghost), "creative or spectator players count as targets");
        Helper.startSequence().thenIdle(160).thenExecute(() -> {
            try {
                Helper.assertTrue(Ship.shotsFired() == 0, "the ship fired at a creative player");
                Helper.assertTrue(Fighter.getTarget() == null, "the crewman targeted a creative or spectator player: " + Fighter.getTarget());
                Helper.assertTrue(Builder.getHealth() == Builder.getMaxHealth(), "the creative player was hurt");
            } finally {
                dismiss(Helper, Builder, Ghost);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Shore, timeoutTicks = 40, batch = "z0004_variants")
    public static void crewVariantsKeepTheirKit(GameTestHelper Helper) {
        try {
            for (int Variant = 0; Variant < Z0004Pirate.Variants; Variant++) {
                Z0004Pirate Made = crew(Helper, new Vec3(3.5 + Variant * 2.5, 1.0, 5.5), Variant);
                Helper.assertTrue(Made.variant() == Variant && Made.getMaxHealth() == (float) Z0004Pirate.VariantHealth[Variant], "variant " + Variant + " has the wrong stats");
                Helper.assertTrue(Made.melee() == (Variant == Z0004Pirate.Cutlass || Variant == Z0004Pirate.Hook), "variant " + Variant + " has the wrong weapon");
                Z0004Pirate Copy = Z0004Registry.Crew.get().create(Helper.getLevel());
                Copy.load(Made.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
                Helper.assertTrue(Copy.variant() == Variant, "variant " + Variant + " did not survive saving");
            }
            Z0004Captain Leader = Z0004Registry.Captain.get().create(Helper.getLevel());
            Helper.assertTrue(Leader.captain() && Leader.melee() && Leader.gunner() && Leader.getMaxHealth() == (float) Z0004Captain.CaptainHealth, "the captain is not a mini boss");
        } finally {
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Shore, timeoutTicks = 20, batch = "z0004_shop")
    public static void shopSellsTheChestWithItsFile(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Z0004Registry.DocumentId);
        Helper.assertTrue(Document != null, "z_0004 is not loaded");
        Helper.assertTrue(Document.Classification() == DocumentClass.ZClass, "Z-0004 is not Z-Class");
        Helper.assertTrue(Document.Condition() instanceof Z0004ProcuredCondition, "Z-0004 does not use the procured condition: " + Document.Condition().getTypeName());
        Helper.assertTrue(StartConditions.isRegistered(Z0004ProcuredCondition.TypeName), "z0004_procured is not registered");
        Helper.assertTrue(AnomalySpawners.spawnerFor(Z0004Registry.DocumentId).isEmpty(), "Z-0004 still has a courier spawner");
        ShopEntry Entry = ShopCatalog.get(Z0004Registry.location("z_0004"));
        Helper.assertTrue(Entry != null && Entry.Kind() == ShopKind.Item, "Z-0004 is not sold as an item in the shop");
        Helper.assertTrue(Entry.Goods().map(Stack -> Stack.is(Z0004Registry.ChestItem.get())).orElse(false), "the shop entry does not sell the chest");
        Helper.assertTrue(Entry.Document().map(Z0004Registry.DocumentId::equals).orElse(false), "the shop entry does not carry the Z-0004 file");
        Helper.assertTrue(Entry.Name().isEmpty() && Entry.Icon().isEmpty() && Entry.Extras().isEmpty(), "the shop entry has overrides the other Z entries do not use");
        List<ItemStack> Bought = delivered(Helper, Entry);
        Helper.assertTrue(Bought.stream().anyMatch(Stack -> Stack.is(Z0004Registry.ChestItem.get())), "buying Z-0004 did not hand over the chest");
        Helper.assertTrue(Bought.stream().anyMatch(Stack -> {
            DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
            return Stamp != null && Stamp.Document().equals(Z0004Registry.DocumentId);
        }), "buying Z-0004 did not hand over its file");
        Helper.succeed();
    }

    @GameTest(template = Shore, timeoutTicks = 20, batch = "z0004_creative_tabs")
    public static void creativeTabsListEveryItem(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        CreativeModeTab.ItemDisplayParameters Parameters = new CreativeModeTab.ItemDisplayParameters(World.enabledFeatures(), true, World.registryAccess());
        CreativeModeTab Blocks = ModCreativeTabs.ScpBlocks.get();
        CreativeModeTab Items = ModCreativeTabs.ScpItems.get();
        Blocks.buildContents(Parameters);
        Items.buildContents(Parameters);
        Helper.assertTrue(listed(Blocks, Z0004Registry.ChestItem.get()), "the chest is missing from SCP Blocks");
        for (Item Each : List.of(Z0004Registry.Cutlass.get(), Z0004Registry.CutlassEgg.get(), Z0004Registry.HookEgg.get(), Z0004Registry.PistolEgg.get(),
                Z0004Registry.BlunderbussEgg.get(), Z0004Registry.CaptainEgg.get())) {
            Helper.assertTrue(listed(Items, Each), BuiltInRegistries.ITEM.getKey(Each) + " is missing from SCP Items");
        }
        Helper.assertFalse(listed(Items, Z0004Registry.ChestItem.get()), "the chest is listed in SCP Items instead of SCP Blocks");
        Helper.succeed();
    }

    static boolean listed(CreativeModeTab Tab, Item Wanted) {
        return Tab.getDisplayItems().stream().anyMatch(Stack -> Stack.is(Wanted));
    }

    @SuppressWarnings("unchecked")
    static List<ItemStack> delivered(GameTestHelper Helper, ShopEntry Entry) {
        try {
            Method Prepare = ShopService.class.getDeclaredMethod("prepare", ServerLevel.class, ComputerBlockEntity.class, ShopEntry.class);
            Prepare.setAccessible(true);
            Object Delivery = Prepare.invoke(null, Helper.getLevel(), null, Entry);
            Helper.assertTrue(Delivery != null, "the shop could not prepare Z-0004");
            Method Stacks = Delivery.getClass().getDeclaredMethod("Stacks");
            Stacks.setAccessible(true);
            return (List<ItemStack>) Stacks.invoke(Delivery);
        } catch (ReflectiveOperationException Problem) {
            throw new IllegalStateException("could not inspect the shop delivery", Problem);
        }
    }
}

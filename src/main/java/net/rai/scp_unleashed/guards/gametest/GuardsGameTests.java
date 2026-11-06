package net.rai.scp_unleashed.guards.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import net.rai.scp_unleashed.dclass.duty.DClassManagerMenu;
import net.rai.scp_unleashed.dclass.duty.DClassRole;
import net.rai.scp_unleashed.dclass.duty.DClassTask;
import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.guards.GuardDuty;
import net.rai.scp_unleashed.guards.GuardOrders;
import net.rai.scp_unleashed.guards.GuardRules;
import net.rai.scp_unleashed.guards.GuardStatus;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.guards.PatrolConfigPayload;
import net.rai.scp_unleashed.guards.PatrolManagerBlockEntity;
import net.rai.scp_unleashed.guards.PatrolNodeBlock;
import net.rai.scp_unleashed.guards.PatrolPoint;
import net.rai.scp_unleashed.guards.PatrolRoute;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.inkwire.InkMarks;
import net.rai.scp_unleashed.security.TurretCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GuardsGameTests {
    public static final String Arena = "guards_arena";
    public static final float Sturdy = 400.0f;
    public static final int ShortWait = 10;
    public static final double Near = 1.0;
    public static final int DoorZ = 12;
    public static final int DoorX = 11;
    public static final int RoomWest = 8;
    public static final int RoomEast = 14;
    public static final int RoomSouth = 21;
    public static final int Inside = 17;

    private GuardsGameTests() {
    }

    private static int floor(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(23, Y, 23)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(23, Y + 1, 23)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void clean(GameTestHelper Helper) {
        AABB Area = Helper.getBounds().inflate(8.0);
        for (Entity Found : Helper.getLevel().getEntities((Entity) null, Area, Candidate -> !(Candidate instanceof Player))) {
            Found.discard();
        }
    }

    private static DClass guard(GameTestHelper Helper, double X, double Z) {
        DClass Guard = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(X, floor(Helper), Z));
        Guard.identify(DClassTrait.Stoic, DClassCrime.Smuggling, 0);
        Guard.setPersistenceRequired();
        return Guard;
    }

    private static Husk target(GameTestHelper Helper, double X, double Z) {
        Husk Target = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(X, floor(Helper), Z));
        Target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Target.setHealth(Sturdy);
        return Target;
    }

    private static PatrolRoute route(GameTestHelper Helper, List<BlockPos> Relative, boolean PingPong, GuardRules Rules, Optional<UUID> Owner) {
        List<PatrolPoint> Points = new ArrayList<>();
        for (BlockPos Spot : Relative) {
            Points.add(new PatrolPoint(Helper.absolutePos(Spot), ShortWait, Direction.NORTH));
        }
        return new PatrolRoute(UUID.randomUUID(), Helper.getLevel().dimension(), Helper.absolutePos(new BlockPos(0, floor(Helper), 0)), Points, PingPong, Rules, Owner, "");
    }

    private static double flat(GameTestHelper Helper, DClass Guard, BlockPos Relative) {
        Vec3 Spot = Vec3.atBottomCenterOf(Helper.absolutePos(Relative));
        return Math.sqrt(Math.pow(Guard.getX() - Spot.x, 2) + Math.pow(Guard.getZ() - Spot.z, 2));
    }

    private static int rounds(DClass Guard, ItemStack Ammo) {
        int Total = 0;
        for (ItemStack Stack : Guard.main()) {
            if (Stack.is(Ammo.getItem())) {
                Total += Stack.getCount();
            }
        }
        return Total;
    }

    private static InteractionResult useOn(GameTestHelper Helper, FakePlayer User, BlockPos Rel) {
        BlockPos Pos = Helper.absolutePos(Rel);
        return User.getMainHandItem().useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false)));
    }

    private static GuardRules rules(boolean HoldFire, boolean Melee, TurretCategory... Extra) {
        int Mask = TurretCategory.DefaultMask;
        for (TurretCategory Category : Extra) {
            Mask |= Category.bit();
        }
        return new GuardRules(Mask, List.of(), HoldFire, Melee);
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "guards_route")
    public static void followsRouteAfterBreaking(GameTestHelper Helper) {
        int F = floor(Helper);
        BlockPos ManagerSpot = new BlockPos(1, F, 1);
        List<BlockPos> Posts = List.of(new BlockPos(4, F, 4), new BlockPos(18, F, 4), new BlockPos(11, F, 18));
        Helper.setBlock(ManagerSpot, GuardsRegistry.PatrolManager.get());
        PatrolManagerBlockEntity Manager = (PatrolManagerBlockEntity) Helper.getBlockEntity(ManagerSpot);
        for (BlockPos Post : Posts) {
            Helper.setBlock(Post, GuardsRegistry.PatrolNode.get().defaultBlockState().setValue(PatrolNodeBlock.Facing, Direction.SOUTH));
            Manager.link(Helper.absolutePos(Post), Direction.SOUTH);
        }
        for (int Index = 0; Index < Posts.size(); Index++) {
            Manager.applyConfig(PatrolConfigPayload.ActionWait, Index, ShortWait - PatrolPoint.DefaultWait, "");
        }
        PatrolRoute Route = Manager.route();
        Helper.assertTrue(Route.size() == 3 && Route.point(1).Wait() == ShortWait, "the manager did not build the route");
        DClass Guard = guard(Helper, 4.5, 6.5);
        GuardDuty.assign(Guard, Route);
        boolean[] Seen = new boolean[Posts.size()];
        boolean[] After = new boolean[Posts.size()];
        boolean[] Broken = {false};
        Helper.onEachTick(() -> {
            for (int Index = 0; Index < Posts.size(); Index++) {
                if (flat(Helper, Guard, Posts.get(Index)) < Near) {
                    (Broken[0] ? After : Seen)[Index] = true;
                }
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Seen[1] && Seen[2], "the guard never walked the route"))
                .thenExecute(() -> {
                    for (BlockPos Post : Posts) {
                        Helper.setBlock(Post, Blocks.AIR);
                    }
                    Helper.setBlock(ManagerSpot, Blocks.AIR);
                    Broken[0] = true;
                })
                .thenWaitUntil(() -> Helper.assertTrue(After[0] && After[1] && After[2], "the guard lost its route once the posts were broken"))
                .thenExecute(() -> Helper.assertTrue(GuardDuty.has(Guard), "the guard lost its orders"))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "guards_ink")
    public static void patrolsThroughHiddenNodes(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos ManagerSpot = new BlockPos(1, F, 1);
        List<BlockPos> Posts = List.of(new BlockPos(4, F, 4), new BlockPos(12, F, 4));
        Helper.setBlock(ManagerSpot, GuardsRegistry.PatrolManager.get());
        PatrolManagerBlockEntity Manager = (PatrolManagerBlockEntity) Helper.getBlockEntity(ManagerSpot);
        FakePlayer Planner = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "patrol_planner"));
        Planner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GuardsRegistry.PatrolLinker.get()));
        Helper.assertTrue(useOn(Helper, Planner, ManagerSpot).consumesAction(), "the linker did not bind to the manager");
        for (BlockPos Post : Posts) {
            Helper.setBlock(Post, GuardsRegistry.PatrolNode.get().defaultBlockState().setValue(PatrolNodeBlock.Facing, Direction.SOUTH));
            Helper.assertTrue(InkMarks.toggleBlock(World, Helper.absolutePos(Post)) == InkMarks.Toggle.Hidden, "the patrol node did not take the ink");
            Helper.assertTrue(useOn(Helper, Planner, Post).consumesAction(), "the linker could not link a hidden patrol node");
        }
        Helper.assertTrue(Manager.getPoints().size() == Posts.size() && Manager.indexOf(Helper.absolutePos(Posts.get(1))) == 1, "the hidden patrol nodes are missing from the route");
        for (int Index = 0; Index < Posts.size(); Index++) {
            Manager.applyConfig(PatrolConfigPayload.ActionWait, Index, ShortWait - PatrolPoint.DefaultWait, "");
        }
        DClass Guard = guard(Helper, 4.5, 8.5);
        GuardDuty.assign(Guard, Manager.route());
        boolean[] Seen = new boolean[Posts.size()];
        Helper.onEachTick(() -> {
            for (int Index = 0; Index < Posts.size(); Index++) {
                if (flat(Helper, Guard, Posts.get(Index)) < Near) {
                    Seen[Index] = true;
                }
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Seen[0] && Seen[1], "the guard never walked the route through the hidden patrol nodes"))
                .thenExecute(() -> {
                    for (BlockPos Post : Posts) {
                        Helper.assertTrue(InkMarks.isHidden(Helper.getBlockState(Post)), "a patrol node lost its ink while the guard walked the route");
                    }
                })
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "guards_post")
    public static void standsGuard(GameTestHelper Helper) {
        int F = floor(Helper);
        BlockPos Post = new BlockPos(12, F, 12);
        DClass Guard = guard(Helper, 5.5, 12.5);
        GuardDuty.assign(Guard, route(Helper, List.of(Post), false, GuardRules.Default, Optional.empty()));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Guard, Post) < Near, "the guard never reached its post "))
                .thenExecuteFor(100, () -> Helper.assertTrue(flat(Helper, Guard, Post) < 1.6, "the guard wandered off its post"))
                .thenExecute(() -> {
                    GuardOrders Orders = GuardDuty.orders(Guard);
                    Helper.assertTrue(Orders != null && Orders.status() == GuardStatus.Standing, "the guard is not standing guard");
                })
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "guards_shoot")
    public static void shootsHostile(GameTestHelper Helper) {
        int F = floor(Helper);
        DClass Guard = guard(Helper, 6.5, 12.5);
        Guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunsRegistry.gunItem(GunKind.Pistol)));
        ItemStack Ammo = new ItemStack(GunKind.Pistol.getAmmo(), 24);
        Guard.main().set(5, Ammo.copy());
        GuardDuty.assign(Guard, route(Helper, List.of(new BlockPos(6, F, 12)), false, GuardRules.Default, Optional.empty()));
        Husk Target = target(Helper, 16.5, 12.5);
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    Helper.assertTrue(Target.getHealth() < Sturdy, "the guard never hurt the hostile");
                    Helper.assertTrue(Target.getLastHurtByMob() == Guard, "the damage was not the guard's");
                    Helper.assertTrue(rounds(Guard, Ammo) < 24, "the guard did not reload from its pack");
                })
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "guards_hold")
    public static void holdsFire(GameTestHelper Helper) {
        int F = floor(Helper);
        DClass Guard = guard(Helper, 6.5, 12.5);
        ItemStack Gun = GunItem.loaded(GunKind.Pistol);
        Guard.setItemSlot(EquipmentSlot.MAINHAND, Gun);
        ItemStack Ammo = new ItemStack(GunKind.Pistol.getAmmo(), 24);
        Guard.main().set(5, Ammo.copy());
        GuardDuty.assign(Guard, route(Helper, List.of(new BlockPos(6, F, 12)), false, rules(true, false), Optional.empty()));
        Husk Target = target(Helper, 16.5, 12.5);
        boolean[] Noticed = {false};
        Helper.onEachTick(() -> {
            GuardOrders Orders = GuardDuty.orders(Guard);
            if (Orders != null && Orders.status() == GuardStatus.Engaging) {
                Noticed[0] = true;
            }
        });
        Helper.startSequence()
                .thenIdle(240)
                .thenExecute(() -> {
                    Helper.assertTrue(Noticed[0], "the guard never noticed the hostile");
                    Helper.assertTrue(Target.getHealth() >= Sturdy, "the guard fired while holding fire");
                    Helper.assertTrue(GunItem.getAmmo(Guard.main().stream().filter(Stack -> Stack.getItem() instanceof GunItem).findFirst().orElse(Gun))
                            == GunKind.Pistol.getStats().getCapacity(), "rounds were spent while holding fire");
                    Helper.assertTrue(rounds(Guard, Ammo) == 24, "ammo was taken while holding fire");
                })
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "guards_melee")
    public static void fightsWithSword(GameTestHelper Helper) {
        int F = floor(Helper);
        DClass Guard = guard(Helper, 8.5, 12.5);
        Guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        GuardDuty.assign(Guard, route(Helper, List.of(new BlockPos(8, F, 12)), false, GuardRules.Default, Optional.empty()));
        Husk Target = target(Helper, 13.5, 12.5);
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    Helper.assertTrue(Target.getHealth() < Sturdy, "the guard never struck the hostile");
                    Helper.assertTrue(Target.getLastHurtByMob() == Guard, "the blow was not the guard's");
                    Helper.assertTrue(Guard.getMainHandItem().is(Items.IRON_SWORD), "the guard did not fight with its sword");
                })
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1000, batch = "guards_owner")
    public static void neverTargetsOwner(GameTestHelper Helper) {
        int F = floor(Helper);
        Visitor Owner = visitor(Helper, 14.5, 12.5, "patrol_owner");
        DClass Guard = guard(Helper, 6.5, 12.5);
        Guard.setItemSlot(EquipmentSlot.MAINHAND, GunItem.loaded(GunKind.Pistol));
        Guard.main().set(5, new ItemStack(GunKind.Pistol.getAmmo(), 24));
        List<BlockPos> Post = List.of(new BlockPos(6, F, 12));
        GuardDuty.assign(Guard, route(Helper, Post, false, rules(false, true, TurretCategory.Players), Optional.of(Owner.getUUID())));
        float Full = Owner.getHealth();
        Helper.startSequence()
                .thenExecuteFor(200, () -> {
                    Helper.assertTrue(Owner.getHealth() >= Full && Owner.Deaths == 0, "the guard shot its owner");
                    Helper.assertTrue(Guard.getTarget() != Owner, "the guard targeted its owner");
                })
                .thenExecute(() -> GuardDuty.assign(Guard, route(Helper, Post, false, rules(false, true, TurretCategory.Players), Optional.empty())))
                .thenWaitUntil(() -> Helper.assertTrue(Owner.getHealth() < Full || Owner.Deaths > 0, "the same player was never engaged once not the owner"))
                .thenExecute(() -> Helper.getLevel().removePlayerImmediately(Owner, Entity.RemovalReason.DISCARDED))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "guards_reload")
    public static void ordersSurviveReload(GameTestHelper Helper) {
        int F = floor(Helper);
        DClass Guard = guard(Helper, 4.5, 4.5);
        PatrolRoute Route = route(Helper, List.of(new BlockPos(4, F, 4), new BlockPos(18, F, 4), new BlockPos(11, F, 18)), true, rules(true, false, TurretCategory.DClass),
                Optional.of(UUID.randomUUID()));
        GuardDuty.assign(Guard, Route);
        GuardDuty.orders(Guard).setIndex(2);
        GuardDuty.orders(Guard).advance();
        int Index = GuardDuty.orders(Guard).index();
        boolean Reverse = GuardDuty.orders(Guard).reverse();
        CompoundTag Saved = Guard.saveWithoutId(new CompoundTag());
        DClass Copy = DClassRegistry.DClassType.get().create(Helper.getLevel());
        Helper.assertTrue(Copy != null, "no copy could be made");
        Copy.load(Saved);
        GuardOrders Loaded = GuardDuty.orders(Copy);
        Helper.assertTrue(Loaded != null, "orders were lost on reload");
        Helper.assertTrue(Loaded.route().equals(Route), "the route changed on reload");
        Helper.assertTrue(Loaded.index() == Index && Loaded.reverse() == Reverse, "the patrol position changed on reload");
        Helper.assertTrue(GuardDuty.clear(Copy) && !GuardDuty.has(Copy), "orders could not be cleared");
        clean(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "guards_armour")
    public static void wearsGuardArmour(GameTestHelper Helper) {
        int F = floor(Helper);
        DClass Plain = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(4.5, F, 4.5));
        DClass Armoured = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(8.5, F, 4.5));
        Armoured.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.SiteGuardCap.get()));
        Armoured.setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.SiteGuardShirt.get()));
        Armoured.setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.SiteGuardTrousers.get()));
        Armoured.setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.SiteGuardBoots.get()));
        Husk Attacker = target(Helper, 6.5, 8.5);
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    Helper.assertTrue(Armoured.getArmorValue() > 0, "site guard armour gives no armour on a D-Class");
                    DamageSource Blow = Helper.getLevel().damageSources().mobAttack(Attacker);
                    Plain.hurt(Blow, 10.0f);
                    Armoured.hurt(Blow, 10.0f);
                    Helper.assertTrue(Armoured.getHealth() > Plain.getHealth(), "site guard armour did not protect the D-Class");
                })
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "guards_duties")
    public static void oneJobAtATime(GameTestHelper Helper) {
        int F = floor(Helper);
        DClass Guard = guard(Helper, 4.5, 4.5);
        List<BlockPos> Post = List.of(new BlockPos(4, F, 4));
        GuardDuty.assign(Guard, route(Helper, Post, false, GuardRules.Default, Optional.empty()));
        Helper.assertTrue(GuardDuty.has(Guard), "patrol orders were not assigned");
        Helper.assertTrue(DClassDuties.assignRole(Guard, DClassRole.Janitor), "the janitor role was refused");
        Helper.assertTrue(DClassDuties.hasRole(Guard) && !GuardDuty.has(Guard), "a janitor role did not clear patrol orders");
        GuardDuty.assign(Guard, route(Helper, Post, false, GuardRules.Default, Optional.empty()));
        Helper.assertTrue(GuardDuty.has(Guard) && !DClassDuties.hasRole(Guard), "patrol orders did not clear the janitor role");
        Helper.assertTrue(DClassDuties.orderClean(Guard, null), "the cleaning task was refused");
        Helper.assertTrue(DClassDuties.hasTask(Guard) && !GuardDuty.has(Guard), "a cleaning task did not clear patrol orders");
        GuardDuty.assign(Guard, route(Helper, Post, false, GuardRules.Default, Optional.empty()));
        Helper.assertTrue(GuardDuty.has(Guard) && !DClassDuties.busy(Guard), "patrol orders did not clear the cleaning task");
        Visitor Officer = visitor(Helper, 6.5, 4.5, "duty_officer");
        DClassManagerMenu Sheet = new DClassManagerMenu(1, Officer.getInventory(), Guard);
        Helper.assertTrue(Sheet.patrolling(), "the duty sheet does not show the patrol");
        Helper.assertTrue(Sheet.clickMenuButton(Officer, DClassManagerMenu.CancelButton), "the duty sheet cancel did nothing");
        Helper.assertTrue(!GuardDuty.has(Guard) && !Sheet.patrolling(), "the duty sheet did not stand the guard down");
        Sheet.removed(Officer);
        Helper.getLevel().removePlayerImmediately(Officer, Entity.RemovalReason.DISCARDED);
        GuardDuty.assign(Guard, route(Helper, Post, false, GuardRules.Default, Optional.empty()));
        ItemStack Drink = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        Helper.assertTrue(DClassDuties.orderEat(Guard, null, Drink) == DClassDuties.Verdict.Accepted, "the guard refused a drink");
        Helper.assertTrue(GuardDuty.has(Guard) && DClassDuties.task(Guard) == DClassTask.EatThis, "a drink ended the patrol instead of pausing it");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(DClassDuties.task(Guard) == DClassTask.None, "the guard is still drinking"))
                .thenExecute(() -> Helper.assertTrue(GuardDuty.has(Guard), "the patrol did not resume after the drink"))
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Guard, Post.get(0)) < Near, "the guard never went back to its post"))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    private static void wall(GameTestHelper Helper, int F) {
        for (int Y = F; Y < F + 4; Y++) {
            for (int X = RoomWest; X <= RoomEast; X++) {
                Helper.setBlock(new BlockPos(X, Y, DoorZ), Blocks.STONE);
                Helper.setBlock(new BlockPos(X, Y, RoomSouth), Blocks.STONE);
            }
            for (int Z = DoorZ; Z <= RoomSouth; Z++) {
                Helper.setBlock(new BlockPos(RoomWest, Y, Z), Blocks.STONE);
                Helper.setBlock(new BlockPos(RoomEast, Y, Z), Blocks.STONE);
            }
        }
    }

    private static void panelDoor(GameTestHelper Helper, PanelDoorBlock Frame, BlockPos Origin, Direction Front) {
        ServerLevel World = Helper.getLevel();
        BlockPos Base = Helper.absolutePos(Origin);
        DoorKind Kind = Frame.getKind();
        BlockState State = Frame.defaultBlockState().setValue(PanelDoorBlock.Facing, Front);
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Base, Front, Kind.columnOf(Part), Kind.rowOf(Part));
            World.setBlock(Cell, State.setValue(Frame.partProperty(), Part), Block.UPDATE_ALL);
        }
    }

    private static DClass doorGuard(GameTestHelper Helper, int F) {
        DClass Guard = guard(Helper, DoorX + 0.5, 6.5);
        GuardDuty.assign(Guard, route(Helper, List.of(new BlockPos(DoorX, F, 6), new BlockPos(DoorX, F, Inside)), false, GuardRules.Default, Optional.empty()));
        return Guard;
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "guards_door_button")
    public static void opensButtonDoor(GameTestHelper Helper) {
        int F = floor(Helper);
        wall(Helper, F);
        panelDoor(Helper, (PanelDoorBlock) DoorRegistry.SingleSlidingDoor.get(), new BlockPos(DoorX, F, DoorZ), Direction.NORTH);
        Helper.setBlock(new BlockPos(DoorX + 1, F + 1, DoorZ - 1), DoorRegistry.DoorButton.get().defaultBlockState().setValue(DoorButtonBlock.FACING, Direction.NORTH));
        DClass Guard = doorGuard(Helper, F);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Guard, new BlockPos(DoorX, F, Inside)) < Near, "the guard never got through the button door"))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "guards_door_lever")
    public static void opensLeverDoor(GameTestHelper Helper) {
        int F = floor(Helper);
        wall(Helper, F);
        panelDoor(Helper, (PanelDoorBlock) DoorRegistry.RoomDoor.get(), new BlockPos(DoorX + 1, F, DoorZ), Direction.NORTH);
        BlockPos Lever = new BlockPos(DoorX, F + 2, DoorZ - 1);
        Helper.setBlock(Lever, DoorRegistry.DoorLever.get().defaultBlockState().setValue(DoorLeverBlock.FACE, AttachFace.WALL).setValue(DoorLeverBlock.FACING, Direction.NORTH));
        DClass Guard = doorGuard(Helper, F);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Guard, new BlockPos(DoorX, F, Inside)) < Near, "the guard never got through the lever door"))
                .thenExecute(() -> Helper.assertTrue(Helper.getBlockState(Lever).getValue(DoorLeverBlock.Powered), "the lever was flipped back"))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    private static void scanner(GameTestHelper Helper, int F) {
        Helper.setBlock(new BlockPos(DoorX + 1, F + 1, DoorZ - 1), DoorRegistry.KeycardScanner.get().defaultBlockState()
                .setValue(KeycardScannerBlock.FACING, Direction.NORTH).setValue(KeycardScannerBlock.Clearance, 2));
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "guards_door_card")
    public static void swipesKeycard(GameTestHelper Helper) {
        int F = floor(Helper);
        wall(Helper, F);
        panelDoor(Helper, (PanelDoorBlock) DoorRegistry.SingleSlidingDoor.get(), new BlockPos(DoorX, F, DoorZ), Direction.NORTH);
        scanner(Helper, F);
        DClass Guard = doorGuard(Helper, F);
        Guard.main().set(4, new ItemStack(DoorRegistry.KeycardLevel1.get()));
        Guard.main().set(6, new ItemStack(DoorRegistry.KeycardLevel3.get()));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Guard, new BlockPos(DoorX, F, Inside)) < Near, "the guard never swiped through the keycard door"))
                .thenExecute(() -> Helper.assertTrue(Guard.main().get(6).is(DoorRegistry.KeycardLevel3.get()) && Guard.main().get(4).is(DoorRegistry.KeycardLevel1.get()),
                        "the guard lost a keycard"))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "guards_door_denied")
    public static void staysOutWithoutCard(GameTestHelper Helper) {
        int F = floor(Helper);
        wall(Helper, F);
        panelDoor(Helper, (PanelDoorBlock) DoorRegistry.SingleSlidingDoor.get(), new BlockPos(DoorX, F, DoorZ), Direction.NORTH);
        scanner(Helper, F);
        DClass Guard = doorGuard(Helper, F);
        Guard.main().set(4, new ItemStack(DoorRegistry.KeycardLevel1.get()));
        boolean[] Blocked = {false};
        Helper.onEachTick(() -> {
            Vec3 Spot = Helper.relativeVec(Guard.position());
            boolean Inside = Spot.x > RoomWest + 1 && Spot.x < RoomEast && Spot.z > DoorZ + 1 && Spot.z < RoomSouth;
            Helper.assertTrue(!Inside, "the guard got through a door it has no card for");
            GuardOrders Orders = GuardDuty.orders(Guard);
            if (Orders != null && Orders.status() == GuardStatus.Blocked) {
                Blocked[0] = true;
            }
        });
        Helper.startSequence()
                .thenIdle(600)
                .thenExecute(() -> Helper.assertTrue(Blocked[0], "the guard never reported the door as blocked"))
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    private static Visitor visitor(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Visitor User = new Visitor(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, floor(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    public static final class Visitor extends FakePlayer {
        public int Deaths;

        public Visitor(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
            ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, this, 0, "spawnInvulnerableTime");
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return isRemoved() || getAbilities().invulnerable && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }

        @Override
        public void die(DamageSource Source) {
            if (CommonHooks.onLivingDeath(this, Source)) {
                return;
            }
            Deaths++;
            setHealth(getMaxHealth());
        }
    }
}

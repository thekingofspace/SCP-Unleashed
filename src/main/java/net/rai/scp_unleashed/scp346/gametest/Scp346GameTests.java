package net.rai.scp_unleashed.scp346.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
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
import net.rai.scp_unleashed.scp346.Scp346Flock;
import net.rai.scp_unleashed.scp346.Scp346FlockCondition;
import net.rai.scp_unleashed.scp346.Scp346Pterosaur;
import net.rai.scp_unleashed.scp346.Scp346Registry;
import net.rai.scp_unleashed.scp346.Scp346Variant;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp346GameTests {
    public static final String Arena = "scp346_arena";
    public static final double Floor = 2.0;
    public static final double SearchRadius = 64.0;

    private Scp346GameTests() {
    }

    private static Scp346Pterosaur spawn(GameTestHelper Helper, double X, double Z) {
        return Helper.spawn(Scp346Registry.Pterosaur.get(), new Vec3(X, Floor, Z));
    }

    private static ServerPlayer person(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void moveTo(GameTestHelper Helper, ServerPlayer User, double X, double Z) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
    }

    private static List<Scp346Pterosaur> around(GameTestHelper Helper, Predicate<Scp346Pterosaur> Wanted) {
        Vec3 Centre = Helper.absoluteVec(new Vec3(16.0, Floor, 16.0));
        return Helper.getLevel().getEntitiesOfClass(Scp346Pterosaur.class, new AABB(Centre, Centre).inflate(SearchRadius), Wanted);
    }

    private static void tidy(GameTestHelper Helper, List<? extends Entity> Spawned, ServerPlayer... Users) {
        for (Entity Made : Spawned) {
            Made.discard();
        }
        for (ServerPlayer User : Users) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static boolean byPterosaur(@Nullable LivingEntity Attacker) {
        return Attacker instanceof Scp346Pterosaur;
    }

    private static String state(Scp346Pterosaur Bird) {
        return " [variant " + Bird.getVariant().getSerializedName() + ", tame " + Bird.isTame() + ", sitting " + Bird.isOrderedToSit() + ", flying " + Bird.isFlying()
                + ", action " + Bird.action() + ", at " + Bird.blockPosition() + "]";
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp346_flockArrives")
    public static void flockConditionSpawnsFour(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Reader = person(Helper, 16.5, 26.5, "scp346_reader");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp346Registry.DocumentId);
        Helper.assertTrue(Document != null, "the scp_346 dossier did not load");
        BlockPos Spot = Helper.absolutePos(new BlockPos(16, 2, 10));
        boolean Spawned = Scp346FlockCondition.manifest(new AnomalySpawners.Discovery(World, Spot, Reader, Scp346Registry.DocumentId, Document, true));
        Helper.assertTrue(Spawned, "the flock condition spawned nothing");
        List<Scp346Pterosaur> Flock = around(Helper, Bird -> Spot.equals(Bird.home()));
        Helper.assertTrue(Flock.size() == Scp346FlockCondition.FlockSize, "the flock condition spawned " + Flock.size() + " instances, not " + Scp346FlockCondition.FlockSize);
        double Farthest = 0.0;
        for (Scp346Pterosaur Bird : Flock) {
            Helper.assertTrue(List.of(Scp346Variant.values()).contains(Bird.getVariant()), "an instance has no valid colouring" + state(Bird));
            Helper.assertTrue(Bird.arrival() != null, "an instance has nowhere to land" + state(Bird));
            Helper.assertTrue(Bird.isFlying(), "an instance did not arrive on the wing" + state(Bird));
            Helper.assertTrue(Bird.isPersistenceRequired(), "an arriving instance could despawn" + state(Bird));
            Farthest = Math.max(Farthest, Bird.position().distanceTo(Vec3.atBottomCenterOf(Spot)));
        }
        Helper.assertTrue(Farthest > 6.0, "the flock appeared on top of its landing spot instead of flying in (" + Farthest + " blocks)");
        tidy(Helper, Flock, Reader);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp346_reload")
    public static void variantsSurviveReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Owner = person(Helper, 4.5, 4.5, "scp346_keeper");
        List<Scp346Pterosaur> Spawned = new ArrayList<>();
        int Index = 0;
        for (Scp346Variant Variant : Scp346Variant.values()) {
            Scp346Pterosaur Bird = spawn(Helper, 6.5 + Index * 2, 8.5);
            Spawned.add(Bird);
            Bird.setVariant(Variant);
            boolean Tamed = Index % 2 == 0;
            if (Tamed) {
                Bird.tame(Owner);
                Bird.setOrderedToSit(true);
            }
            CompoundTag Saved = new CompoundTag();
            Helper.assertTrue(Bird.save(Saved), "SCP-346 refused to save" + state(Bird));
            Optional<Entity> Copy = EntityType.create(Saved, World);
            Helper.assertTrue(Copy.isPresent() && Copy.get() instanceof Scp346Pterosaur, "SCP-346 could not be rebuilt from its save data");
            Scp346Pterosaur Loaded = (Scp346Pterosaur) Copy.get();
            Helper.assertTrue(Loaded.getVariant() == Variant, "the " + Variant.getSerializedName() + " colouring came back as " + Loaded.getVariant().getSerializedName());
            Helper.assertTrue(Loaded.isTame() == Tamed, "the tame state did not survive a reload" + state(Loaded));
            if (Tamed) {
                Helper.assertTrue(Owner.getUUID().equals(Loaded.getOwnerUUID()), "the owner did not survive a reload" + state(Loaded));
                Helper.assertTrue(Loaded.isOrderedToSit(), "the sit order did not survive a reload" + state(Loaded));
            }
            Loaded.discard();
            Index++;
        }
        tidy(Helper, Spawned, Owner);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp346_lands")
    public static void calledFlockLands(GameTestHelper Helper) {
        Vec3 Spot = Helper.absoluteVec(new Vec3(16.5, Floor, 16.5));
        List<Scp346Pterosaur> Flock = Scp346Flock.spawnFlock(Helper.getLevel(), Spot, 4, Scp346Variant.Pale, null, false);
        Helper.assertTrue(Flock.size() == 4, "the flock call made " + Flock.size() + " instances");
        for (Scp346Pterosaur Bird : Flock) {
            Bird.setInvulnerable(true);
            Helper.assertTrue(Bird.getVariant() == Scp346Variant.Pale, "a called instance ignored the requested colouring" + state(Bird));
        }
        Helper.succeedWhen(() -> {
            for (Scp346Pterosaur Bird : Flock) {
                Helper.assertTrue(!Bird.isFlying() && Bird.onGround(), "an instance has not landed yet" + state(Bird));
                Helper.assertTrue(Bird.position().distanceTo(Spot) < 4.0, "an instance landed far from the call" + state(Bird));
            }
            tidy(Helper, Flock);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp346_tame")
    public static void fishTamesAndFollows(GameTestHelper Helper) {
        ServerPlayer Owner = person(Helper, 6.5, 16.5, "scp346_feeder");
        Scp346Pterosaur Bird = spawn(Helper, 8.5, 16.5);
        Bird.setInvulnerable(true);
        int Fed = 0;
        while (!Bird.isTame() && Fed < 80) {
            Owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Fed % 2 == 0 ? Items.COD : Items.SALMON, 2));
            Bird.mobInteract(Owner, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Owner.getMainHandItem().getCount() == 1, "feeding SCP-346 did not use up a fish");
            Fed++;
        }
        Helper.assertTrue(Bird.isTame(), "80 fish never tamed SCP-346" + state(Bird));
        Helper.assertTrue(Owner.getUUID().equals(Bird.getOwnerUUID()), "SCP-346 was tamed to the wrong owner" + state(Bird));
        Owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT_SEEDS));
        Bird.mobInteract(Owner, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Owner.getMainHandItem().getCount() == 1, "SCP-346 ate seeds");
        moveTo(Helper, Owner, 27.5, 16.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Bird.distanceTo(Owner) < 5.0, "the tamed SCP-346 did not follow its owner" + state(Bird) + " owner at " + Owner.blockPosition());
            tidy(Helper, List.of(Bird), Owner);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp346_sit")
    public static void sitToggle(GameTestHelper Helper) {
        ServerPlayer Owner = person(Helper, 14.5, 16.5, "scp346_sitter");
        Scp346Pterosaur Bird = spawn(Helper, 16.5, 16.5);
        Bird.setInvulnerable(true);
        Bird.tame(Owner);
        Owner.setShiftKeyDown(true);
        Owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Bird.mobInteract(Owner, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Bird.isOrderedToSit() && Bird.isInSittingPose(), "sneak-using a tamed SCP-346 did not make it sit" + state(Bird));
        Vec3 Start = Bird.position();
        moveTo(Helper, Owner, 30.5, 30.5);
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Bird.isOrderedToSit(), "SCP-346 stopped sitting on its own" + state(Bird));
            Helper.assertTrue(Bird.position().distanceTo(Start) < 1.0, "a sitting SCP-346 wandered off" + state(Bird));
            moveTo(Helper, Owner, 14.5, 16.5);
            Bird.mobInteract(Owner, InteractionHand.MAIN_HAND);
            Helper.assertTrue(!Bird.isOrderedToSit() && !Bird.isInSittingPose(), "sneak-using a sitting SCP-346 did not stand it up" + state(Bird));
            Owner.setShiftKeyDown(false);
            tidy(Helper, List.of(Bird), Owner);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp346_stasis")
    public static void stasisKeepsVariantAndOwner(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer Owner = person(Helper, 14.5, 16.5, "scp346_handler");
        ServerPlayer Stranger = person(Helper, 18.5, 16.5, "scp346_stranger");
        Scp346Pterosaur Bird = spawn(Helper, 16.5, 16.5);
        Bird.setInvulnerable(true);
        Bird.setVariant(Scp346Variant.Rusty);
        Bird.tame(Owner);
        Bird.setOrderedToSit(true);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(Stranger, Pod, Bird) != null, "a stranger could pod someone else's SCP-346");
        Helper.assertTrue(StasisPods.refusal(Owner, Pod, Bird) == null, "a pod refuses SCP-346 for its owner");
        Helper.assertTrue(StasisPods.capture(World, Owner, Pod, Bird), "the pod failed to capture SCP-346");
        Helper.assertTrue(Bird.isRemoved(), "SCP-346 is still in the world after capture");
        Helper.assertTrue(StasisPodItem.isOccupied(Pod), "the pod holds nothing");
        Owner.setItemInHand(InteractionHand.MAIN_HAND, Pod);
        BlockPos Ground = Helper.absolutePos(new BlockPos(16, 1, 20));
        UseOnContext Context = new UseOnContext(Owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Ground).add(0.0, 0.5, 0.0), Direction.UP, Ground, false));
        Helper.assertTrue(StasisPods.release(Context, Pod), "the pod failed to release SCP-346");
        List<Scp346Pterosaur> Released = around(Helper, Found -> Owner.getUUID().equals(Found.getOwnerUUID()));
        Helper.assertTrue(Released.size() == 1, "expected one released SCP-346, found " + Released.size());
        Scp346Pterosaur Back = Released.get(0);
        Helper.assertTrue(Back.getVariant() == Scp346Variant.Rusty, "the colouring changed in stasis" + state(Back));
        Helper.assertTrue(Back.isTame() && Owner.getUUID().equals(Back.getOwnerUUID()), "the owner was lost in stasis" + state(Back));
        Helper.assertTrue(Back.isOrderedToSit(), "the sit order was lost in stasis" + state(Back));
        tidy(Helper, Released, Owner, Stranger);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp346_peaceful")
    public static void neverAttacks(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = person(Helper, 18.5, 16.5, "scp346_poker");
        Pig Pig0 = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(13.5, Floor, 16.5));
        Scp346Pterosaur[] Flock = new Scp346Pterosaur[4];
        for (int Index = 0; Index < Flock.length; Index++) {
            Flock[Index] = spawn(Helper, 15.0 + Index, 15.5 + (Index % 2));
        }
        float Health = User.getHealth();
        Scp346Pterosaur Poked = Flock[0];
        Poked.hurt(World.damageSources().playerAttack(User), 1.0f);
        Helper.startSequence()
                .thenExecuteAfter(30, () -> Helper.assertTrue(Poked.isFlying() || Poked.panic() > 0 && Poked.action() == Scp346Pterosaur.ActionTakeoff,
                        "a hurt SCP-346 did not try to flee" + state(Poked)))
                .thenExecuteAfter(170, () -> {
                    Helper.assertTrue(User.getHealth() >= Health || !byPterosaur(User.getLastHurtByMob()), "SCP-346 hurt the player");
                    Helper.assertTrue(!byPterosaur(Pig0.getLastHurtByMob()), "SCP-346 hurt the pig");
                    for (Scp346Pterosaur Bird : Flock) {
                        Helper.assertTrue(Bird.getTarget() == null, "an SCP-346 picked a target" + state(Bird));
                        Helper.assertTrue(Bird.getLastHurtMob() == null, "an SCP-346 attacked something" + state(Bird));
                    }
                    Pig0.discard();
                    tidy(Helper, List.of(Flock), User);
                })
                .thenSucceed();
    }
}

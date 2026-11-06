package net.rai.scp_unleashed.rift.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;
import net.rai.scp_unleashed.space.SpaceRegistry;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class RiftGameTests {
    public static final String Arena = "rift_arena";
    public static final String VisitorName = "rift_visitor";
    public static final double SweepRadius = 96.0;
    private static final ResourceKey<Level> StandIn = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rift_test_stand_in"));

    private RiftGameTests() {
    }

    private static FakePlayer visitor(ServerLevel World, Vec3 Spot) {
        FakePlayer Visitor = FakePlayerFactory.get(World, new GameProfile(UUID.nameUUIDFromBytes(VisitorName.getBytes(StandardCharsets.UTF_8)), VisitorName));
        Visitor.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        return Visitor;
    }

    private static void sweep(ServerLevel World, Vec3 Centre) {
        World.getEntitiesOfClass(RiftEntity.class, new AABB(Centre, Centre).inflate(SweepRadius), RiftEntity::isAlive).forEach(Entity::discard);
    }

    private static void refused(GameTestHelper Helper, ServerLevel Other) {
        String Name = Other.dimension().location().toString();
        Helper.assertTrue(!RiftFormation.allowedIn(Other), Name + " allows rifts");
        Vec3 Spot = new Vec3(0.5, Other.getMinBuildHeight() + 64.0, 0.5);
        FakePlayer Visitor = visitor(Other, Spot);
        long Now = Other.getGameTime();
        Visitor.getPersistentData().putLong(RiftFormation.TimerKey, Now - 1L);
        Helper.assertTrue(!RiftFormation.tickPlayer(Other, Visitor, Now), "natural formation ran in " + Name);
        Helper.assertTrue(Visitor.getPersistentData().getLong(RiftFormation.TimerKey) == Now - 1L, "the formation timer ticked in " + Name);
        Helper.assertTrue(!RiftFormation.tryForm(Other, Visitor), "a rift formed near a player in " + Name);
        Helper.assertTrue(!RiftFormation.formAround(Other, Spot, 2.0, 4.0), "a rift formed around a point in " + Name);
        RiftEntity Stray = RiftRegistry.Rift.get().create(Other);
        Helper.assertTrue(Stray != null, "could not build a rift for " + Name);
        Stray.setRarity(FragmentRarity.Volatile);
        Stray.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.assertTrue(!Other.addFreshEntity(Stray), "a new rift was let into " + Name);
        ApocalypticRiftEntity Golden = RiftTechRegistry.ApocalypticRift.get().create(Other);
        Helper.assertTrue(Golden != null, "could not build a golden rift for " + Name);
        Golden.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.assertTrue(!Other.addFreshEntity(Golden), "a new golden rift was let into " + Name);
        closes(Helper, RiftRegistry.Rift.get().create(Other), Name);
        closes(Helper, RiftTechRegistry.ApocalypticRift.get().create(Other), Name);
    }

    private static void closes(GameTestHelper Helper, RiftEntity Saved, String Name) {
        Helper.assertTrue(Saved != null, "could not build a saved rift for " + Name);
        Saved.tick();
        Helper.assertTrue(Saved.isCollapsing() || Saved.isRemoved(), "a saved rift in " + Name + " did not start closing");
        for (int Tick = 0; Tick <= RiftEntity.CollapseTicks && !Saved.isRemoved(); Tick++) {
            Saved.tick();
        }
        Helper.assertTrue(Saved.isRemoved(), "a saved rift in " + Name + " never closed");
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "rift_overworld_only", skyAccess = true)
    public static void riftOverworldOnly(GameTestHelper Helper) {
        ServerLevel Overworld = Helper.getLevel();
        MinecraftServer Server = Overworld.getServer();
        Helper.assertTrue(Overworld.dimension().equals(Level.OVERWORLD), "the test level is not the overworld");
        Helper.assertTrue(RiftFormation.allowedIn(Level.OVERWORLD) && RiftFormation.allowedIn(Overworld), "the overworld refuses rifts");
        for (ResourceKey<Level> Key : List.of(Level.NETHER, Level.END, SpaceRegistry.MoonLevel, StandIn)) {
            Helper.assertTrue(!RiftFormation.allowedIn(Key), Key.location() + " allows rifts");
        }
        int Checked = 0;
        for (ServerLevel Other : Server.getAllLevels()) {
            if (!Other.dimension().equals(Level.OVERWORLD)) {
                refused(Helper, Other);
                Checked++;
            }
        }
        Helper.assertTrue(Server.getLevel(Level.NETHER) == null || Checked >= 1, "no other dimension was checked");
        Vec3 Centre = Helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        sweep(Overworld, Centre);
        FakePlayer Visitor = visitor(Overworld, Centre);
        long Now = Overworld.getGameTime();
        Visitor.getPersistentData().putLong(RiftFormation.TimerKey, Now - 1L);
        Helper.assertTrue(RiftFormation.tickPlayer(Overworld, Visitor, Now), "natural formation did not run in the overworld");
        Helper.assertTrue(Visitor.getPersistentData().getLong(RiftFormation.TimerKey) > Now, "the overworld formation timer was not rescheduled");
        sweep(Overworld, Centre);
        Helper.assertTrue(RiftFormation.formAround(Overworld, Centre, 2.0, 3.0), "no rift formed in the overworld arena");
        List<RiftEntity> Formed = Overworld.getEntitiesOfClass(RiftEntity.class, Helper.getBounds(), RiftEntity::isAlive);
        Helper.assertTrue(Formed.size() == 1, "expected one overworld rift in the arena, found " + Formed.size());
        RiftEntity Kept = Formed.get(0);
        Helper.runAfterDelay(20, () -> {
            Helper.assertTrue(Kept.isAlive() && !Kept.isCollapsing(), "an overworld rift closed by itself");
            sweep(Overworld, Centre);
            Helper.succeed();
        });
    }
}

package net.rai.scp_unleashed.rift.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.rift.RiftRegistry;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class RiftRuleGameTests {
    public static final String Arena = "rift_arena";
    public static final String VisitorName = "rift_rule_visitor";
    public static final double SweepRadius = 96.0;

    private RiftRuleGameTests() {
    }

    private static void sweep(ServerLevel World, Vec3 Centre) {
        World.getEntitiesOfClass(RiftEntity.class, new AABB(Centre, Centre).inflate(SweepRadius), RiftEntity::isAlive).forEach(Entity::discard);
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "rift_spawningRule", skyAccess = true)
    public static void spawningRule(GameTestHelper Helper) {
        ServerLevel Overworld = Helper.getLevel();
        MinecraftServer Server = Overworld.getServer();
        Helper.assertTrue(RiftRegistry.SpawningRule != null, "the scpRiftSpawning game rule was never registered");
        GameRules.BooleanValue Rule = Overworld.getGameRules().getRule(RiftRegistry.SpawningRule);
        Helper.assertTrue(Rule.get(), "natural rift spawning is off by default");
        Helper.assertTrue(RiftRegistry.SpawningRuleName.equals(RiftRegistry.SpawningRule.getId()), "the game rule has the wrong name: " + RiftRegistry.SpawningRule.getId());
        Vec3 Centre = Helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        sweep(Overworld, Centre);
        FakePlayer Visitor = FakePlayerFactory.get(Overworld, new GameProfile(UUID.nameUUIDFromBytes(VisitorName.getBytes(StandardCharsets.UTF_8)), VisitorName));
        Visitor.moveTo(Centre.x, Centre.y, Centre.z, 0.0f, 0.0f);
        Helper.assertTrue(RiftFormation.formAround(Overworld, Centre, 2.0, 3.0), "no rift formed to test with");
        List<RiftEntity> Formed = Overworld.getEntitiesOfClass(RiftEntity.class, Helper.getBounds(), RiftEntity::isAlive);
        Helper.assertTrue(Formed.size() == 1, "expected one rift in the arena, found " + Formed.size());
        RiftEntity Kept = Formed.get(0);
        long Now = Overworld.getGameTime();
        Rule.set(false, Server);
        try {
            Visitor.getPersistentData().putLong(RiftFormation.TimerKey, Now - 1L);
            Helper.assertFalse(RiftFormation.tickPlayer(Overworld, Visitor, Now), "a rift formed naturally with scpRiftSpawning off");
            int Count = Overworld.getEntitiesOfClass(RiftEntity.class, new AABB(Centre, Centre).inflate(SweepRadius), RiftEntity::isAlive).size();
            Helper.assertTrue(Count == 1, "the rift count changed with scpRiftSpawning off: " + Count);
        } catch (RuntimeException Failure) {
            Rule.set(true, Server);
            throw Failure;
        }
        Helper.runAfterDelay(20, () -> {
            try {
                Helper.assertTrue(Kept.isAlive() && !Kept.isCollapsing(), "an existing rift closed when natural spawning was turned off");
            } finally {
                Rule.set(true, Server);
            }
            Helper.assertTrue(RiftFormation.tickPlayer(Overworld, Visitor, Overworld.getGameTime()), "natural formation did not resume with scpRiftSpawning back on");
            sweep(Overworld, Centre);
            Helper.succeed();
        });
    }
}

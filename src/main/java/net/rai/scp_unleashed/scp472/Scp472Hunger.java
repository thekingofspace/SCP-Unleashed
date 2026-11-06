package net.rai.scp_unleashed.scp472;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp472Hunger {
    public static final int BodyCycle = 5;
    public static final int RemarkMin = 600;
    public static final int RemarkMax = 1100;
    public static final int RemarkLines = 6;
    public static final int GlanceMin = 60;
    public static final int GlanceMax = 160;
    public static final String RemarkPrefix = "message.scp_unleashed.scp472.hungry.";
    private static final Map<UUID, Pace> Paces = new HashMap<>();
    private static final Map<ServerLevel, Set<UUID>> Bodies = new WeakHashMap<>();

    private Scp472Hunger() {
    }

    private static final class Pace {
        private long NextRemark;
        private int LastLine = -1;
        private long NextGlance;
    }

    public static boolean isSubject(LivingEntity Subject) {
        if (!Subject.isAlive() || Subject.isSpectator() || !Targeting.canWitness(Subject)) {
            return false;
        }
        if (Subject instanceof ServerPlayer User) {
            return !User.isCreative();
        }
        return Subject instanceof Mob && ControlBodies.isPersonLike(Subject);
    }

    public static Vec3 centre(Entity Subject) {
        return Subject.getBoundingBox().getCenter();
    }

    public static DamageSource source(LivingEntity Subject) {
        return Subject.damageSources().source(Scp472Registry.ConsumedDamage);
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        List<BlockPos> Stones = Scp472Stones.of(World);
        long Now = World.getGameTime();
        for (ServerPlayer User : List.copyOf(World.players())) {
            tickPlayer(World, User, Stones, Now);
        }
        if (Now % BodyCycle == 0) {
            tickBodies(World, Stones, Now);
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && !(Event.getEntity() instanceof ServerPlayer) && Event.getEntity() instanceof LivingEntity Living
                && Scp472Mind.isTouched(Living)) {
            Bodies.computeIfAbsent(World, Key -> new HashSet<>()).add(Living.getUUID());
        }
    }

    public static void tickPlayer(ServerLevel World, ServerPlayer User, List<BlockPos> Stones, long Now) {
        if (!User.isAlive()) {
            return;
        }
        Scp472Stones.Nearest Found = Stones.isEmpty() || !isSubject(User) ? null : Scp472Stones.nearest(World, Stones, centre(User), Scp472Mind.Reach);
        if (Found == null) {
            release(User);
            return;
        }
        float Before = Scp472Mind.sanity(User);
        float After = Math.max(0.0f, Before - Scp472Mind.drainPerSecond(Found.Distance()) / 20.0f);
        Scp472Mind.store(User, After);
        int Stage = Scp472Mind.stage(After);
        remark(User, Scp472Mind.stage(Before), Stage, Now);
        if (Stage >= Scp472Mind.HarmStage && Math.floorMod(Now + User.getId(), Scp472Mind.HarmInterval) == 0) {
            User.hurt(source(User), Scp472Mind.harm(After));
        }
        Scp472Sync.mind(User, false);
    }

    public static void release(ServerPlayer User) {
        Paces.remove(User.getUUID());
        if (Scp472Mind.isTouched(User)) {
            Scp472Mind.forget(User);
            Scp472Sync.mind(User, true);
        } else {
            Scp472Sync.mind(User, false);
        }
    }

    private static void remark(ServerPlayer User, int Before, int Stage, long Now) {
        if (Stage < Scp472Mind.RemarkStage) {
            return;
        }
        Pace State = Paces.computeIfAbsent(User.getUUID(), Key -> new Pace());
        boolean Entered = Before < Scp472Mind.RemarkStage;
        if (!Entered && Now < State.NextRemark) {
            return;
        }
        RandomSource Random = User.getRandom();
        int Line = Random.nextInt(RemarkLines);
        if (Line == State.LastLine) {
            Line = (Line + 1 + Random.nextInt(RemarkLines - 1)) % RemarkLines;
        }
        State.LastLine = Line;
        State.NextRemark = Now + Mth.nextInt(Random, RemarkMin, RemarkMax);
        User.displayClientMessage(Component.translatable(RemarkPrefix + Line).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
    }

    private static void tickBodies(ServerLevel World, List<BlockPos> Stones, long Now) {
        Set<UUID> Known = Bodies.computeIfAbsent(World, Key -> new HashSet<>());
        Set<UUID> Seen = new HashSet<>();
        for (BlockPos Stone : Stones) {
            if (!World.isLoaded(Stone)) {
                continue;
            }
            AABB Area = new AABB(Stone).inflate(Scp472Mind.Reach);
            for (Mob Body : World.getEntitiesOfClass(Mob.class, Area, Scp472Hunger::isSubject)) {
                if (Seen.add(Body.getUUID())) {
                    tickBody(World, Body, Stones, Now);
                    if (Scp472Mind.isTouched(Body)) {
                        Known.add(Body.getUUID());
                    }
                }
            }
        }
        Iterator<UUID> Each = Known.iterator();
        while (Each.hasNext()) {
            UUID Id = Each.next();
            if (Seen.contains(Id)) {
                continue;
            }
            Entity Found = World.getEntity(Id);
            if (Found instanceof LivingEntity Living) {
                Scp472Mind.forget(Living);
            }
            Paces.remove(Id);
            Each.remove();
        }
    }

    private static void tickBody(ServerLevel World, Mob Body, List<BlockPos> Stones, long Now) {
        Scp472Stones.Nearest Found = Scp472Stones.nearest(World, Stones, centre(Body), Scp472Mind.Reach);
        if (Found == null) {
            Scp472Mind.forget(Body);
            Paces.remove(Body.getUUID());
            return;
        }
        float After = Math.max(0.0f, Scp472Mind.sanity(Body) - Scp472Mind.drainPerSecond(Found.Distance()) * BodyCycle / 20.0f);
        Scp472Mind.store(Body, After);
        int Stage = Scp472Mind.stage(After);
        if (Stage >= Scp472Mind.VisionStage) {
            Pace State = Paces.computeIfAbsent(Body.getUUID(), Key -> new Pace());
            if (Now >= State.NextGlance) {
                State.NextGlance = Now + Mth.nextInt(Body.getRandom(), GlanceMin, GlanceMax);
                Vec3 Centre = Found.centre();
                Body.getLookControl().setLookAt(Centre.x, Centre.y, Centre.z);
            }
        }
        if (Stage >= Scp472Mind.HarmStage && Math.floorMod(Now + Body.getId(), Scp472Mind.HarmInterval) < BodyCycle) {
            Body.hurt(source(Body), Scp472Mind.harm(After));
        }
    }

    public static void forgetAll() {
        Paces.clear();
        Bodies.clear();
    }
}

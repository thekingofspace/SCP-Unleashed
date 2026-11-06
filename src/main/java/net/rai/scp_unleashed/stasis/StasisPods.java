package net.rai.scp_unleashed.stasis;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class StasisPods {
    public static final double NoticeRange = 16.0;
    public static final int RoomSearch = 2;
    private static final Logger Log = LogUtils.getLogger();
    private static final List<Predicate<Entity>> Vetoes = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<ServerLevel, Entity>> ReleaseHooks = new CopyOnWriteArrayList<>();

    private static final List<CaptureHook> CaptureHooks = new CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface CaptureHook {
        void captured(ServerLevel World, Player User, Mob Target, CompoundTag Data);
    }

    private StasisPods() {
    }

    public static void addCaptureHook(CaptureHook Hook) {
        CaptureHooks.add(Hook);
    }

    public static void addCaptureVeto(Predicate<Entity> Veto) {
        Vetoes.add(Veto);
    }

    public static void addReleaseHook(BiConsumer<ServerLevel, Entity> Hook) {
        ReleaseHooks.add(Hook);
    }

    private static void afterRelease(ServerLevel World, Entity Spawned) {
        for (BiConsumer<ServerLevel, Entity> Hook : ReleaseHooks) {
            Hook.accept(World, Spawned);
        }
    }

    public static boolean isVetoed(Entity Target) {
        for (Predicate<Entity> Veto : Vetoes) {
            if (Veto.test(Target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isRiddenByPlayer(Entity Target) {
        for (Entity Rider : Target.getIndirectPassengers()) {
            if (Rider instanceof Player) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Component refusal(Player User, ItemStack Stack, Entity Target) {
        StasisContents Held = StasisPodItem.contents(Stack);
        if (Held != null) {
            return Component.translatable("message.scp_unleashed.stasis.occupied", Held.Label());
        }
        if (StasisPodItem.remaining(Stack, 0L) <= 0L) {
            return Component.translatable("message.scp_unleashed.stasis.spent");
        }
        if (Target instanceof Player || !(Target instanceof Mob)) {
            return Component.translatable("message.scp_unleashed.stasis.immune", Target.getDisplayName());
        }
        if (isVetoed(Target)) {
            return Component.translatable("message.scp_unleashed.stasis.vetoed");
        }
        if (isRiddenByPlayer(Target)) {
            return Component.translatable("message.scp_unleashed.stasis.ridden", Target.getDisplayName());
        }
        if (!Target.isAlive() || Target.getType().is(StasisRegistry.Immune) || Target.isMultipartEntity() || !Target.getType().canSerialize()) {
            return Component.translatable("message.scp_unleashed.stasis.immune", Target.getDisplayName());
        }
        if (Target instanceof OwnableEntity Pet && Pet.getOwnerUUID() != null && !Pet.getOwnerUUID().equals(User.getUUID()) && !User.getAbilities().instabuild) {
            return Component.translatable("message.scp_unleashed.stasis.owned", Target.getDisplayName());
        }
        return null;
    }

    public static boolean capture(ServerLevel World, Player User, ItemStack Stack, Mob Target) {
        if (!(Stack.getItem() instanceof StasisPodItem Pod)) {
            return false;
        }
        if (refusal(User, Stack, Target) != null) {
            return false;
        }
        long Now = World.getGameTime();
        long Left = StasisPodItem.remaining(Stack, Now);
        if (Target.isLeashed()) {
            Target.dropLeash(true, true);
        }
        Target.ejectPassengers();
        Target.stopRiding();
        CompoundTag Data = new CompoundTag();
        if (!Target.save(Data)) {
            StasisEffects.deny(World, Target.getBoundingBox().getCenter(), Pod.getGrade());
            return false;
        }
        Data.remove("Passengers");
        Data.remove("leash");
        for (CaptureHook Hook : CaptureHooks) {
            Hook.captured(World, User, Target, Data);
        }
        StasisContents Held = new StasisContents(Data, Target.getName().copy(), Target.getType().getDescription().copy(), Target.getHealth(), Target.getMaxHealth(),
                Now + Left);
        StasisEffects.capture(World, Target, User, Pod.getGrade());
        World.gameEvent(User, GameEvent.ENTITY_INTERACT, Target.position());
        Target.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        Stack.remove(StasisRegistry.Charge.get());
        Stack.set(StasisRegistry.Contents.get(), Held);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.stasis.captured", Held.Label(), StasisPodItem.clock(Left))
                .withStyle(styleFor(Pod)), true);
        User.awardStat(Stats.ITEM_USED.get(Pod));
        return true;
    }

    public static boolean release(UseOnContext Context, ItemStack Stack) {
        if (!(Context.getLevel() instanceof ServerLevel World) || !(Stack.getItem() instanceof StasisPodItem Pod)) {
            return false;
        }
        StasisContents Held = StasisPodItem.contents(Stack);
        if (Held == null) {
            return false;
        }
        Player User = Context.getPlayer();
        long Now = World.getGameTime();
        if (Held.expired(Now)) {
            ItemStack Copy = Stack.copy();
            spend(Stack);
            Vec3 Where = User != null ? User.position() : Vec3.atBottomCenterOf(Context.getClickedPos().relative(Context.getClickedFace()));
            breakOut(World, Copy, Where, User);
            return true;
        }
        BlockPos Clicked = Context.getClickedPos();
        Direction Face = Context.getClickedFace();
        BlockState State = World.getBlockState(Clicked);
        VoxelShape Shape = State.getCollisionShape(World, Clicked);
        BlockPos Spot = Shape.isEmpty() ? Clicked : Clicked.relative(Face);
        Optional<Entity> Made = create(World, Held);
        if (Made.isEmpty()) {
            tell(User, Component.translatable("message.scp_unleashed.stasis.release_failed", Held.Label()));
            StasisEffects.deny(World, Context.getClickLocation(), Pod.getGrade());
            return false;
        }
        Entity Spawned = Made.get();
        double Y = Spot.getY();
        if (!Shape.isEmpty() && Face == Direction.UP) {
            Y = Clicked.getY() + Shape.max(Direction.Axis.Y);
        } else if (!Shape.isEmpty() && Face == Direction.DOWN) {
            Y = Clicked.getY() + Shape.min(Direction.Axis.Y) - Spawned.getBbHeight();
        }
        float Yaw = User != null ? Mth.wrapDegrees(User.getYRot() + 180.0f) : World.random.nextFloat() * 360.0f;
        Spawned.moveTo(Spot.getX() + 0.5, Y, Spot.getZ() + 0.5, Yaw, 0.0f);
        if (!World.noCollision(Spawned)) {
            tell(User, Component.translatable("message.scp_unleashed.stasis.no_room", Held.Label()));
            StasisEffects.deny(World, Context.getClickLocation(), Pod.getGrade());
            return false;
        }
        if (!place(World, Spawned, Yaw)) {
            tell(User, Component.translatable("message.scp_unleashed.stasis.release_failed", Held.Label()));
            StasisEffects.deny(World, Context.getClickLocation(), Pod.getGrade());
            return false;
        }
        long Left = Held.remaining(Now);
        Stack.remove(StasisRegistry.Contents.get());
        Stack.set(StasisRegistry.Charge.get(), (int) Math.min(Integer.MAX_VALUE, Left));
        StasisEffects.release(World, Spawned, Pod.getGrade());
        World.gameEvent(User, GameEvent.ENTITY_PLACE, Spawned.position());
        tell(User, Component.translatable(Left > 0L ? "message.scp_unleashed.stasis.released" : "message.scp_unleashed.stasis.released_spent",
                Held.Label(), StasisPodItem.clock(Left)).withStyle(styleFor(Pod)));
        afterRelease(World, Spawned);
        return true;
    }

    public static void spend(ItemStack Stack) {
        Stack.remove(StasisRegistry.Contents.get());
        Stack.set(StasisRegistry.Charge.get(), 0);
        Stack.setCount(0);
    }

    public static boolean breakOut(ServerLevel World, ItemStack Pod, Vec3 Where, @Nullable Entity Holder) {
        StasisContents Held = StasisPodItem.contents(Pod);
        if (Held == null || !(Pod.getItem() instanceof StasisPodItem Kind)) {
            return false;
        }
        FragmentRarity Grade = Kind.getGrade();
        Optional<Entity> Made = create(World, Held);
        if (Made.isEmpty()) {
            StasisEffects.breakout(World, Where, 0.6f, 0.6f, Grade);
            notice(World, Where, Component.translatable("message.scp_unleashed.stasis.dissipated", Held.Label()));
            return false;
        }
        Entity Spawned = Made.get();
        float Yaw = Holder != null ? Mth.wrapDegrees(Holder.getYRot() + 180.0f) : World.random.nextFloat() * 360.0f;
        Vec3 Spot = findRoom(World, Spawned, Where, Yaw);
        Spawned.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        StasisEffects.breakout(World, Spot, Spawned.getBbWidth(), Spawned.getBbHeight(), Grade);
        if (!place(World, Spawned, Yaw)) {
            Log.warn("Stasis pod breakout could not add {} at {}", Held.Kind().getString(), Spot);
            notice(World, Where, Component.translatable("message.scp_unleashed.stasis.dissipated", Held.Label()));
            return false;
        }
        notice(World, Where, Component.translatable("message.scp_unleashed.stasis.broke_out", Held.Label()));
        afterRelease(World, Spawned);
        return true;
    }

    public static Optional<Entity> create(ServerLevel World, StasisContents Held) {
        CompoundTag Data = Held.copyData();
        Data.remove("Passengers");
        try {
            return EntityType.create(Data, World);
        } catch (RuntimeException Error) {
            Log.warn("Stasis pod could not rebuild {}", Held.Kind().getString(), Error);
            return Optional.empty();
        }
    }

    public static boolean place(ServerLevel World, Entity Spawned, float Yaw) {
        UUID Original = Spawned.getUUID();
        if (World.getEntity(Original) != null) {
            Spawned.setUUID(Mth.createInsecureUUID(World.random));
        }
        Spawned.setDeltaMovement(Vec3.ZERO);
        Spawned.fallDistance = 0.0f;
        if (Spawned instanceof LivingEntity Living) {
            Living.setYHeadRot(Yaw);
            Living.setYBodyRot(Yaw);
        }
        if (Spawned instanceof Mob Creature) {
            Creature.setPersistenceRequired();
        }
        return World.addFreshEntity(Spawned);
    }

    public static Vec3 findRoom(ServerLevel World, Entity Spawned, Vec3 Base, float Yaw) {
        Spawned.moveTo(Base.x, Base.y, Base.z, Yaw, 0.0f);
        if (World.noCollision(Spawned)) {
            return Base;
        }
        for (int Radius = 1; Radius <= RoomSearch; Radius++) {
            for (int Lift = 0; Lift <= 1; Lift++) {
                for (int Dx = -Radius; Dx <= Radius; Dx++) {
                    for (int Dz = -Radius; Dz <= Radius; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                            continue;
                        }
                        Vec3 Candidate = new Vec3(Math.floor(Base.x + Dx) + 0.5, Math.floor(Base.y) + Lift, Math.floor(Base.z + Dz) + 0.5);
                        Spawned.moveTo(Candidate.x, Candidate.y, Candidate.z, Yaw, 0.0f);
                        if (World.noCollision(Spawned)) {
                            return Candidate;
                        }
                    }
                }
            }
            Vec3 Above = Base.add(0.0, Radius, 0.0);
            Spawned.moveTo(Above.x, Above.y, Above.z, Yaw, 0.0f);
            if (World.noCollision(Spawned)) {
                return Above;
            }
        }
        return Base;
    }

    public static void notice(ServerLevel World, Vec3 Where, Component Text) {
        Component Styled = Text.copy().withStyle(ChatFormatting.RED);
        for (ServerPlayer Watcher : World.players()) {
            if (Watcher.distanceToSqr(Where) <= NoticeRange * NoticeRange) {
                Watcher.displayClientMessage(Styled, true);
            }
        }
    }

    private static void tell(@Nullable Player User, Component Text) {
        if (User != null) {
            User.displayClientMessage(Text, true);
        }
    }

    private static Style styleFor(StasisPodItem Pod) {
        return Style.EMPTY.withColor(Pod.getRgb());
    }
}

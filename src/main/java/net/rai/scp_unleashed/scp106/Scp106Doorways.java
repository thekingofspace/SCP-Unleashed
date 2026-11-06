package net.rai.scp_unleashed.scp106;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp106Doorways {
    public static final float BaseEscape = 0.06f;
    public static final float EscapePerDoor = 0.03f;
    public static final float MaxEscape = 0.33f;
    public static final float TrapChance = 0.18f;
    public static final float BackChance = 0.20f;
    public static final float FallShare = 0.55f;
    public static final float CorrosionDamage = 5.0f;
    public static final int CorrosionSlow = 100;
    public static final int PassCooldown = 20;
    private static final Map<UUID, Long> LastPass = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> Pending = new ConcurrentHashMap<>();

    public enum Outcome {
        Escape,
        Fall,
        Corrosion,
        Back,
        Deeper
    }

    private Scp106Doorways() {
    }

    public static float escapeChance(int Doors) {
        return Math.min(MaxEscape, BaseEscape + EscapePerDoor * Math.max(0, Doors - 1));
    }

    public static Outcome roll(RandomSource Random, int Doors) {
        float Roll = Random.nextFloat();
        float Escape = escapeChance(Doors);
        if (Roll < Escape) {
            return Outcome.Escape;
        }
        Roll -= Escape;
        if (Roll < TrapChance) {
            return Random.nextFloat() < FallShare ? Outcome.Fall : Outcome.Corrosion;
        }
        Roll -= TrapChance;
        if (Roll < BackChance) {
            return Outcome.Back;
        }
        return Outcome.Deeper;
    }

    public static void queue(ServerLevel Pocket, BlockPos Doorway, LivingEntity Walker) {
        if (touching(Pocket, Doorway, Walker)) {
            Pending.putIfAbsent(Walker.getUUID(), Doorway.immutable());
        }
    }

    public static void flush(MinecraftServer Server) {
        if (Pending.isEmpty()) {
            return;
        }
        ServerLevel Pocket = Scp106Pocket.level(Server);
        for (Map.Entry<UUID, BlockPos> Entry : new ArrayList<>(Pending.entrySet())) {
            Pending.remove(Entry.getKey());
            if (Pocket != null && Pocket.getEntity(Entry.getKey()) instanceof LivingEntity Walker && Walker.isAlive() && Scp106Pocket.inPocket(Walker)) {
                pass(Pocket, Entry.getValue(), Walker);
            }
        }
    }

    private static boolean touching(ServerLevel Pocket, BlockPos Doorway, LivingEntity Walker) {
        BlockState State = Pocket.getBlockState(Doorway);
        if (!(State.getBlock() instanceof Scp106DoorwayBlock)) {
            return false;
        }
        VoxelShape Pane = State.getShape(Pocket, Doorway, CollisionContext.empty());
        AABB Box = Walker.getBoundingBox();
        for (AABB Piece : Pane.toAabbs()) {
            if (Piece.move(Doorway).intersects(Box)) {
                return true;
            }
        }
        return false;
    }

    public static void pass(ServerLevel Pocket, BlockPos Doorway, LivingEntity Walker) {
        long Now = Pocket.getGameTime();
        Long Last = LastPass.get(Walker.getUUID());
        if (Last != null && Now - Last < PassCooldown && Now >= Last) {
            return;
        }
        LastPass.put(Walker.getUUID(), Now);
        Vec3 Here = Walker.position();
        Pocket.playSound(null, Here.x, Here.y + 1.0, Here.z, Scp106Registry.DoorwayPass.get(), SoundSource.AMBIENT, 1.0f, 0.8f + Pocket.getRandom().nextFloat() * 0.2f);
        if (Walker instanceof ServerPlayer User) {
            passPlayer(Pocket, User);
            return;
        }
        if (Walker instanceof Scp106OldMan) {
            return;
        }
        Scp106Pocket.takeCaptive(Walker);
    }

    private static void passPlayer(ServerLevel Pocket, ServerPlayer User) {
        Scp106PocketWorld Data = Scp106PocketWorld.get(User.server);
        Scp106PocketWorld.Visit Visit = Data.visitOf(User.getUUID());
        if (Visit == null) {
            Visit = new Scp106PocketWorld.Visit(Scp106Pocket.outside(User), 0, Scp106PocketLayout.roomAt(User.getX()));
            Data.startVisit(User.getUUID(), Visit);
        }
        Visit.Doors++;
        Data.touchVisit();
        RandomSource Random = User.getRandom();
        Outcome Result = roll(Random, Visit.Doors);
        int Room = Scp106PocketLayout.roomAt(User.getX());
        switch (Result) {
            case Escape -> Scp106Pocket.leave(User, true);
            case Fall -> {
                Scp106Pocket.sendToRoom(User, Scp106PocketLayout.Trench);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.fall").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
            }
            case Corrosion -> {
                User.hurt(Pocket.damageSources().source(Scp106Registry.Corrosion), CorrosionDamage);
                User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CorrosionSlow, 1, false, true));
                Scp106Effects.splash(Pocket, User.position().add(0.0, 1.0, 0.0), 20, 0.5);
                if (User.isAlive()) {
                    Scp106Pocket.sendToRoom(User, Scp106Pocket.wanderRoom(Random, Room));
                }
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.corroded").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
            }
            case Back -> Scp106Pocket.sendToRoom(User, Room == Scp106PocketLayout.Arrival ? Scp106Pocket.wanderRoom(Random, Room) : Scp106PocketLayout.Arrival);
            default -> Scp106Pocket.sendToRoom(User, Scp106Pocket.wanderRoom(Random, Room));
        }
        if (Result != Outcome.Escape && Random.nextFloat() < 0.3f) {
            Pocket.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp106Registry.PocketLaugh.get(), SoundSource.AMBIENT, 0.9f, 0.97f + Random.nextFloat() * 0.06f);
        }
    }

    public static void forget(UUID Id) {
        LastPass.remove(Id);
    }

    public static void clear() {
        LastPass.clear();
        Pending.clear();
    }
}

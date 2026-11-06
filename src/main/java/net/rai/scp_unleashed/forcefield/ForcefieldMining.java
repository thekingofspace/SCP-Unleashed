package net.rai.scp_unleashed.forcefield;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ForcefieldMining {
    public static final int PulseTicks = 10;
    public static final float MeleeScale = 1.0f;
    public static final float MinimumBlow = 1.0f;
    private static final Map<UUID, Dig> Diggers = new HashMap<>();

    private ForcefieldMining() {
    }

    public static float blowOf(ServerPlayer User) {
        return Math.max(MinimumBlow, (float) User.getAttributeValue(Attributes.ATTACK_DAMAGE) * MeleeScale);
    }

    public static void strike(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        Vec3 Point = aimPoint(User, Pos);
        ForcefieldBarrierBlock.strike(World, Pos, Point, blowOf(User));
        if (!User.isCreative()) {
            Diggers.put(User.getUUID(), new Dig(World, Pos.immutable(), World.getGameTime() + PulseTicks));
        }
    }

    public static void tick(ServerLevel World) {
        if (Diggers.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        Iterator<Map.Entry<UUID, Dig>> Entries = Diggers.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<UUID, Dig> Entry = Entries.next();
            Dig Current = Entry.getValue();
            if (Current.World() != World || Now < Current.Due()) {
                continue;
            }
            ServerPlayer User = World.getServer().getPlayerList().getPlayer(Entry.getKey());
            if (User == null || User.level() != World || !User.isAlive() || !User.swinging || !aimed(User, Current.Pos())) {
                Entries.remove();
                continue;
            }
            ForcefieldBarrierBlock.strike(World, Current.Pos(), aimPoint(User, Current.Pos()), blowOf(User));
            Entry.setValue(new Dig(World, Current.Pos(), Now + PulseTicks));
        }
    }

    public static void forget(UUID Id) {
        Diggers.remove(Id);
    }

    public static void clear() {
        Diggers.clear();
    }

    private static boolean aimed(ServerPlayer User, BlockPos Pos) {
        HitResult Target = User.pick(User.blockInteractionRange(), 1.0f, false);
        return Target instanceof BlockHitResult Aimed && Aimed.getType() == HitResult.Type.BLOCK && Aimed.getBlockPos().equals(Pos);
    }

    private static Vec3 aimPoint(ServerPlayer User, BlockPos Pos) {
        HitResult Target = User.pick(User.blockInteractionRange(), 1.0f, false);
        if (Target instanceof BlockHitResult Aimed && Aimed.getType() == HitResult.Type.BLOCK && Aimed.getBlockPos().equals(Pos)) {
            return Aimed.getLocation();
        }
        return Vec3.atCenterOf(Pos);
    }

    private record Dig(ServerLevel World, BlockPos Pos, long Due) {
    }
}

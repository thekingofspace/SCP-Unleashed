package net.rai.scp_unleashed.scp539;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Scp539Plan {
    public static final String Phrase = "i got the perfect plan";
    public static final int Window = 200;
    public static final int Cooldown = 1200;

    private static final Map<UUID, Long> Armed = new HashMap<>();
    private static final Map<UUID, Long> Ready = new HashMap<>();
    private static final Map<UUID, UUID> Flying = new HashMap<>();

    private Scp539Plan() {
    }

    public static boolean matches(String Raw) {
        if (Raw == null) {
            return false;
        }
        String Text = Raw.strip().toLowerCase(Locale.ROOT);
        int End = Text.length();
        while (End > 0 && !Character.isLetterOrDigit(Text.charAt(End - 1))) {
            End--;
        }
        return Text.substring(0, End).replaceAll("\\s+", " ").equals(Phrase);
    }

    public static void arm(ServerPlayer User, long Now) {
        Armed.put(User.getUUID(), Now);
    }

    public static boolean isArmed(UUID Id, long Now) {
        Long Said = Armed.get(Id);
        return Said != null && Now - Said <= Window;
    }

    public static boolean claim(ServerPlayer User, long Now) {
        Long Said = Armed.remove(User.getUUID());
        if (Said == null || Now - Said > Window || Scp539Compulsion.isAffected(User)) {
            return false;
        }
        if (busy(User.server, User.getUUID()) || Now < Ready.getOrDefault(User.getUUID(), Long.MIN_VALUE)) {
            return false;
        }
        Ready.put(User.getUUID(), Now + Cooldown);
        return true;
    }

    public static boolean busy(MinecraftServer Server, UUID Owner) {
        UUID Craft = Flying.get(Owner);
        if (Craft == null) {
            return false;
        }
        for (ServerLevel World : Server.getAllLevels()) {
            Entity Found = World.getEntity(Craft);
            if (Found instanceof Scp539Helicopter && Found.isAlive()) {
                return true;
            }
        }
        Flying.remove(Owner);
        return false;
    }

    @Nullable
    public static Scp539Helicopter launch(ServerLevel World, Scp539Stone Stone, Vec3 Where, @Nullable Direction Face, @Nullable BlockPos Struck) {
        Entity Owner = Stone.getOwner();
        UUID OwnerId = Owner == null ? null : Owner.getUUID();
        if (OwnerId != null && busy(World.getServer(), OwnerId)) {
            return null;
        }
        Scp539Crash.Route Route = Owner instanceof LivingEntity Target && Target.isAlive() && Target.level() == World
                ? Scp539Crash.plan(World, Target.position(), null, null, Scp539Stone.flat(Where.subtract(Target.position())))
                : Scp539Crash.plan(World, Where, Face, Struck, Stone.heading());
        Scp539Helicopter Craft = Scp539Helicopter.launch(World, Route, OwnerId, Scp539Helicopter.Flight);
        if (Craft != null && OwnerId != null) {
            Flying.put(OwnerId, Craft.getUUID());
        }
        return Craft;
    }

    public static void track(UUID Owner, UUID Craft) {
        Flying.put(Owner, Craft);
    }

    public static void release(@Nullable UUID Owner, UUID Craft) {
        if (Owner != null && Craft.equals(Flying.get(Owner))) {
            Flying.remove(Owner);
        }
    }

    public static long readyAt(UUID Owner) {
        return Ready.getOrDefault(Owner, Long.MIN_VALUE);
    }

    public static void clear(UUID Owner) {
        Armed.remove(Owner);
        Ready.remove(Owner);
        Flying.remove(Owner);
    }

    public static void reset() {
        Armed.clear();
        Ready.clear();
        Flying.clear();
    }
}

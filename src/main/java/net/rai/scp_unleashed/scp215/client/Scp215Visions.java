package net.rai.scp_unleashed.scp215.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp215.Scp215GlassesItem;
import net.rai.scp_unleashed.scp215.Scp215Ground;
import net.rai.scp_unleashed.scp215.Scp215Phantom;
import net.rai.scp_unleashed.scp215.Scp215Registry;
import net.rai.scp_unleashed.scp215.Scp215Variant;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class Scp215Visions {
    public static final int Cap = 3;
    public static final int FirstDelayMin = 100;
    public static final int FirstDelayMax = 220;
    public static final int GapMin = 240;
    public static final int GapMax = 560;
    public static final int RetryDelay = 40;
    public static final int AfterVanishGap = 140;
    public static final int FirstWhisper = 600;
    public static final int WhisperMin = 900;
    public static final int WhisperMax = 2000;
    public static final float LifeSpread = 0.3f;
    public static final float SpawnClickChance = 0.4f;
    public static final float WhisperVolume = 0.7f;
    public static final int FirstId = -2_150_000;
    public static final int LastId = -2_000_000_000;
    private static final RandomSource Random = RandomSource.create();
    private static final List<Scp215Phantom> Active = new ArrayList<>();
    @Nullable
    private static ClientLevel Current;
    private static boolean Wearing;
    private static int Cooldown;
    private static int WhisperCooldown;
    private static int NextId = FirstId;

    private Scp215Visions() {
    }

    public static List<Scp215Phantom> active() {
        return List.copyOf(Active);
    }

    public static void tick(Minecraft Game) {
        ClientLevel World = Game.level;
        LocalPlayer User = Game.player;
        if (World == null || User == null) {
            reset();
            return;
        }
        if (World != Current) {
            Active.clear();
            Current = World;
            Wearing = false;
        }
        if (Game.isPaused()) {
            return;
        }
        int Before = Active.size();
        Active.removeIf(Entity::isRemoved);
        if (Active.size() < Before) {
            Cooldown = Math.max(Cooldown, AfterVanishGap);
        }
        boolean Worn = Scp215GlassesItem.isWorn(User) && User.isAlive() && !User.isSpectator();
        if (!Worn) {
            if (Wearing) {
                clear();
            }
            Wearing = false;
            return;
        }
        if (!Wearing) {
            Wearing = true;
            Cooldown = between(FirstDelayMin, FirstDelayMax);
            WhisperCooldown = FirstWhisper + Random.nextInt(WhisperMin);
        }
        if (--Cooldown <= 0) {
            Cooldown = Active.size() < Cap && spawn(World, User) ? between(GapMin, GapMax) : RetryDelay;
        }
        if (--WhisperCooldown <= 0) {
            whisper(World, User);
            WhisperCooldown = between(WhisperMin, WhisperMax);
        }
    }

    private static int between(int Low, int High) {
        return Low + Random.nextInt(High - Low + 1);
    }

    private static boolean spawn(ClientLevel World, LocalPlayer User) {
        Set<Scp215Variant> Present = EnumSet.noneOf(Scp215Variant.class);
        for (Scp215Phantom Phantom : Active) {
            Present.add(Phantom.variant());
        }
        Scp215Variant Kind = Scp215Variant.pick(Random, Present);
        Scp215Spots.Spot Found = Scp215Spots.find(World, User, Kind, Random);
        if (Found == null) {
            return false;
        }
        Vec3 Feet = Found.Feet();
        Vec3 Eye = User.getEyePosition();
        Scp215Phantom Phantom = new Scp215Phantom(Scp215Registry.Phantom.get(), World);
        Phantom.setId(nextId());
        Phantom.moveTo(Feet.x, Feet.y, Feet.z, 0.0f, 0.0f);
        int Life = Math.round(Kind.Lifetime * (1.0f - LifeSpread / 2.0f + Random.nextFloat() * LifeSpread));
        Phantom.setup(Kind, Life, Scp215Ground.yawToward(Eye.x - Feet.x, Eye.z - Feet.z));
        World.addEntity(Phantom);
        if (Phantom.isRemoved() || World.getEntity(Phantom.getId()) != Phantom) {
            return false;
        }
        Active.add(Phantom);
        if (Random.nextFloat() < SpawnClickChance) {
            Phantom.sound(Scp215Registry.ClickLayers, 0.4f);
        }
        return true;
    }

    private static int nextId() {
        int Id = NextId--;
        if (NextId < LastId) {
            NextId = FirstId;
        }
        return Id;
    }

    private static void whisper(ClientLevel World, LocalPlayer User) {
        float Yaw = (User.getYRot() + 180.0f + (Random.nextFloat() * 2.0f - 1.0f) * 70.0f) * Mth.DEG_TO_RAD;
        double Distance = 4.0 + Random.nextDouble() * 4.0;
        double X = User.getX() - Mth.sin(Yaw) * Distance;
        double Z = User.getZ() + Mth.cos(Yaw) * Distance;
        double Y = User.getEyeY() + (Random.nextDouble() - 0.5);
        float Pitch = 0.92f + Random.nextFloat() * 0.16f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp215Registry.WhisperLayers) {
            World.playLocalSound(X, Y, Z, Layer.get(), SoundSource.AMBIENT, WhisperVolume, Pitch, false);
        }
    }

    public static void clear() {
        for (Scp215Phantom Phantom : Active) {
            if (!Phantom.isRemoved()) {
                Phantom.discard();
            }
        }
        Active.clear();
    }

    public static void reset() {
        clear();
        Current = null;
        Wearing = false;
        Cooldown = 0;
        WhisperCooldown = 0;
    }
}

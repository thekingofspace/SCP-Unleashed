package net.rai.scp_unleashed.worldend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.worldend.WorldEndRegistry;
import net.rai.scp_unleashed.worldend.network.WorldEndPayload;

public final class WorldEndClient {
    public static final int Lead = 40;
    public static final int Grow = 250;
    public static final int Recede = 70;
    public static final int Settle = 60;
    private static final RandomSource Random = RandomSource.create();
    private static int Stage = WorldEndPayload.Idle;
    private static int Elapsed;
    private static int Duration = 360;
    private static boolean Focused;
    private static Vec3 Focus = Vec3.ZERO;
    private static Component Message = Component.empty();
    private static int ReleaseTicks;
    private static float ReleaseFrom;
    private static boolean PendingFinale;
    private static boolean Captured;
    private static float FrozenYaw;
    private static float FrozenPitch;
    private static float Cover;
    private static float CoverO;
    private static int NextBeat;
    private static int NextWet;

    private WorldEndClient() {
    }

    public static void receive(WorldEndPayload Payload) {
        int Previous = Stage;
        Stage = Payload.Stage();
        Duration = Math.max(1, Payload.Duration());
        Focused = Payload.Focused();
        Focus = Payload.Focus();
        Message = Payload.Message();
        if (Stage == WorldEndPayload.Freezing) {
            if (Previous != WorldEndPayload.Freezing || Math.abs(Elapsed - Payload.Elapsed()) > 5) {
                Elapsed = Payload.Elapsed();
            }
            if (Previous != WorldEndPayload.Freezing) {
                Captured = false;
                NextBeat = Elapsed + 10;
                NextWet = Elapsed + 30;
                play(WorldEndRegistry.Swell.get(), 0.6f, 1.0f);
            }
        } else if (Stage == WorldEndPayload.Released) {
            ReleaseTicks = 0;
            ReleaseFrom = Math.max(Cover, 0.001f);
        } else if (Stage == WorldEndPayload.Final) {
            PendingFinale = true;
            Cover = 1.0f;
        } else {
            reset();
        }
    }

    public static void reset() {
        Stage = WorldEndPayload.Idle;
        Elapsed = 0;
        Cover = 0.0f;
        CoverO = 0.0f;
        Captured = false;
        PendingFinale = false;
    }

    public static boolean cinematic() {
        return Stage == WorldEndPayload.Freezing || Stage == WorldEndPayload.Final;
    }

    public static boolean visible() {
        return Stage != WorldEndPayload.Idle;
    }

    public static float cover(float Partial) {
        return Mth.lerp(Partial, CoverO, Cover);
    }

    public static float darkness(float Partial) {
        if (Stage == WorldEndPayload.Final) {
            return 1.0f;
        }
        if (Stage != WorldEndPayload.Freezing) {
            return 0.0f;
        }
        return Mth.clamp((Elapsed + Partial - (Duration - Settle)) / (float) Settle, 0.0f, 1.0f);
    }

    public static float beat(float Partial) {
        if (!visible()) {
            return 0.0f;
        }
        float Since = Elapsed + Partial - (NextBeat - interval());
        return Since < 0.0f ? 0.0f : (float) Math.exp(-Since * 0.35);
    }

    public static int elapsed() {
        return Elapsed;
    }

    private static int interval() {
        return Math.round(Mth.lerp(Cover, 34.0f, 15.0f));
    }

    public static void tick(Minecraft Client) {
        CoverO = Cover;
        Player Viewer = Client.player;
        if (Stage == WorldEndPayload.Freezing) {
            Elapsed++;
            if (!Captured && Viewer != null) {
                Captured = true;
                FrozenYaw = Viewer.getYRot();
                FrozenPitch = Viewer.getXRot();
            }
            float Linear = Mth.clamp((Elapsed - Lead) / (float) Grow, 0.0f, 1.0f);
            Cover = Linear * Linear * (3.0f - 2.0f * Linear);
            if (Elapsed >= NextBeat) {
                NextBeat = Elapsed + interval();
                play(WorldEndRegistry.Heartbeat.get(), 0.9f, 0.85f + 0.25f * Cover);
                play(WorldEndRegistry.Thump.get(), 0.7f, 0.8f + 0.2f * Cover);
            }
            if (Elapsed >= NextWet && Cover > 0.02f && Cover < 0.99f) {
                NextWet = Elapsed + 10 + Random.nextInt(22);
                play(WorldEndRegistry.Wet.get(), 0.5f + 0.4f * Cover, 0.7f + Random.nextFloat() * 0.4f);
            }
            if (Elapsed == Duration - Settle / 2) {
                play(WorldEndRegistry.Toll.get(), 1.0f, 0.6f);
            }
        } else if (Stage == WorldEndPayload.Final) {
            Cover = 1.0f;
            if (PendingFinale) {
                PendingFinale = false;
                finale(Client);
            }
        } else if (Stage == WorldEndPayload.Released) {
            ReleaseTicks++;
            float Left = 1.0f - Mth.clamp(ReleaseTicks / (float) Recede, 0.0f, 1.0f);
            Cover = ReleaseFrom * Left * Left;
            if (ReleaseTicks % 18 == 1 && Left > 0.2f) {
                play(WorldEndRegistry.Wet.get(), 0.4f * Left, 0.9f);
            }
            if (ReleaseTicks > Recede) {
                reset();
            }
        }
    }

    private static void finale(Minecraft Client) {
        ClientLevel World = Client.level;
        Component Shown = Message;
        WorldEndGate.release(Client);
        if (World != null) {
            World.disconnect();
        }
        Client.disconnect(new WorldEndScreen(Shown));
        reset();
    }

    private static void play(SoundEvent Sound, float Volume, float Pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
    }

    public static float yaw(float Partial, float Fallback) {
        if (!Captured) {
            return Fallback;
        }
        float Toward = FrozenYaw;
        Player Viewer = Minecraft.getInstance().player;
        float Blend = smooth(Mth.clamp((Elapsed + Partial) / (Lead + 60.0f), 0.0f, 1.0f));
        if (Focused && Viewer != null) {
            Vec3 Delta = Focus.subtract(Viewer.getEyePosition(Partial));
            float Target = (float) (Mth.atan2(Delta.z, Delta.x) * Mth.RAD_TO_DEG) - 90.0f;
            Toward = FrozenYaw + Mth.wrapDegrees(Target - FrozenYaw) * Blend;
        }
        return Toward + tremble(Partial, 0.0f);
    }

    public static float pitch(float Partial, float Fallback) {
        if (!Captured) {
            return Fallback;
        }
        Player Viewer = Minecraft.getInstance().player;
        float Blend = smooth(Mth.clamp((Elapsed + Partial) / (Lead + 60.0f), 0.0f, 1.0f));
        float Target = -24.0f;
        if (Focused && Viewer != null) {
            Vec3 Delta = Focus.subtract(Viewer.getEyePosition(Partial));
            double Flat = Math.sqrt(Delta.x * Delta.x + Delta.z * Delta.z);
            Target = (float) -(Mth.atan2(Delta.y, Flat) * Mth.RAD_TO_DEG);
        }
        return Mth.lerp(Blend, FrozenPitch, Mth.clamp(Target, -80.0f, 60.0f)) + tremble(Partial, 3.1f);
    }

    private static float tremble(float Partial, float Phase) {
        float Time = Elapsed + Partial;
        return (float) Math.sin(Time * 0.9 + Phase) * 0.35f * Cover;
    }

    private static float smooth(float Value) {
        return Value * Value * (3.0f - 2.0f * Value);
    }
}

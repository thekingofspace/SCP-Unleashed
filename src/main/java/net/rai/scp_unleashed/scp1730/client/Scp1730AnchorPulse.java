package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.network.Scp1730PulsePayload;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class Scp1730AnchorPulse {
    public static final ResourceLocation Pipeline = VeilPost.id("scp1730_anchor_pulse");
    public static final int RingTicks = 11;
    public static final int Life = 34;
    public static final int FlashTicks = 6;
    public static final int RippleTicks = 18;
    public static final int Points = 44;
    public static final double Sight = 4.0;
    public static final int Block = 8;
    private static final List<Pulse> Active = new ArrayList<>();
    private static boolean Drawn;
    private static float Flash;
    private static float Ripple;
    private static float RingShare;

    private Scp1730AnchorPulse() {
    }

    public static void start(Scp1730PulsePayload Payload) {
        Minecraft.getInstance().execute(() -> begin(Payload));
    }

    private static void begin(Scp1730PulsePayload Payload) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        double Ground = Payload.Y();
        BlockPos Probe = BlockPos.containing(Payload.X(), Payload.Y(), Payload.Z());
        for (int Step = 0; Step < 4; Step++) {
            BlockPos Below = Probe.below(Step + 1);
            if (!World.getBlockState(Below).getCollisionShape(World, Below).isEmpty()) {
                Ground = Below.getY() + World.getBlockState(Below).getCollisionShape(World, Below).max(net.minecraft.core.Direction.Axis.Y);
                break;
            }
        }
        Pulse Made = new Pulse(new Vec3(Payload.X(), Payload.Y(), Payload.Z()), Ground, Payload.Radius());
        Active.add(Made);
        RandomSource Random = World.random;
        World.addParticle(ParticleTypes.FLASH, Payload.X(), Payload.Y(), Payload.Z(), 0.0, 0.0, 0.0);
        for (int Index = 0; Index < 26; Index++) {
            double Yaw = Random.nextDouble() * Math.PI * 2.0;
            double Lift = Random.nextDouble() * 0.6;
            double Speed = 0.25 + Random.nextDouble() * 0.35;
            World.addParticle(ParticleTypes.END_ROD, Payload.X(), Payload.Y(), Payload.Z(), Math.cos(Yaw) * Speed, Lift * Speed, Math.sin(Yaw) * Speed);
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, Payload.X(), Payload.Y(), Payload.Z(), Math.cos(Yaw) * Speed * 1.5, Lift * Speed + 0.1, Math.sin(Yaw) * Speed * 1.5);
        }
    }

    public static void tick() {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Client.player == null) {
            Active.clear();
            Flash = 0.0f;
            Ripple = 0.0f;
            return;
        }
        float WantFlash = 0.0f;
        float WantRipple = 0.0f;
        float WantRing = 0.0f;
        Vec3 Eye = Client.player.getEyePosition();
        for (int Index = Active.size() - 1; Index >= 0; Index--) {
            Pulse Each = Active.get(Index);
            Each.Age++;
            if (Each.Age > Life) {
                Active.remove(Index);
                continue;
            }
            if (Each.Age <= RingTicks) {
                ring(World, Each);
            } else if (World.random.nextInt(3) == 0) {
                double Yaw = World.random.nextDouble() * Math.PI * 2.0;
                World.addParticle(Scp1730Registry.AnchorParticle.get(), Each.Centre.x + Math.cos(Yaw) * Each.Radius, Each.Ground + 0.15, Each.Centre.z + Math.sin(Yaw) * Each.Radius,
                        0.0, 0.03, 0.0);
            }
            double Distance = Eye.distanceTo(Each.Centre);
            float Near = (float) Mth.clamp(1.0 - Distance / (Each.Radius * Sight), 0.0, 1.0);
            float Comfort = (float) Scp1730ClientAura.comfortScale();
            WantFlash = Math.max(WantFlash, Near * Comfort * Mth.clamp(1.0f - (Each.Age - 1) / (float) FlashTicks, 0.0f, 1.0f));
            if (Each.Age <= RippleTicks) {
                float Share = Each.Age / (float) RippleTicks;
                WantRipple = Math.max(WantRipple, Near * Comfort * (1.0f - Share));
                WantRing = Share;
                Focus = Each.Centre;
            }
        }
        Flash = WantFlash;
        Ripple = WantRipple;
        RingShare = WantRing;
    }

    private static Vec3 Focus = Vec3.ZERO;

    private static void ring(ClientLevel World, Pulse Each) {
        float Share = Each.Age / (float) RingTicks;
        double Reach = Each.Radius * (1.0 - Math.pow(1.0 - Share, 2.2));
        RandomSource Random = World.random;
        for (int Index = 0; Index < Points; Index++) {
            double Yaw = (Index + Random.nextDouble() * 0.6) / Points * Math.PI * 2.0;
            double X = Each.Centre.x + Math.cos(Yaw) * Reach;
            double Z = Each.Centre.z + Math.sin(Yaw) * Reach;
            World.addParticle(Scp1730Registry.AnchorParticle.get(), X, Each.Ground + 0.12, Z, Math.cos(Yaw) * 0.02, 0.01, Math.sin(Yaw) * 0.02);
            if (Index % 4 == 0) {
                World.addParticle(ParticleTypes.ELECTRIC_SPARK, X, Each.Ground + 0.1, Z, Math.cos(Yaw) * 0.08, 0.06, Math.sin(Yaw) * 0.08);
            }
        }
    }

    public static void render(Matrix4f ModelView, Matrix4f Projection, Vec3 Eye) {
        Drawn = false;
        if (Ripple < 0.003f && Flash < 0.003f || !VeilPost.available()) {
            return;
        }
        Vector4f Clip = new Vector4f((float) (Focus.x - Eye.x), (float) (Focus.y - Eye.y), (float) (Focus.z - Eye.z), 1.0f);
        ModelView.transform(Clip);
        Projection.transform(Clip);
        float AtX = 0.5f;
        float AtY = 0.5f;
        if (Clip.w > 0.05f) {
            AtX = Mth.clamp(Clip.x / Clip.w * 0.5f + 0.5f, -2.0f, 3.0f);
            AtY = Mth.clamp(Clip.y / Clip.w * 0.5f + 0.5f, -2.0f, 3.0f);
        }
        float CentreX = AtX;
        float CentreY = AtY;
        float Shown = Ripple;
        float Bright = Flash;
        float Ring = 0.08f + RingShare * 1.1f;
        Drawn = VeilPost.run(Pipeline, Values -> Values.set("Strength", Shown)
                .set("CenterX", CentreX)
                .set("CenterY", CentreY)
                .set("Ring", Ring)
                .set("Flash", Bright));
    }

    public static void overlay(GuiGraphics Graphics, DeltaTracker Delta) {
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        if (!Drawn && Flash > 0.01f) {
            Graphics.fill(0, 0, Width, Height, (int) (Flash * 120.0f) << 24 | 0xD8F4FF);
        }
        if (Drawn || Ripple < 0.01f) {
            return;
        }
        int Rings = 4;
        for (int Ring = 0; Ring < Rings; Ring++) {
            float Share = (Rings - Ring) / (float) Rings;
            int Alpha = (int) (Ripple * 60.0f * Share);
            int Colour = Alpha << 24 | 0x9FE6FF;
            int Inset = Ring * Block;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Block, Colour);
            Graphics.fill(Inset, Height - Inset - Block, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Inset + Block, Inset + Block, Height - Inset - Block, Colour);
            Graphics.fill(Width - Inset - Block, Inset + Block, Width - Inset, Height - Inset - Block, Colour);
        }
    }

    public static void reset() {
        Active.clear();
        Flash = 0.0f;
        Ripple = 0.0f;
    }

    static final class Pulse {
        final Vec3 Centre;
        final double Ground;
        final double Radius;
        int Age;

        Pulse(Vec3 Centre, double Ground, double Radius) {
            this.Centre = Centre;
            this.Ground = Ground;
            this.Radius = Radius;
        }
    }
}

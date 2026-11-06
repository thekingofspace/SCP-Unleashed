package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.scp1730.Scp1730Aura;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class Scp1730ClientAura {
    public static final ResourceLocation Pipeline = VeilPost.id("scp1730_distortion");
    public static final double Search = 32.0;
    public static final int Block = 8;
    private static float Strength;
    private static float StrengthOld;
    private static float Flicker;
    private static Vec3 Focus = Vec3.ZERO;
    private static boolean Drawn;
    @Nullable
    private static Hum Playing;

    private Scp1730ClientAura() {
    }

    public static double comfortScale() {
        return ScpClientConfig.realityDistortion();
    }

    public static void tick() {
        Minecraft Client = Minecraft.getInstance();
        StrengthOld = Strength;
        if (Client.level == null || Client.player == null) {
            Strength = 0.0f;
            return;
        }
        Scp1730Guardian Nearest = null;
        double Best = Search;
        for (Scp1730Guardian Each : Client.level.getEntitiesOfClass(Scp1730Guardian.class, Client.player.getBoundingBox().inflate(Search), Scp1730Guardian::isAlive)) {
            double Distance = Each.glyphPoint().distanceTo(Client.player.getEyePosition());
            if (Distance < Best) {
                Best = Distance;
                Nearest = Each;
            }
        }
        float Want = 0.0f;
        if (Nearest != null && !Nearest.isDeadOrDying()) {
            Want = (float) Scp1730Aura.comfort(Scp1730Aura.strength(Best, Nearest.anchored(), Nearest.casting()), comfortScale());
            Focus = Nearest.glyphPoint();
            if (Playing == null || Playing.isStopped()) {
                Playing = new Hum(Nearest);
                Client.getSoundManager().play(Playing);
            }
        }
        Strength = Strength + (Want - Strength) * (Want < Strength ? 0.35f : 0.12f);
        Flicker = Client.level.random.nextInt(14) == 0 ? Client.level.random.nextFloat() * Strength : Flicker * 0.6f;
    }

    public static float shown(float Partial) {
        return Mth.lerp(Partial, StrengthOld, Strength);
    }

    public static void render(Matrix4f ModelView, Matrix4f Projection, Vec3 Eye, float Partial, double Seconds) {
        float Shown = shown(Partial);
        Drawn = false;
        if (Shown < 0.003f || !VeilPost.available()) {
            return;
        }
        Vector4f Clip = new Vector4f((float) (Focus.x - Eye.x), (float) (Focus.y - Eye.y), (float) (Focus.z - Eye.z), 1.0f);
        ModelView.transform(Clip);
        Projection.transform(Clip);
        float CenterX = 0.5f;
        float CenterY = 0.5f;
        if (Clip.w > 0.05f) {
            CenterX = Mth.clamp(Clip.x / Clip.w * 0.5f + 0.5f, -2.0f, 3.0f);
            CenterY = Mth.clamp(Clip.y / Clip.w * 0.5f + 0.5f, -2.0f, 3.0f);
        }
        float AtX = CenterX;
        float AtY = CenterY;
        float Dip = Flicker;
        Drawn = VeilPost.run(Pipeline, Values -> Values.set("Strength", Shown)
                .set("CenterX", AtX)
                .set("CenterY", AtY)
                .set("Seconds", (float) Seconds)
                .set("Flicker", Dip)
                .set("TintR", 0.55f)
                .set("TintG", 0.92f)
                .set("TintB", 1.0f));
    }

    public static void overlay(GuiGraphics Graphics, DeltaTracker Delta) {
        float Shown = shown(Delta.getGameTimeDeltaPartialTick(false));
        if (Drawn || Shown < 0.01f) {
            return;
        }
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Rings = 6;
        for (int Ring = 0; Ring < Rings; Ring++) {
            float Share = (Rings - Ring) / (float) Rings;
            int Alpha = (int) (Shown * 70.0f * Share * Share);
            int Colour = Alpha << 24 | 0x5CD8F0;
            int Inset = Ring * Block;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Block, Colour);
            Graphics.fill(Inset, Height - Inset - Block, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Inset + Block, Inset + Block, Height - Inset - Block, Colour);
            Graphics.fill(Width - Inset - Block, Inset + Block, Width - Inset, Height - Inset - Block, Colour);
        }
        if (Flicker > 0.05f) {
            Graphics.fill(0, 0, Width, Height, (int) (Flicker * 90.0f) << 24);
        }
    }

    public static void reset() {
        Strength = 0.0f;
        StrengthOld = 0.0f;
        Playing = null;
    }

    static final class Hum extends AbstractTickableSoundInstance {
        private final Scp1730Guardian Source;

        Hum(Scp1730Guardian Source) {
            super(Scp1730Registry.GuardianHum.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.Source = Source;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.x = Source.getX();
            this.y = Source.getY() + 3.0;
            this.z = Source.getZ();
        }

        @Override
        public void tick() {
            if (Source.isRemoved() || !Source.isAlive()) {
                stop();
                return;
            }
            x = Source.getX();
            y = Source.getY() + 3.0;
            z = Source.getZ();
            volume = Math.max(0.01f, Strength * 1.6f);
            pitch = Source.anchored() ? 0.55f : 0.8f + Strength * 0.1f;
        }
    }
}

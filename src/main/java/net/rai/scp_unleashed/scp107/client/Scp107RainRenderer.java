package net.rai.scp_unleashed.scp107.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp107.Scp107RainKind;
import net.rai.scp_unleashed.scp107.Scp107Registry;
import org.jetbrains.annotations.Nullable;

public final class Scp107RainRenderer {
    public static final ResourceLocation LavaTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/environment/scp107_lava_rain.png");
    public static final ResourceLocation CausticTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/environment/scp107_caustic_rain.png");
    public static final ResourceLocation MilkTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/environment/scp107_milk_rain.png");
    public static final float LavaSpeed = 1.7f;
    public static final float CausticSpeed = 2.6f;
    public static final float MilkSpeed = 2.1f;
    public static final ResourceLocation OilTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/environment/scp107_oil_rain.png");
    public static final float OilSpeed = 1.2f;
    public static final float SpeedSpread = 0.8f;
    public static final int FancyReach = 10;
    public static final int FastReach = 5;
    private static final float[] AcrossX = new float[1024];
    private static final float[] AcrossZ = new float[1024];
    private static int SoundTimer;

    static {
        for (int Row = 0; Row < 32; Row++) {
            for (int Column = 0; Column < 32; Column++) {
                float Dx = Column - 16;
                float Dz = Row - 16;
                float Length = Mth.sqrt(Dx * Dx + Dz * Dz);
                AcrossX[Row << 5 | Column] = Length > 0.0f ? -Dz / Length : 0.0f;
                AcrossZ[Row << 5 | Column] = Length > 0.0f ? Dx / Length : 0.0f;
            }
        }
    }

    private Scp107RainRenderer() {
    }

    public static void render(ClientLevel World, Scp107RainKind Kind, int Ticks, float Partial, LightTexture Lights, double CameraX, double CameraY, double CameraZ) {
        float Rain = World.getRainLevel(Partial);
        if (Rain <= 0.0f) {
            return;
        }
        Lights.turnOnLightLayer();
        int BaseX = Mth.floor(CameraX);
        int BaseY = Mth.floor(CameraY);
        int BaseZ = Mth.floor(CameraZ);
        int Reach = Minecraft.useFancyGraphics() ? FancyReach : FastReach;
        boolean Molten = Kind == Scp107RainKind.Lava;
        float Speed = switch (Kind) {
            case Lava -> LavaSpeed;
            case Milk -> MilkSpeed;
            case Oil -> OilSpeed;
            default -> CausticSpeed;
        };
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, switch (Kind) {
            case Lava -> LavaTexture;
            case Milk -> MilkTexture;
            case Oil -> OilTexture;
            default -> CausticTexture;
        });
        BufferBuilder Builder = null;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Z = BaseZ - Reach; Z <= BaseZ + Reach; Z++) {
            for (int X = BaseX - Reach; X <= BaseX + Reach; X++) {
                int Slot = (Z - BaseZ + 16) * 32 + X - BaseX + 16;
                double HalfX = AcrossX[Slot] * 0.5;
                double HalfZ = AcrossZ[Slot] * 0.5;
                if (HalfX == 0.0 && HalfZ == 0.0) {
                    continue;
                }
                int Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
                int Low = Math.max(BaseY - Reach, Ground);
                int High = Math.max(BaseY + Reach, Ground);
                if (Low == High) {
                    continue;
                }
                if (Builder == null) {
                    Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }
                RandomSource Random = RandomSource.create((long) (X * X * 3121 + X * 45238971 ^ Z * Z * 418711 + Z * 13761));
                int Cycle = Ticks & 131071;
                int Offset = X * X * 3121 + X * 45238971 + Z * Z * 418711 + Z * 13761 & 0xFF;
                float Rate = Speed + Random.nextFloat() * SpeedSpread;
                float Shift = (-((float) (Cycle + Offset) + Partial) / 32.0f * Rate) % 32.0f;
                double Dx = X + 0.5 - CameraX;
                double Dz = Z + 0.5 - CameraZ;
                float Distance = (float) Math.sqrt(Dx * Dx + Dz * Dz) / Reach;
                float Alpha = ((1.0f - Distance * Distance) * 0.5f + 0.5f) * Rain;
                Cursor.set(X, Math.max(Ground, BaseY), Z);
                int Light = Molten ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(World, Cursor);
                float Left = (float) (X - CameraX - HalfX + 0.5);
                float Right = (float) (X - CameraX + HalfX + 0.5);
                float Near = (float) (Z - CameraZ - HalfZ + 0.5);
                float Far = (float) (Z - CameraZ + HalfZ + 0.5);
                float Top = (float) (High - CameraY);
                float Bottom = (float) (Low - CameraY);
                Builder.addVertex(Left, Top, Near).setUv(0.0f, Low * 0.25f + Shift).setColor(1.0f, 1.0f, 1.0f, Alpha).setLight(Light);
                Builder.addVertex(Right, Top, Far).setUv(1.0f, Low * 0.25f + Shift).setColor(1.0f, 1.0f, 1.0f, Alpha).setLight(Light);
                Builder.addVertex(Right, Bottom, Far).setUv(1.0f, High * 0.25f + Shift).setColor(1.0f, 1.0f, 1.0f, Alpha).setLight(Light);
                Builder.addVertex(Left, Bottom, Near).setUv(0.0f, High * 0.25f + Shift).setColor(1.0f, 1.0f, 1.0f, Alpha).setLight(Light);
            }
        }
        if (Builder != null) {
            MeshData Mesh = Builder.build();
            if (Mesh != null) {
                BufferUploader.drawWithShader(Mesh);
            }
        }
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        Lights.turnOffLightLayer();
    }

    public static void tick(ClientLevel World, Scp107RainKind Kind, int Ticks, Camera View) {
        Minecraft Game = Minecraft.getInstance();
        float Rain = World.getRainLevel(1.0f) / (Minecraft.useFancyGraphics() ? 1.0f : 2.0f);
        if (Rain <= 0.0f) {
            return;
        }
        RandomSource Random = RandomSource.create((long) Ticks * 312987231L);
        BlockPos Eye = BlockPos.containing(View.getPosition());
        BlockPos Landing = null;
        ParticleStatus Status = Game.options.particles().get();
        int Count = (int) (100.0f * Rain * Rain) / (Status == ParticleStatus.DECREASED ? 2 : 1);
        for (int Index = 0; Index < Count; Index++) {
            int Dx = Random.nextInt(21) - 10;
            int Dz = Random.nextInt(21) - 10;
            BlockPos Top = World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, Eye.offset(Dx, 0, Dz));
            if (Top.getY() <= World.getMinBuildHeight() || Top.getY() > Eye.getY() + 10 || Top.getY() < Eye.getY() - 10) {
                continue;
            }
            Landing = Top.below();
            if (Status == ParticleStatus.MINIMAL) {
                break;
            }
            double PartX = Random.nextDouble();
            double PartZ = Random.nextDouble();
            BlockState Struck = World.getBlockState(Landing);
            FluidState Liquid = World.getFluidState(Landing);
            double Surface = Math.max(Struck.getCollisionShape(World, Landing).max(Direction.Axis.Y, PartX, PartZ), Liquid.getHeight(World, Landing));
            ParticleOptions Spray = splash(Kind, Liquid, Random);
            if (Spray != null) {
                World.addParticle(Spray, Landing.getX() + PartX, Landing.getY() + Surface, Landing.getZ() + PartZ, 0.0, 0.0, 0.0);
            }
        }
        if (Landing != null && Random.nextInt(3) < SoundTimer++) {
            SoundTimer = 0;
            boolean Covered = Landing.getY() > Eye.getY() + 1 && World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, Eye).getY() > Mth.floor((float) Eye.getY());
            float Volume = Covered ? 0.1f : 0.2f;
            float Pitch = Covered ? 0.5f : 1.0f;
            SoundEvent Bed = switch (Kind) {
                case Lava -> Scp107Registry.LavaRain.get();
                case Milk -> Scp107Registry.MilkRain.get();
                case Oil -> Scp107Registry.OilRain.get();
                default -> Scp107Registry.CausticRain.get();
            };
            SoundEvent Accent = switch (Kind) {
                case Lava -> Scp107Registry.Sizzle.get();
                case Milk -> Scp107Registry.MilkDrip.get();
                case Oil -> Scp107Registry.OilDrip.get();
                default -> Scp107Registry.Hiss.get();
            };
            World.playLocalSound(Landing, Bed, SoundSource.WEATHER, Volume, Pitch, false);
            if (Random.nextInt(4) == 0) {
                World.playLocalSound(Landing, Accent, SoundSource.WEATHER, Volume * 1.5f, Pitch, false);
            }
            if (Kind == Scp107RainKind.Lava && Random.nextInt(6) == 0) {
                World.playLocalSound(Landing, Scp107Registry.SizzlePop.get(), SoundSource.WEATHER, Volume * 1.5f, Pitch, false);
            }
        }
    }

    @Nullable
    private static ParticleOptions splash(Scp107RainKind Kind, FluidState Liquid, RandomSource Random) {
        if (Kind == Scp107RainKind.Lava) {
            if (Liquid.is(FluidTags.WATER) || Random.nextInt(4) == 0) {
                return ParticleTypes.SMOKE;
            }
            return Scp107Registry.Ember.get();
        }
        if (Liquid.is(FluidTags.LAVA)) {
            return ParticleTypes.SMOKE;
        }
        return switch (Kind) {
            case Milk -> Scp107Registry.MilkSplash.get();
            case Oil -> Scp107Registry.OilSplash.get();
            default -> Scp107Registry.CausticSplash.get();
        };
    }
}

package net.rai.scp_unleashed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.rai.scp_unleashed.particle.PipeLiquidOptions;

public class PipeLiquidParticle extends TextureSheetParticle {
    private static final float Window = 0.25f;

    private final boolean Suction;
    private final boolean Glows;
    private final ParticleOptions Landing;
    private final float CornerU;
    private final float CornerV;
    private final float BaseSize;
    private final double StartX;
    private final double StartY;
    private final double StartZ;
    private final double TravelX;
    private final double TravelY;
    private final double TravelZ;

    protected PipeLiquidParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, PipeLiquidOptions Options) {
        super(World, X, Y, Z);
        Fluid Liquid = Options.Liquid();
        IClientFluidTypeExtensions Extensions = IClientFluidTypeExtensions.of(Liquid);
        ResourceLocation Still = Extensions.getStillTexture();
        setSprite(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Still != null ? Still : MissingTextureAtlasSprite.getLocation()));
        int Colour = Extensions.getTintColor(Liquid.defaultFluidState(), World, BlockPos.containing(X, Y, Z));
        float Shade = 0.88f + random.nextFloat() * 0.12f;
        setColor(((Colour >> 16) & 0xFF) / 255.0f * Shade, ((Colour >> 8) & 0xFF) / 255.0f * Shade, (Colour & 0xFF) / 255.0f * Shade);
        Glows = Liquid.getFluidType().getLightLevel() > 0;
        setAlpha(Glows ? 1.0f : 0.85f);
        Landing = Liquid.defaultFluidState().is(FluidTags.LAVA) ? ParticleTypes.LANDING_LAVA : ParticleTypes.SPLASH;
        CornerU = random.nextFloat() * (1.0f - Window);
        CornerV = random.nextFloat() * (1.0f - Window);
        Suction = Options.Suction();
        StartX = X;
        StartY = Y;
        StartZ = Z;
        if (Suction) {
            TravelX = SpeedX * PipeLiquidOptions.SuctionTicks;
            TravelY = SpeedY * PipeLiquidOptions.SuctionTicks;
            TravelZ = SpeedZ * PipeLiquidOptions.SuctionTicks;
            lifetime = PipeLiquidOptions.SuctionTicks;
            hasPhysics = false;
            gravity = 0.0f;
            BaseSize = 0.035f + random.nextFloat() * 0.035f;
        } else {
            TravelX = 0.0;
            TravelY = 0.0;
            TravelZ = 0.0;
            xd = SpeedX;
            yd = SpeedY;
            zd = SpeedZ;
            gravity = 1.4f;
            friction = 0.98f;
            hasPhysics = true;
            lifetime = 40 + random.nextInt(20);
            BaseSize = 0.045f + random.nextFloat() * 0.045f;
        }
        quadSize = BaseSize;
        setSize(0.02f, 0.02f);
    }

    @Override
    public void tick() {
        if (Suction) {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            float Progress = (float) age / lifetime;
            float Pulled = Progress * Progress;
            setPos(StartX + TravelX * Pulled, StartY + TravelY * Pulled, StartZ + TravelZ * Pulled);
            quadSize = BaseSize * (1.0f - 0.45f * Pulled);
            return;
        }
        super.tick();
        if (!removed && (onGround || !level.getFluidState(BlockPos.containing(x, y, z)).isEmpty())) {
            level.addParticle(Landing, x, y + 0.05, z, 0.0, 0.0, 0.0);
            remove();
        }
    }

    @Override
    protected float getU0() {
        return sprite.getU(CornerU);
    }

    @Override
    protected float getU1() {
        return sprite.getU(CornerU + Window);
    }

    @Override
    protected float getV0() {
        return sprite.getV(CornerV);
    }

    @Override
    protected float getV1() {
        return sprite.getV(CornerV + Window);
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return Glows ? LightTexture.FULL_BRIGHT : super.getLightColor(PartialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static class Provider implements ParticleProvider<PipeLiquidOptions> {
        @Override
        public Particle createParticle(PipeLiquidOptions Options, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new PipeLiquidParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Options);
        }
    }
}

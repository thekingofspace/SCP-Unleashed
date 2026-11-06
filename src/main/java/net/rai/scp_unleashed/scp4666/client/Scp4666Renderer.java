package net.rai.scp_unleashed.scp4666.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp4666.Scp4666YuleMan;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Map;
import java.util.WeakHashMap;

public class Scp4666Renderer extends SharedGeoRenderer<Scp4666YuleMan> {
    public static final float Scale = 1.0f;
    public static final float Shadow = 0.5f;
    public static final String SackEnd = "sack_bottom";
    public static final float SackReach = 9.0f;
    public static final float PuffChance = 0.45f;
    public static final double PuffSpread = 0.12;
    public static final double PuffLift = 0.06;
    public static final double PuffRange = 4.0;
    public static final double CullReach = 2.0;
    public static final double CullRise = 0.5;
    public static final double PeekDrop = 4.5;

    private final Map<Scp4666YuleMan, Integer> Puffed = new WeakHashMap<>();

    public Scp4666Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp4666GeoModel());
        withScale(Scale);
        shadowRadius = Shadow;
        addRenderLayer(new Scp4666GlowLayer(this));
    }

    @Override
    public boolean shouldRender(Scp4666YuleMan Man, Frustum View, double CameraX, double CameraY, double CameraZ) {
        if (!Man.shouldRender(CameraX, CameraY, CameraZ)) {
            return false;
        }
        AABB Box = Man.getBoundingBox().inflate(CullReach, CullRise, CullReach);
        if (Man.mode() == Scp4666YuleMan.ModePeek) {
            Box = Box.expandTowards(0.0, -PeekDrop, 0.0);
        }
        return View.isVisible(Box);
    }

    @Override
    public void render(Scp4666YuleMan Man, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Man.setRenderPartial(PartialTick);
        super.render(Man, Yaw, PartialTick, Pose, Buffers, Light);
        puff(Man);
    }

    private void puff(Scp4666YuleMan Man) {
        if (!Man.striding() || !Scp4666GeoModel.showsSack(Man)) {
            return;
        }
        Integer Last = Puffed.get(Man);
        if (Last != null && Last == Man.tickCount) {
            return;
        }
        Puffed.put(Man, Man.tickCount);
        Level World = Man.level();
        if (World.getRandom().nextFloat() > PuffChance) {
            return;
        }
        GeoBone End = getGeoModel().getAnimationProcessor().getBone(SackEnd);
        if (End == null) {
            return;
        }
        Matrix4f Matrix = End.getWorldSpaceMatrix();
        Vector4f Point = Matrix.transform(new Vector4f(End.getPivotX() / 16.0f, (End.getPivotY() - SackReach) / 16.0f, End.getPivotZ() / 16.0f, 1.0f));
        if (!Float.isFinite(Point.x()) || Math.abs(Point.x() - Man.getX()) > PuffRange || Math.abs(Point.z() - Man.getZ()) > PuffRange) {
            return;
        }
        BlockPos Under = BlockPos.containing(Point.x(), Point.y() - 0.15, Point.z());
        BlockState Ground = World.getBlockState(Under);
        BlockState Above = World.getBlockState(Under.above());
        if (!Ground.is(BlockTags.SNOW) && !Above.is(BlockTags.SNOW)) {
            return;
        }
        double Dx = (World.getRandom().nextDouble() - 0.5) * PuffSpread;
        double Dz = (World.getRandom().nextDouble() - 0.5) * PuffSpread;
        World.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), Point.x(), Point.y() + 0.05, Point.z(), Dx, PuffLift, Dz);
    }
}

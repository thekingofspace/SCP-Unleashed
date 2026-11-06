package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001GeneratorBlockEntity;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class Scp5001GeneratorRenderer extends GeoBlockRenderer<Scp5001GeneratorBlockEntity> {
    public static final int ViewDistance = 96;
    public static final double[][] Heads = {{-10.0, -10.0}, {10.0, -10.0}, {10.0, 10.0}, {-10.0, 10.0}};

    public Scp5001GeneratorRenderer(BlockEntityRendererProvider.Context Context) {
        super(new Scp5001GeneratorModel());
        addRenderLayer(new Scp5001GlowLayer<>(this, Owner -> Owner.overdriven() ? Scp5001GeneratorModel.HotGlowPath : Scp5001GeneratorModel.GlowPath,
                Owner -> Owner.overdriven() ? 0.72 + 0.1 * Math.sin((Owner.getLevel() == null ? 0 : Owner.getLevel().getGameTime()) * 0.9)
                        : 0.62 + 0.08 * Math.sin((Owner.getLevel() == null ? 0 : Owner.getLevel().getGameTime()) * 0.08)));
    }

    @Override
    public void render(Scp5001GeneratorBlockEntity Generator, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        super.render(Generator, Partial, Pose, Buffers, Light, Overlay);
        if (!Generator.overdriven() || Generator.getLevel() == null) {
            return;
        }
        long Now = Generator.getLevel().getGameTime();
        RandomSource Dice = RandomSource.create(Generator.getBlockPos().asLong() * 31 + Now / 2);
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.lightning());
        Matrix4f Matrix = Pose.last().pose();
        double[] Heights = Scp5001GeneratorBlockEntity.CoilHeights;
        int Count = 2 + Dice.nextInt(3);
        for (int Index = 0; Index < Count; Index++) {
            int Lower = Dice.nextInt(Heights.length - 1);
            double Angle = Dice.nextDouble() * Mth.TWO_PI;
            double Twist = (Dice.nextDouble() - 0.5) * 1.1;
            double Reach = Scp5001GeneratorBlockEntity.CoilReach;
            Vec3 From = new Vec3(0.5 + Math.cos(Angle) * Reach, Heights[Lower], 0.5 + Math.sin(Angle) * Reach);
            Vec3 To = new Vec3(0.5 + Math.cos(Angle + Twist) * Reach, Heights[Lower + 1], 0.5 + Math.sin(Angle + Twist) * Reach);
            arc(Matrix, Buffer, From, To, Dice);
        }
        if (Dice.nextInt(3) == 0) {
            double[] Head = Heads[Dice.nextInt(Heads.length)];
            Vec3 From = new Vec3(0.5 + Head[0] / 16.0, Scp5001GeneratorBlockEntity.StackTop, 0.5 + Head[1] / 16.0);
            Vec3 To = new Vec3(0.5, 49.0 / 16.0, 0.5);
            arc(Matrix, Buffer, From, To, Dice);
        }
    }

    private static void arc(Matrix4f Matrix, VertexConsumer Buffer, Vec3 From, Vec3 To, RandomSource Dice) {
        int Pieces = 6;
        Vec3 Last = From;
        for (int Index = 1; Index <= Pieces; Index++) {
            double Share = Index / (double) Pieces;
            double Spread = Index == Pieces ? 0.0 : 0.16 * Mth.sin((float) (Share * Math.PI));
            Vec3 Next = From.lerp(To, Share).add((Dice.nextDouble() - 0.5) * Spread * 2.0, (Dice.nextDouble() - 0.5) * Spread * 2.0, (Dice.nextDouble() - 0.5) * Spread * 2.0);
            Scp5001Beams.ribbon(Matrix, Buffer, Last, Next, 0.05f, 225, 248, 255, 235);
            Scp5001Beams.ribbon(Matrix, Buffer, Last, Next, 0.17f, 143, 227, 255, 55);
            Last = Next;
        }
    }

    @Override
    public int getViewDistance() {
        return ViewDistance;
    }

    @Override
    public AABB getRenderBoundingBox(Scp5001GeneratorBlockEntity Generator) {
        return new AABB(Generator.getBlockPos()).inflate(1.5, 0.0, 1.5).expandTowards(0.0, 3.0, 0.0);
    }
}

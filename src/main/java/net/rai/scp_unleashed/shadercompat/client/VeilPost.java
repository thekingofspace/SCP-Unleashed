package net.rai.scp_unleashed.shadercompat.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.post.stage.BlitPostStage;
import foundry.veil.api.client.render.post.stage.CompositePostPipeline;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

public final class VeilPost {
    private static final Logger Log = LogUtils.getLogger();
    private static final Set<ResourceLocation> Warned = new HashSet<>();
    private static final Set<ResourceLocation> Broken = new HashSet<>();

    private VeilPost() {
    }

    public static ResourceLocation id(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
    }

    public static ResourceLocation program(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
    }

    public static boolean programReady(ResourceLocation Id) {
        try {
            VeilRenderer Renderer = VeilRenderSystem.renderer();
            if (Renderer == null) {
                return false;
            }
            ShaderProgram Program = Renderer.getShaderManager().getShader(Id);
            return Program != null && Program.isValid();
        } catch (Throwable Problem) {
            return false;
        }
    }

    public static boolean available() {
        if (ShaderCompat.skipPostEffects()) {
            return false;
        }
        try {
            return VeilRenderSystem.renderer() != null;
        } catch (Throwable Problem) {
            return false;
        }
    }

    public static boolean ready(ResourceLocation Id) {
        return available() && find(Id) != null;
    }

    public static boolean run(ResourceLocation Id) {
        return run(Id, Values -> {
        });
    }

    public static boolean run(ResourceLocation Id, Consumer<Uniforms> Setup) {
        if (!available()) {
            return false;
        }
        PostPipeline Pipeline = find(Id);
        if (Pipeline == null) {
            return false;
        }
        Setup.accept(new Uniforms(Pipeline));
        Minecraft Client = Minecraft.getInstance();
        RenderTarget Main = Client.getMainRenderTarget();
        boolean DepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        try {
            VeilRenderSystem.renderer().getPostProcessingManager().runPipeline(Pipeline);
        } catch (RuntimeException Problem) {
            Broken.add(Id);
            Log.warn("Veil post pipeline {} failed, its effect falls back to the overlay", Id, Problem);
            return false;
        } finally {
            Main.bindWrite(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            if (DepthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
        }
        return true;
    }

    private static PostPipeline find(ResourceLocation Id) {
        VeilRenderer Renderer;
        try {
            Renderer = VeilRenderSystem.renderer();
        } catch (Throwable Problem) {
            return null;
        }
        if (Renderer == null || Broken.contains(Id)) {
            return null;
        }
        PostProcessingManager Manager = Renderer.getPostProcessingManager();
        PostPipeline Pipeline = Manager.getPipeline(Id);
        if (Pipeline == null) {
            warn(Id, "is not loaded");
            return null;
        }
        if (Pipeline instanceof CompositePostPipeline Composite) {
            for (PostPipeline Stage : Composite.getStages()) {
                if (Stage instanceof BlitPostStage Blit) {
                    ShaderProgram Program = Blit.getShader();
                    if (Program == null || !Program.isValid()) {
                        warn(Id, "has a stage whose shader " + Blit.getShaderId() + " did not compile");
                        return null;
                    }
                }
            }
        }
        return Pipeline;
    }

    private static void warn(ResourceLocation Id, String Reason) {
        if (Warned.add(Id)) {
            Log.warn("Veil post pipeline {} {}, its effect falls back to the overlay", Id, Reason);
        }
    }

    public static final class Uniforms {
        private final PostPipeline Pipeline;

        private Uniforms(PostPipeline Pipeline) {
            this.Pipeline = Pipeline;
        }

        public Uniforms set(String Name, float Value) {
            Pipeline.getUniformSafe(Name).setFloat(Value);
            return this;
        }

        public Uniforms set(String Name, float First, float Second) {
            Pipeline.getUniformSafe(Name).setVector(First, Second);
            return this;
        }

        public Uniforms set(String Name, float First, float Second, float Third) {
            Pipeline.getUniformSafe(Name).setVector(First, Second, Third);
            return this;
        }

        public Uniforms set(String Name, float First, float Second, float Third, float Fourth) {
            Pipeline.getUniformSafe(Name).setVector(First, Second, Third, Fourth);
            return this;
        }
    }
}

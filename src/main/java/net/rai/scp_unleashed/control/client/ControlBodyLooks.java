package net.rai.scp_unleashed.control.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import net.rai.scp_unleashed.control.ControlBodies;

import java.util.Map;
import java.util.WeakHashMap;

public final class ControlBodyLooks {
    private record Hosted(LivingEntity Body, @Nullable ControlLook Look, CompoundTag Identity, ClientLevel World, int Tick) {
    }

    private static final Map<LivingEntity, Hosted> Hosts = new WeakHashMap<>();

    private ControlBodyLooks() {
    }

    public static boolean render(LivingEntity Host, CompoundTag Saved, float Yaw, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        if (Saved.isEmpty() || !(Host.level() instanceof ClientLevel World)) {
            return false;
        }
        ResourceLocation Id = ResourceLocation.tryParse(Saved.getString(ControlBodies.LookKey));
        CompoundTag Identity = Saved.getCompound(ControlBodies.IdentityKey);
        if (Id == null) {
            return false;
        }
        ControlLook Look = ControlLooks.get(Id);
        Hosted Current = Hosts.get(Host);
        if (Current == null || Current.World() != World || !Current.Identity().equals(Identity)) {
            LivingEntity Body = Look != null ? Look.createPuppet(World, Identity) : dummy(World, Id, Identity);
            if (Body == null) {
                return false;
            }
            Current = new Hosted(Body, Look, Identity, World, -1);
            Hosts.put(Host, Current);
        }
        if (Current.Tick() != Host.tickCount) {
            if (Current.Look() != null) {
                Current.Look().mirrorHost(Current.Body(), Host, Current.Identity());
                Current.Look().tickHosted(Current.Body());
            } else {
                ControlPuppets.mirrorBasics(Current.Body(), Host, Current.Identity());
                Current.Body().tickCount++;
            }
            Hosts.put(Host, new Hosted(Current.Body(), Current.Look(), Current.Identity(), Current.World(), Host.tickCount));
        }
        LivingEntity Body = Current.Body();
        Body.setCustomName(Host.getCustomName());
        Body.setCustomNameVisible(Host.isCustomNameVisible());
        EntityRenderer<? super LivingEntity> Renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(Body);
        Renderer.render(Body, Yaw, PartialTick, Matrices, Buffers, Light);
        return true;
    }

    @Nullable
    private static LivingEntity dummy(ClientLevel World, ResourceLocation Id, CompoundTag Identity) {
        EntityType<?> Type = BuiltInRegistries.ENTITY_TYPE.getOptional(Id).orElse(null);
        Entity Made = Type == null ? null : Type.create(World);
        if (!(Made instanceof LivingEntity Body)) {
            return null;
        }
        try {
            Body.load(Identity);
        } catch (RuntimeException Broken) {
            return null;
        }
        return Body;
    }

    public static void clear() {
        Hosts.clear();
    }
}

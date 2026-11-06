package net.rai.scp_unleashed.scp215.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp215.Scp215GlassesItem;
import net.rai.scp_unleashed.scp215.Scp215Phantom;
import net.rai.scp_unleashed.scp215.Scp215Registry;
import net.rai.scp_unleashed.scp215.Scp215Variant;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp215Disguises {
    public static final int FirstPuppetId = -1_000_000;
    public static final int LastPuppetId = -1_999_999;
    public static final int SweepInterval = 200;
    private static final Map<UUID, Scp215Phantom> Puppets = new HashMap<>();
    @Nullable
    private static Level Home;
    private static int Sweep;
    private static int NextPuppetId = FirstPuppetId;
    private static boolean Drawing;

    private Scp215Disguises() {
    }

    public static boolean seeing(Minecraft Game) {
        Player User = Game.player;
        return User != null && !User.isSpectator() && Scp215GlassesItem.isWorn(User);
    }

    public static boolean render(LivingEntity Target, float PartialTick, PoseStack Matrices, MultiBufferSource Buffers, int Light) {
        Minecraft Game = Minecraft.getInstance();
        if (Drawing || !seeing(Game) || Target == Game.player || Target == Game.getCameraEntity() || Game.player == null) {
            return false;
        }
        if (Target.isInvisibleTo(Game.player) || Target.isSleeping()) {
            return false;
        }
        Scp215Phantom Puppet = puppetFor(Target);
        if (Puppet == null) {
            return false;
        }
        Puppet.mirror(Target);
        EntityRenderer<? super Scp215Phantom> Renderer = Game.getEntityRenderDispatcher().getRenderer(Puppet);
        Drawing = true;
        try {
            Renderer.render(Puppet, Puppet.bodyYaw(PartialTick), PartialTick, Matrices, Buffers, Light);
        } finally {
            Drawing = false;
        }
        return true;
    }

    @Nullable
    private static Scp215Phantom puppetFor(LivingEntity Target) {
        Level World = Target.level();
        if (World != Home) {
            Puppets.clear();
            Home = World;
        }
        Scp215Phantom Existing = Puppets.get(Target.getUUID());
        if (Existing != null) {
            return Existing;
        }
        Scp215Phantom Made = new Scp215Phantom(Scp215Registry.Phantom.get(), World);
        Made.setId(NextPuppetId--);
        if (NextPuppetId < LastPuppetId) {
            NextPuppetId = FirstPuppetId;
        }
        Made.becomePuppet(Scp215Variant.forUuid(Target.getUUID()));
        Puppets.put(Target.getUUID(), Made);
        return Made;
    }

    public static void tick(Minecraft Game) {
        if (++Sweep < SweepInterval) {
            return;
        }
        Sweep = 0;
        if (Game.level == null || Game.level != Home || !seeing(Game)) {
            Puppets.clear();
            Home = Game.level;
            return;
        }
        Puppets.keySet().removeIf(Id -> Game.level.getPlayerByUUID(Id) == null && !nearby(Game, Id));
    }

    private static boolean nearby(Minecraft Game, UUID Id) {
        if (Game.level == null) {
            return false;
        }
        for (Entity Each : Game.level.entitiesForRendering()) {
            if (Each.getUUID().equals(Id)) {
                return true;
            }
        }
        return false;
    }

    public static void reset() {
        Puppets.clear();
        Home = null;
        Sweep = 0;
    }
}

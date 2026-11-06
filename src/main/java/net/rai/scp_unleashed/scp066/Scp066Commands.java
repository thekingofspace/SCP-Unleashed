package net.rai.scp_unleashed.scp066;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp066Commands {
    public static final double SearchRange = 48.0;
    public static final double AimReach = 16.0;

    private Scp066Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp066")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("calm").executes(Context -> calm(Context.getSource())))
                        .then(Commands.literal("anger").executes(Context -> anger(Context.getSource())))
                        .then(Commands.literal("treat").executes(Context -> treat(Context.getSource())))));
    }

    @Nullable
    private static Scp066Entity aimed(CommandSourceStack Source) {
        Entity Looker = Source.getEntity();
        if (Looker == null) {
            return null;
        }
        Vec3 Eye = Looker.getEyePosition();
        Vec3 End = Eye.add(Looker.getViewVector(1.0f).scale(AimReach));
        AABB Sweep = Looker.getBoundingBox().expandTowards(Looker.getViewVector(1.0f).scale(AimReach)).inflate(1.0);
        EntityHitResult Hit = ProjectileUtil.getEntityHitResult(Source.getLevel(), Looker, Eye, End, Sweep, Candidate -> Candidate instanceof Scp066Entity);
        return Hit != null && Hit.getEntity() instanceof Scp066Entity Heap ? Heap : null;
    }

    @Nullable
    private static Scp066Entity find(CommandSourceStack Source) {
        Scp066Entity Aimed = aimed(Source);
        if (Aimed != null) {
            return Aimed;
        }
        Vec3 From = Source.getPosition();
        Scp066Entity Near = Source.getLevel().getEntitiesOfClass(Scp066Entity.class, new AABB(From, From).inflate(SearchRange), Entity::isAlive).stream()
                .min(Comparator.comparingDouble(Heap -> Heap.distanceToSqr(From))).orElse(null);
        if (Near == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp066.none", (int) SearchRange));
        }
        return Near;
    }

    private static int calm(CommandSourceStack Source) {
        Scp066Entity Heap = find(Source);
        if (Heap == null) {
            return 0;
        }
        Heap.resetCalm();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp066.calmed", Heap.blockPosition().getX(), Heap.blockPosition().getY(),
                Heap.blockPosition().getZ()), true);
        return 1;
    }

    private static int anger(CommandSourceStack Source) {
        Scp066Entity Heap = find(Source);
        if (Heap == null) {
            return 0;
        }
        if (!Heap.isCalm()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp066.already_angry"));
            return 0;
        }
        Heap.beginChange(Source.getEntity() instanceof LivingEntity Living ? Living : null);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp066.angered", Heap.blockPosition().getX(), Heap.blockPosition().getY(),
                Heap.blockPosition().getZ()), true);
        return 1;
    }

    private static int treat(CommandSourceStack Source) {
        if (!(Source.getEntity() instanceof Player User)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp066.player_only"));
            return 0;
        }
        Scp066Entity Heap = find(Source);
        if (Heap == null) {
            return 0;
        }
        if (!Heap.giveTreat(User, true)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp066.no_treat"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp066.treated"), false);
        return 1;
    }
}

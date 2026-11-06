package net.rai.scp_unleashed.scp035;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp035Commands {
    public static final String Prefix = "commands.scp_unleashed.scp035.";
    public static final double PlaceReach = 6.0;

    private Scp035Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp035")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("place")
                                .executes(Scp035Commands::placeAhead)
                                .then(Commands.argument("position", Vec3Argument.vec3())
                                        .executes(Context -> place(Context.getSource(), Vec3Argument.getVec3(Context, "position")))))
                        .then(targeted("release", Scp035Commands::release))
                        .then(targeted("host", Scp035Commands::host))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<ServerPlayer>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getPlayers(Context, "targets"))));
    }

    private static int placeAhead(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        Entity Looker = Source.getEntityOrException();
        Vec3 Eye = Looker.getEyePosition();
        Vec3 End = Eye.add(Looker.getViewVector(1.0f).scale(PlaceReach));
        BlockHitResult Hit = Source.getLevel().clip(new ClipContext(Eye, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Looker));
        Vec3 Spot = Hit.getType() == HitResult.Type.MISS ? End : Hit.getLocation().add(Vec3.atLowerCornerOf(Hit.getDirection().getNormal()).scale(0.2));
        return place(Source, Spot);
    }

    private static int place(CommandSourceStack Source, Vec3 Spot) {
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() : 0.0f;
        Scp035Mask Placed = Scp035Mask.place(Source.getLevel(), Spot, Yaw, true);
        if (Placed == null) {
            Source.sendFailure(Component.translatable(Prefix + "place.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "place.success", Math.round(Spot.x), Math.round(Spot.y), Math.round(Spot.z)), true);
        return 1;
    }

    private static int release(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            boolean Worn = Scp035MaskItem.is(Target.getItemBySlot(EquipmentSlot.HEAD));
            if (Worn) {
                Target.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
            int Taken = Scp035Possession.takeAll(Target).size() + (Worn ? 1 : 0);
            Scp035Carry.reset(Target);
            Scp035Lure.resend(Target);
            if (Taken > 0) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "release.success", Target.getDisplayName(), Taken), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "release.none", Target.getDisplayName()));
            }
        }
        return Count;
    }

    private static int host(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp035Host.rise(Target.serverLevel(), Target, Scp035MaskItem.fresh()) != null) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "host.success", Target.getDisplayName()), true);
            }
        }
        return Count;
    }
}

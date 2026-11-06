package net.rai.scp_unleashed.gateway;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record GatewayDestination(ResourceLocation Id, ResourceKey<Level> Dimension, int Tint, int ShardVariant, int WarningLines, EntryPoint Entry, Rules Rules, Hooks Hooks) {
    public static final Rules OpenTravel = new Rules(false, false, false, false);
    public static final Rules SealedTravel = new Rules(true, true, true, true);

    public record Arrival(Vec3 Pos, float Yaw) {
    }

    public record Rules(boolean StashBelongings, boolean StashExperience, boolean KeepFoundOnLeave, boolean WipeOnDeath) {
    }

    @FunctionalInterface
    public interface EntryPoint {
        Arrival locate(ServerLevel Target, ServerPlayer User);
    }

    public interface Hooks {
        Hooks None = new Hooks() {
        };

        default boolean canEnter(ServerPlayer User, ServerLevel Target) {
            return true;
        }

        default boolean binds(ServerPlayer User) {
            return true;
        }

        default void prepare(ServerLevel Target) {
        }

        default void arrived(ServerPlayer User, ServerLevel Target) {
        }

        default void left(ServerPlayer User, ServerLevel From) {
        }

        default void died(ServerPlayer User, ServerLevel Where) {
        }
    }

    public String langPrefix() {
        return "gateway." + Id.getNamespace() + ".destination." + Id.getPath();
    }

    public Component displayName() {
        return Component.translatable(langPrefix() + ".name");
    }

    public Component warningTitle() {
        return Component.translatable(langPrefix() + ".warning.title");
    }

    public List<Component> warningLines() {
        List<Component> Lines = new ArrayList<>();
        for (int Index = 0; Index < WarningLines; Index++) {
            Lines.add(Component.translatable(langPrefix() + ".warning." + Index));
        }
        return Lines;
    }

    public float red() {
        return (Tint >> 16 & 255) / 255.0f;
    }

    public float green() {
        return (Tint >> 8 & 255) / 255.0f;
    }

    public float blue() {
        return (Tint & 255) / 255.0f;
    }
}

package net.rai.scp_unleashed.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.Event;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VoicechatProbe {
    public static final String ManagerClass = "de.maxhenkel.voicechat.plugins.PluginManager";

    private VoicechatProbe() {
    }

    public record Sample(MicrophonePacketEvent Packet, AtomicBoolean Cancelled) {
    }

    @Nullable
    public static String check(ServerPlayer Silenced, ServerPlayer Free) {
        if (!ScpVoicechatPlugin.isInitialized() || !ScpVoicechatPlugin.isRegistered()) {
            return "Simple Voice Chat did not load the SCP Unleashed plugin";
        }
        Sample Direct = sample(Silenced);
        ScpVoicechatPlugin.microphone(Direct.Packet());
        if (!Direct.Cancelled().get()) {
            return "the plugin let a silenced speaker through";
        }
        Sample Clear = sample(Free);
        ScpVoicechatPlugin.microphone(Clear.Packet());
        if (Clear.Cancelled().get()) {
            return "the plugin muted a speaker who is not silenced";
        }
        Sample Routed = sample(Silenced);
        Sample Allowed = sample(Free);
        try {
            Object Manager = Class.forName(ManagerClass).getMethod("instance").invoke(null);
            Method Dispatch = Manager.getClass().getMethod("dispatchEvent", Class.class, Event.class);
            Dispatch.invoke(Manager, MicrophonePacketEvent.class, Routed.Packet());
            Dispatch.invoke(Manager, MicrophonePacketEvent.class, Allowed.Packet());
        } catch (ReflectiveOperationException | RuntimeException Problem) {
            return "could not dispatch through Simple Voice Chat: " + Problem;
        }
        if (!Routed.Cancelled().get()) {
            return "a microphone packet dispatched by Simple Voice Chat was not cancelled for a silenced speaker";
        }
        if (Allowed.Cancelled().get()) {
            return "a microphone packet dispatched by Simple Voice Chat was cancelled for a free speaker";
        }
        return null;
    }

    public static Sample sample(ServerPlayer Speaker) {
        AtomicBoolean Cancelled = new AtomicBoolean();
        Object Body = Proxy.newProxyInstance(VoicechatProbe.class.getClassLoader(), new Class<?>[]{de.maxhenkel.voicechat.api.ServerPlayer.class}, (Self, Called, Arguments) -> switch (Called.getName()) {
            case "getPlayer", "getEntity" -> Speaker;
            case "getUuid" -> Speaker.getUUID();
            case "hashCode" -> System.identityHashCode(Self);
            case "equals" -> Self == Arguments[0];
            case "toString" -> "ProbePlayer[" + Speaker.getGameProfile().getName() + "]";
            default -> null;
        });
        Object Link = Proxy.newProxyInstance(VoicechatProbe.class.getClassLoader(), new Class<?>[]{VoicechatConnection.class}, (Self, Called, Arguments) -> switch (Called.getName()) {
            case "getPlayer" -> Body;
            case "isConnected", "isInstalled" -> true;
            case "isInGroup", "isDisabled" -> false;
            case "hashCode" -> System.identityHashCode(Self);
            case "equals" -> Self == Arguments[0];
            case "toString" -> "ProbeConnection";
            default -> null;
        });
        Object Made = Proxy.newProxyInstance(VoicechatProbe.class.getClassLoader(), new Class<?>[]{MicrophonePacketEvent.class}, (Self, Called, Arguments) -> switch (Called.getName()) {
            case "getSenderConnection" -> Link;
            case "isCancellable" -> true;
            case "cancel" -> {
                Cancelled.set(true);
                yield true;
            }
            case "isCancelled" -> Cancelled.get();
            case "hashCode" -> System.identityHashCode(Self);
            case "equals" -> Self == Arguments[0];
            case "toString" -> "ProbeMicrophonePacket";
            default -> null;
        });
        return new Sample((MicrophonePacketEvent) Made, Cancelled);
    }
}

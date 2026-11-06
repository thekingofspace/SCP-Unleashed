package net.rai.scp_unleashed.senses.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.client.event.sound.PlayStreamingSourceEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.space.SpaceTags;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class EarplugMuffle {
    public static final float DuckedVolume = 0.1f;
    public static final float MuffledTreble = 0.05f;
    public static final float FadeStep = 0.1f;
    public static final float SuitMuffle = 0.35f;

    private static final Logger Log = LogUtils.getLogger();
    private static final List<DoubleSupplier> Sources = new CopyOnWriteArrayList<>();
    private static final List<Predicate<SoundInstance>> Exemptions = new CopyOnWriteArrayList<>();
    private static final Set<Channel> Unfiltered = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static float Fade;
    private static float AppliedFade;
    private static boolean Ducking;
    private static volatile float FilterFade;
    private static int Filter;
    private static long FilterContext;
    private static boolean Broken;
    private static Field SourceHandle;
    private static Field EngineHandle;
    private static Field AccessHandle;

    private EarplugMuffle() {
    }

    public static float fade() {
        return Fade;
    }

    public static void addSource(DoubleSupplier Source) {
        Sources.add(Source);
    }

    public static void addExemption(Predicate<SoundInstance> Exemption) {
        Exemptions.add(Exemption);
    }

    private static void exempt(SoundInstance Instance, Channel Playing) {
        for (Predicate<SoundInstance> Exemption : Exemptions) {
            if (Exemption.test(Instance)) {
                Unfiltered.add(Playing);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        float Target = target(Client.player);
        Fade = Fade < Target ? Math.min(Target, Fade + FadeStep) : Math.max(Target, Fade - FadeStep);
        float Master = Client.options.getSoundSourceVolume(SoundSource.MASTER);
        SoundManager Sounds = Client.getSoundManager();
        if (Fade > 0.0f) {
            if (Master > 0.0f) {
                Sounds.updateSourceVolume(SoundSource.MASTER, Master * Mth.lerp(Fade, 1.0f, DuckedVolume));
            }
            Ducking = true;
        } else if (Ducking) {
            Sounds.updateSourceVolume(SoundSource.MASTER, Master);
            Ducking = false;
        }
        if (Fade != AppliedFade) {
            AppliedFade = Fade;
            FilterFade = Fade;
            refilter(Sounds);
        }
    }

    private static float target(Player Listener) {
        if (Listener == null) {
            return 0.0f;
        }
        if (Senses.isDeafened(Listener)) {
            return 1.0f;
        }
        float Worn = Listener.getItemBySlot(EquipmentSlot.HEAD).is(SpaceTags.SpaceSuit) ? SuitMuffle : 0.0f;
        for (DoubleSupplier Source : Sources) {
            Worn = Math.max(Worn, Mth.clamp((float) Source.getAsDouble(), 0.0f, 1.0f));
        }
        return Worn;
    }

    @SubscribeEvent
    public static void started(PlaySoundSourceEvent Event) {
        exempt(Event.getSound(), Event.getChannel());
        if (FilterFade > 0.0f) {
            filterOne(Event.getChannel());
        }
    }

    @SubscribeEvent
    public static void streamed(PlayStreamingSourceEvent Event) {
        exempt(Event.getSound(), Event.getChannel());
        if (FilterFade > 0.0f) {
            filterOne(Event.getChannel());
        }
    }

    private static void refilter(SoundManager Sounds) {
        if (Broken) {
            return;
        }
        try {
            if (EngineHandle == null) {
                EngineHandle = ObfuscationReflectionHelper.findField(SoundManager.class, "soundEngine");
                AccessHandle = ObfuscationReflectionHelper.findField(SoundEngine.class, "channelAccess");
            }
            SoundEngine Engine = (SoundEngine) EngineHandle.get(Sounds);
            ChannelAccess Access = (ChannelAccess) AccessHandle.get(Engine);
            Access.executeOnChannels(Channels -> {
                if (prepare()) {
                    Channels.forEach(EarplugMuffle::attach);
                }
            });
        } catch (Throwable Failure) {
            disable(Failure);
        }
    }

    private static void filterOne(Channel Playing) {
        if (!Broken && prepare()) {
            attach(Playing);
        }
    }

    private static boolean prepare() {
        if (Broken) {
            return false;
        }
        try {
            if (!AL.getCapabilities().ALC_EXT_EFX) {
                Broken = true;
                return false;
            }
            long Context = ALC10.alcGetCurrentContext();
            if (Filter == 0 || Context != FilterContext || !EXTEfx.alIsFilter(Filter)) {
                Filter = EXTEfx.alGenFilters();
                EXTEfx.alFilteri(Filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
                FilterContext = Context;
            }
            EXTEfx.alFilterf(Filter, EXTEfx.AL_LOWPASS_GAIN, 1.0f);
            EXTEfx.alFilterf(Filter, EXTEfx.AL_LOWPASS_GAINHF, Mth.lerp(FilterFade, 1.0f, MuffledTreble));
            AL10.alGetError();
            return true;
        } catch (Throwable Failure) {
            disable(Failure);
            return false;
        }
    }

    private static void attach(Channel Playing) {
        try {
            if (SourceHandle == null) {
                SourceHandle = ObfuscationReflectionHelper.findField(Channel.class, "source");
            }
            int Source = SourceHandle.getInt(Playing);
            if (!AL10.alIsSource(Source)) {
                return;
            }
            AL10.alSourcei(Source, EXTEfx.AL_DIRECT_FILTER, FilterFade > 0.0f && !Unfiltered.contains(Playing) ? Filter : EXTEfx.AL_FILTER_NULL);
            AL10.alGetError();
        } catch (Throwable Failure) {
            disable(Failure);
        }
    }

    private static void disable(Throwable Failure) {
        if (!Broken) {
            Broken = true;
            Log.warn("Earplug low-pass muffling is unavailable, falling back to volume only", Failure);
        }
    }
}

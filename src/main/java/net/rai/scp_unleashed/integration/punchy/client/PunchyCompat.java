package net.rai.scp_unleashed.integration.punchy.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class PunchyCompat {
    public static final String ModId = "punchy";
    public static final String ConfigClass = "punchy.config.PunchyConfig";
    public static final String ConfigField = "DATA";
    public static final String BlacklistName = "itemBlacklist";
    public static final String DualHandedName = "blacklistApplyDualHanded";
    public static final String HandTransform = "applyForgeHandTransform";

    private static final Logger Log = LogUtils.getLogger();
    private static boolean Checked;
    private static boolean Active;
    private static Field ConfigHandle;
    private static Field BlacklistHandle;
    private static Field DualHandedHandle;
    private static Object SeenConfig;
    private static Object SeenBlacklist;
    private static Map<String, Boolean> Wanted;

    private PunchyCompat() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        if (!Checked) {
            Checked = true;
            Active = ModList.get().isLoaded(ModId) && resolve();
        }
        if (Active) {
            keep();
        }
    }

    private static Map<String, Boolean> wanted() {
        if (Wanted == null) {
            Map<String, Boolean> Found = new LinkedHashMap<>();
            for (Item Registered : BuiltInRegistries.ITEM) {
                ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Registered);
                if (!Scp_unleashed.ModId.equals(Id.getNamespace())) {
                    continue;
                }
                if (Registered instanceof GunItem) {
                    Found.put(Id.toString(), true);
                } else if (ownHandAnimation(Registered)) {
                    Found.put(Id.toString(), false);
                }
            }
            Wanted = Found;
        }
        return Wanted;
    }

    private static boolean ownHandAnimation(Item Registered) {
        IClientItemExtensions Extensions = IClientItemExtensions.of(Registered);
        for (Method Candidate : Extensions.getClass().getMethods()) {
            if (HandTransform.equals(Candidate.getName()) && Candidate.getDeclaringClass() != IClientItemExtensions.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean resolve() {
        try {
            Class<?> Config = Class.forName(ConfigClass, false, PunchyCompat.class.getClassLoader());
            Field Handle = Config.getDeclaredField(ConfigField);
            Handle.setAccessible(true);
            ConfigHandle = Handle;
            Log.info("Punchy found: SCP Unleashed guns and items with their own first person animation stay on its item blacklist");
            return true;
        } catch (Throwable Problem) {
            Log.warn("Punchy is present but its config could not be reached, its item blacklist is left alone", Problem);
            return false;
        }
    }

    private static void keep() {
        try {
            Object Live = ConfigHandle.get(null);
            if (Live == null) {
                return;
            }
            if (BlacklistHandle == null) {
                BlacklistHandle = Live.getClass().getField(BlacklistName);
                DualHandedHandle = optionalField(Live.getClass(), DualHandedName);
            }
            Object Listed = BlacklistHandle.get(Live);
            if (Live == SeenConfig && Listed == SeenBlacklist) {
                return;
            }
            List<String> Blacklist = new ArrayList<>();
            if (Listed instanceof List<?> Old) {
                for (Object Value : Old) {
                    if (Value instanceof String Text) {
                        Blacklist.add(Text);
                    }
                }
            }
            boolean Added = false;
            for (String Key : wanted().keySet()) {
                if (!Blacklist.contains(Key)) {
                    Blacklist.add(Key);
                    Added = true;
                }
            }
            if (Added) {
                BlacklistHandle.set(Live, Blacklist);
            }
            if (DualHandedHandle != null) {
                keepDualHanded(Live);
            }
            SeenConfig = Live;
            SeenBlacklist = BlacklistHandle.get(Live);
        } catch (Throwable Problem) {
            Active = false;
            Log.warn("Could not update the Punchy item blacklist, leaving it alone from now on", Problem);
        }
    }

    private static void keepDualHanded(Object Live) throws IllegalAccessException {
        Object Mapped = DualHandedHandle.get(Live);
        Map<String, Boolean> DualHanded = new LinkedHashMap<>();
        if (Mapped instanceof Map<?, ?> Old) {
            for (Map.Entry<?, ?> Pair : Old.entrySet()) {
                if (Pair.getKey() instanceof String Name && Pair.getValue() instanceof Boolean Flag) {
                    DualHanded.put(Name, Flag);
                }
            }
        }
        boolean Changed = false;
        for (Map.Entry<String, Boolean> Wish : wanted().entrySet()) {
            Boolean Current = DualHanded.get(Wish.getKey());
            if (Wish.getValue() && !Boolean.TRUE.equals(Current)) {
                DualHanded.put(Wish.getKey(), true);
                Changed = true;
            } else if (Current == null) {
                DualHanded.put(Wish.getKey(), false);
                Changed = true;
            }
        }
        if (Changed) {
            DualHandedHandle.set(Live, DualHanded);
        }
    }

    private static Field optionalField(Class<?> Owner, String Name) {
        try {
            return Owner.getField(Name);
        } catch (NoSuchFieldException Missing) {
            return null;
        }
    }
}

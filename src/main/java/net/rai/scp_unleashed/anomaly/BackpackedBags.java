package net.rai.scp_unleashed.anomaly;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.List;

public final class BackpackedBags {
    public static final String AccessClass = "com.mrcrayfish.backpacked.inventory.BackpackedInventoryAccess";
    public static final String CountMethod = "backpacked$GetBackpackInventoryCount";
    public static final String BagMethod = "backpacked$GetBackpackInventory";
    public static final String SaveMethod = "saveItemsToStack";

    private static final Logger Log = LogUtils.getLogger();
    private static boolean Resolved;
    private static boolean Warned;
    private static Class<?> AccessType;
    private static Method Count;
    private static Method Bag;

    private BackpackedBags() {
    }

    public static void unpack(ServerPlayer User, List<ItemStack> Found) {
        if (!ModList.get().isLoaded(AnomalyDropConfig.BackpackedId) || !AnomalyDropConfig.equippedBackpacked()) {
            return;
        }
        resolve();
        if (AccessType == null || !AccessType.isInstance(User)) {
            return;
        }
        try {
            int Total = (Integer) Count.invoke(User);
            for (int Index = 0; Index < Total; Index++) {
                if (Bag.invoke(User, Index) instanceof Container Worn && PackedAnomalies.unpack(Worn, Found)) {
                    save(Worn);
                }
            }
        } catch (Throwable Problem) {
            warn("Backpacked backpacks could not be searched for anomalies", Problem);
        }
    }

    private static void save(Container Worn) {
        try {
            Worn.getClass().getMethod(SaveMethod).invoke(Worn);
        } catch (NoSuchMethodException Missing) {
            Worn.setChanged();
        } catch (Throwable Problem) {
            warn("A Backpacked backpack could not be saved after anomalies were taken out", Problem);
        }
    }

    private static void warn(String Message, Throwable Problem) {
        if (!Warned) {
            Warned = true;
            Log.warn(Message, Problem);
        }
    }

    private static void resolve() {
        if (Resolved) {
            return;
        }
        Resolved = true;
        try {
            Class<?> Access = Class.forName(AccessClass, false, BackpackedBags.class.getClassLoader());
            Method Counter = Access.getMethod(CountMethod);
            Method Getter = Access.getMethod(BagMethod, int.class);
            Count = Counter;
            Bag = Getter;
            AccessType = Access;
        } catch (ClassNotFoundException Missing) {
            AccessType = null;
        } catch (Throwable Problem) {
            AccessType = null;
            Log.warn("Backpacked is present but its backpack slots are unusable, equipped backpacks will keep their anomalies", Problem);
        }
    }
}

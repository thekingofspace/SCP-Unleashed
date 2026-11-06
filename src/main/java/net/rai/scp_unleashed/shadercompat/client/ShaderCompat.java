package net.rai.scp_unleashed.shadercompat.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.rai.scp_unleashed.config.ScpClientConfig;
import org.slf4j.Logger;

public final class ShaderCompat {
    public static final String ApiClass = "net.irisshaders.iris.api.v0.IrisApi";
    public static final String FallbackProperty = "scp_unleashed.shaderFallback";

    private static final Logger Log = LogUtils.getLogger();
    private static final boolean Forced = Boolean.getBoolean(FallbackProperty);
    private static boolean Resolved;
    private static boolean Warned;
    private static Object Api;
    private static Method PackInUse;
    private static Method ShadowPass;

    private ShaderCompat() {
    }

    public static boolean forced() {
        return Forced;
    }

    public static boolean useFallback() {
        return Forced || shaderPackInUse();
    }

    public static boolean skipPostEffects() {
        return useFallback() || !ScpClientConfig.postEffects();
    }

    public static boolean shaderPackInUse() {
        resolve();
        return ask(PackInUse);
    }

    public static boolean renderingShadowPass() {
        resolve();
        return ask(ShadowPass);
    }

    private static boolean ask(Method Query) {
        if (Api == null || Query == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(Query.invoke(Api));
        } catch (Throwable Problem) {
            if (!Warned) {
                Warned = true;
                Log.warn("Iris API call {} failed, treating it as false", Query.getName(), Problem);
            }
            return false;
        }
    }

    private static void resolve() {
        if (Resolved) {
            return;
        }
        Resolved = true;
        try {
            Class<?> ApiType = Class.forName(ApiClass, false, ShaderCompat.class.getClassLoader());
            Method InUse = ApiType.getMethod("isShaderPackInUse");
            Method Shadow = ApiType.getMethod("isRenderingShadowPass");
            Object Instance = ApiType.getMethod("getInstance").invoke(null);
            if (Instance != null) {
                PackInUse = InUse;
                ShadowPass = Shadow;
                Api = Instance;
                Log.info("Iris API found: rifts switch to vanilla render types while a shader pack is active");
            }
        } catch (ClassNotFoundException Missing) {
            Api = null;
        } catch (Throwable Problem) {
            Api = null;
            Log.warn("Iris API is present but unusable, shader pack detection is off", Problem);
        }
        if (Forced) {
            Log.info("Rift shader fallback forced on by -D{}=true", FallbackProperty);
        }
    }
}

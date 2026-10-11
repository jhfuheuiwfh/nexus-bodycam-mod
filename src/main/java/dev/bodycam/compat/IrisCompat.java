package dev.bodycam.compat;

import dev.bodycam.config.BodycamConfig;

public class IrisCompat {
    private static Boolean irisAvailable = null;
    private static Object irisApi = null;
    private static java.lang.reflect.Method isShaderPackInUse = null;
    private static java.lang.reflect.Method getShaderPack = null;
    private static java.lang.reflect.Method setShaderOption = null;

    public static boolean isShaderActive() {
        if (!BodycamConfig.loadCached().syncWithShader) return false;
        if (irisAvailable == null) initIris();
        if (irisApi == null || isShaderPackInUse == null) return false;
        try {
            return (boolean) isShaderPackInUse.invoke(irisApi);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBodycamShaderActive() {
        if (!isShaderActive()) return false;
        try {
            Object shaderPack = getShaderPack.invoke(irisApi);
            if (shaderPack == null) return false;
            String name = shaderPack.getClass().getMethod("getName").invoke(shaderPack).toString().toLowerCase();
            return name.contains("bodycam") || name.contains("body_cam") || name.contains("gopro");
        } catch (Exception e) {
            return false;
        }
    }

    public static void applyShaderOverrides(BodycamConfig config) {
        if (!isBodycamShaderActive()) return;
        if (setShaderOption == null) initShaderOptions();
        if (setShaderOption == null) return;
        try {
            setShaderOption.invoke(irisApi, "bodycam_motionBlur", (float) config.motionBlurBoost);
            setShaderOption.invoke(irisApi, "bodycam_sway", (float) config.swayIntensity);
        } catch (Exception ignored) {}
    }

    private static void initIris() {
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.api.IrisApi");
            irisApi = irisClass.getMethod("getInstance").invoke(null);
            isShaderPackInUse = irisClass.getMethod("isShaderPackInUse");
            getShaderPack = irisClass.getMethod("getShaderPack");
            irisAvailable = true;
        } catch (Exception e) {
            irisAvailable = false;
        }
    }

    private static void initShaderOptions() {
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.api.IrisApi");
            setShaderOption = irisClass.getMethod("setShaderOption", String.class, float.class);
        } catch (Exception ignored) {}
    }

    public static boolean isRenderingShadowPass() {
        if (irisAvailable == null) initIris();
        if (irisApi == null) return false;
        try {
            Class<?> irisClass = Class.forName("net.irisshaders.api.IrisApi");
            java.lang.reflect.Method m = irisClass.getMethod("isRenderingShadowPass");
            return (boolean) m.invoke(irisApi);
        } catch (Exception e) {
            return false;
        }
    }
}
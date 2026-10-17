package dev.bodycam.camera;

import dev.bodycam.BodycamClient;
import dev.bodycam.config.BodycamConfig;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * Phase D — the TACZ bridge. TACZ is an OPTIONAL integration: the mod must
 * compile, load and run on a machine that has never seen a TACZ jar.
 *
 * So this class holds NO compile-time dependency on com.tacz/cn.sh1rocu.
 * Every TACZ symbol is resolved reflectively in {@link #init()}, which is only
 * reached after BodycamClient has confirmed FabricLoader.isModLoaded("tacz").
 * If any lookup fails the bridge simply stays off and the camera behaves as if
 * TACZ were not installed.
 *
 * Two details make this cheap and safe:
 *   - TACZ's event bus IS the Fabric event bus (net.fabricmc.fabric.api.event.Event),
 *     which is already a compile dependency, so only the listener type is proxied.
 *   - Minecraft types are never named: the live ClientPlayerEntity is handed to
 *     reflective calls, so the code is mapping-agnostic.
 *
 * Separation of masses (roadmap rule): real shots drive the WEAPON recoil
 * springs here; the camera itself is never kicked by gunfire.
 */
public final class TaczBridge {
    private static boolean inited = false;
    private static float aimAmount = 0.0f;
    private static double wallPull = 0.0;

    /* Resolved TACZ members — null until init() succeeds. */
    private static Object logicalSideClient;   // LogicalSide.CLIENT
    private static Method getLogicalSide;      // event -> LogicalSide
    private static Method getShooter;          // GunShootEvent -> the shooter
    private static Method getEntity;           // GunReloadEvent -> the shooter
    private static Method fromLocalPlayer;     // IClientPlayerGunOperator.fromLocalPlayer(player)
    private static Method aimingProgress;      // operator.getClientAimingProgress(float) -> float

    private TaczBridge() {}

    /** Called once at client init, only when TACZ is present. */
    public static void init() {
        if (inited) return;
        try {
            Class<?> shootEvent  = Class.forName("com.tacz.guns.api.event.common.GunShootEvent");
            Class<?> reloadEvent = Class.forName("com.tacz.guns.api.event.common.GunReloadEvent");
            Class<?> logicalSide = Class.forName("cn.sh1rocu.tacz.api.LogicalSide");
            Class<?> gunOperator = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");

            logicalSideClient = logicalSide.getField("CLIENT").get(null);
            getLogicalSide    = noArg(shootEvent, "getLogicalSide");
            getShooter        = noArg(shootEvent, "getShooter");
            getEntity         = noArg(reloadEvent, "getEntity");
            aimingProgress    = oneFloat(gunOperator, "getClientAimingProgress");
            fromLocalPlayer   = staticWith(gunOperator, "fromLocalPlayer", 1);

            onCallback(shootEvent, TaczBridge::onShoot);
            onCallback(reloadEvent, TaczBridge::onReload);

            inited = true;
            BodycamClient.LOGGER.info("[bodycam] TACZ API ligada via reflection: GunShootEvent + GunReloadEvent");
        } catch (Throwable t) {
            // Any missing member (older/newer TACZ, renamed API) -> bridge stays off.
            inited = false;
            BodycamClient.LOGGER.info("[bodycam] TACZ indisponivel ou API incompativel, bridge desligado: {}", t.toString());
        }
    }

    /** Subscribes a proxy implementing TACZ's `XxxEvent$Callback#post(event)`. */
    private static void onCallback(Class<?> eventClass, java.util.function.Consumer<Object> handler) throws Exception {
        Field field = eventClass.getField("CALLBACK");
        Object bus = field.get(null);
        if (!(bus instanceof Event)) {
            throw new IllegalStateException(eventClass.getName() + ".CALLBACK nao e uma Fabric Event");
        }
        Class<?> listener = Class.forName(eventClass.getName() + "$Callback");
        Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class<?>[] { listener },
                (p, method, args) -> {
                    if ("post".equals(method.getName()) && args != null && args.length == 1) {
                        handler.accept(args[0]);
                    }
                    return null;
                });
        // Fabric's Event#register — resolved by name so a signature change is a
        // caught Throwable here rather than a compile error.
        Method register = null;
        for (Method m : bus.getClass().getMethods()) {
            if ("register".equals(m.getName()) && m.getParameterCount() == 1) { register = m; break; }
        }
        if (register == null) throw new NoSuchMethodException("Event#register");
        register.invoke(bus, proxy);
    }

    private static void onShoot(Object event) {
        try {
            if (getLogicalSide.invoke(event) != logicalSideClient) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || getShooter.invoke(event) != mc.player) return;
            TaczGunRealism.triggerRecoil(0.5, 0.08);
        } catch (Throwable ignored) {}
    }

    private static void onReload(Object event) {
        try {
            if (getLogicalSide.invoke(event) != logicalSideClient) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || getEntity.invoke(event) != mc.player) return;
            TaczGunRealism.triggerReload();
        } catch (Throwable ignored) {}
    }

    /** Sampled once per render frame (main camera only — see TaczSway.update). */
    public static void tickFrame() {
        if (!inited) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            aimAmount = 0.0f;
            return;
        }
        try {
            Object operator = fromLocalPlayer.invoke(null, player);
            Object progress = aimingProgress.invoke(operator, 1.0f);
            aimAmount = MathHelper.clamp(progress == null ? 0.0f : (Float) progress, 0.0f, 1.0f);
        } catch (Throwable t) {
            aimAmount = 0.0f;
        }
    }

    /** 0..1 ADS progress (0 when TACZ absent or not aiming). */
    public static float getAimAmount() {
        return inited ? aimAmount : 0.0f;
    }

    /** 0..1 — how close something solid is in front of the muzzle (0 = clear). */
    public static double getWallPull() {
        return wallPull;
    }

    /** Wall pullback probe — called once per frame from TaczGunRealism. */
    public static void updateWallPull(ClientPlayerEntity player, double dt) {
        if (!inited) {
            wallPull = 0.0;
            return;
        }
        BodycamConfig cfg = BodycamEffects.getConfig();
        double target = 0.0;
        if (cfg.taczWallPullbackEnabled && cfg.enabled) {
            try {
                HitResult hit = player.raycast(1.0, 1.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    double dist = hit.getPos().distanceTo(player.getEyePos());
                    target = MathHelper.clamp(1.0 - dist, 0.0, 1.0);
                }
            } catch (Throwable ignored) {}
        }
        double lambda = target > wallPull ? 14.0 : 7.0;
        wallPull = TemporalSmoother.expSmooth(wallPull, target, lambda, dt);
    }

    /* ── reflective lookups ────────────────────────────────────────────
       Matched by NAME + arity rather than by descriptor, so a remapped
       Minecraft parameter type can never break the lookup. */

    private static Method noArg(Class<?> type, String name) throws NoSuchMethodException {
        for (Method m : type.getMethods()) {
            if (name.equals(m.getName()) && m.getParameterCount() == 0) return m;
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private static Method oneFloat(Class<?> type, String name) throws NoSuchMethodException {
        for (Method m : type.getMethods()) {
            if (name.equals(m.getName()) && m.getParameterCount() == 1 && m.getParameterTypes()[0] == float.class) {
                return m;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name + "(float)");
    }

    private static Method staticWith(Class<?> type, String name, int params) throws NoSuchMethodException {
        for (Method m : type.getMethods()) {
            if (name.equals(m.getName()) && m.getParameterCount() == params && Modifier.isStatic(m.getModifiers())) {
                return m;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + " (static, " + params + " params)");
    }
}

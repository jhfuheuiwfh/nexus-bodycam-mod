package dev.bodycam.hud;

import dev.bodycam.BodycamClient;
import dev.bodycam.config.BodycamConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * v1.5 — in-game REC device overlay (bodycam HUD).
 *
 * Simulated device UI drawn over the world, vanilla-HUD style:
 *   top-left     blinking ● REC + session elapsed timer
 *   top-right    battery icon + % (drains slowly, resets on world join)
 *   bottom-centre clock + date
 *   bottom corners device label + real resolution/FPS
 *
 * Hidden while any screen is open, F1 (hudHidden) or when the mod /
 * recOverlayEnabled is off. Every block is independently toggleable from
 * the config screen (HUD & Overlay category).
 */
public final class RecOverlay {
    private static final int MARGIN = 6;
    private static final int COL_RED = 0xFFFF4444;
    private static final int COL_WHITE = 0xFFFFFFFF;
    private static final int COL_ELAPSED = 0xFFBBBBBB;
    private static final int COL_DATE = 0xFFAAAAAA;
    private static final int COL_INFO = 0xFF777777;
    private static final int COL_BATT_HI = 0xFF55FF55;
    private static final int COL_BATT_MID = 0xFFFFAA00;
    private static final int COL_BATT_LO = 0xFFFF5555;

    private static final long BLINK_HALF_MS = 500L;
    private static final long DRAIN_MS = 180_000L; // ~1 % per 3 real minutes
    private static final int BATTERY_MIN = 8;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static boolean registered = false;
    private static boolean inWorld = false;
    private static long joinMillis = 0L;
    private static long lastDrain = 0L;
    private static int battery = 100;

    private RecOverlay() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudRenderCallback.EVENT.register(RecOverlay::render);
        BodycamClient.LOGGER.info("[bodycam] REC overlay HUD registado.");
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            inWorld = false;
            return;
        }
        if (mc.currentScreen != null || mc.options.hudHidden) return;

        BodycamConfig cfg = BodycamConfig.loadCached();
        if (!cfg.enabled || !cfg.recOverlayEnabled) return;

        long now = System.currentTimeMillis();
        if (!inWorld) {
            inWorld = true;
            joinMillis = now;
            lastDrain = now;
            battery = 100;
        }
        if (now - lastDrain >= DRAIN_MS && battery > BATTERY_MIN) {
            battery--;
            lastDrain = now;
        }

        MatrixStack ms = context.getMatrices();
        ms.push();

        // ---- top-left: blinking ● REC + elapsed session time ----
        if ((now / BLINK_HALF_MS) % 2L == 0L) {
            context.fill(MARGIN, MARGIN + 1, MARGIN + 3, MARGIN + 4, COL_RED);
        }
        context.drawTextWithShadow(mc.textRenderer, "REC", MARGIN + 6, MARGIN, COL_WHITE);
        long secs = Math.max(0L, (now - joinMillis) / 1000L);
        String elapsed = String.format("%02d:%02d:%02d", secs / 3600L, (secs / 60L) % 60L, secs % 60L);
        context.drawTextWithShadow(mc.textRenderer, elapsed, MARGIN, MARGIN + 11, COL_ELAPSED);

        // ---- top-right: battery icon + % ----
        if (cfg.recOverlayBattery) {
            int col = battery > 50 ? COL_BATT_HI : (battery > 20 ? COL_BATT_MID : COL_BATT_LO);
            int bx = context.getScaledWindowWidth() - MARGIN - 27;
            int by = MARGIN;
            context.fill(bx + 10, by + 1, bx + 12, by + 5, 0xFF888888); // cap
            context.fill(bx, by, bx + 10, by + 6, 0xFF111111);          // body
            int fw = Math.round(8f * battery / 100f);
            if (fw > 0) context.fill(bx + 1, by + 1, bx + 1 + fw, by + 5, col);
            context.drawTextWithShadow(mc.textRenderer, battery + "%", bx + 15, by, COL_ELAPSED);
        }

        // ---- bottom-centre: clock + date ----
        if (cfg.recOverlayTimestamp) {
            String time = LocalTime.now().format(TIME_FMT);
            String date = LocalDate.now().format(DATE_FMT);
            int w = context.getScaledWindowWidth();
            context.drawCenteredTextWithShadow(mc.textRenderer, time, w / 2, context.getScaledWindowHeight() - 24, COL_WHITE);
            context.drawCenteredTextWithShadow(mc.textRenderer, date, w / 2, context.getScaledWindowHeight() - 14, COL_DATE);
        }

        // ---- bottom corners: device label + real resolution/FPS ----
        if (cfg.recOverlayInfo) {
            int w = context.getScaledWindowWidth();
            int h = context.getScaledWindowHeight();
            context.drawTextWithShadow(mc.textRenderer, "NEXUS-7 BODYCAM", MARGIN, h - 12, COL_INFO);
            String res = mc.getWindow().getFramebufferWidth() + "x" + mc.getWindow().getFramebufferHeight()
                    + " " + mc.getCurrentFps() + "FPS";
            int rx = w - MARGIN - mc.textRenderer.getWidth(res);
            context.drawTextWithShadow(mc.textRenderer, res, rx, h - 12, COL_INFO);
        }

        ms.pop();
    }
}

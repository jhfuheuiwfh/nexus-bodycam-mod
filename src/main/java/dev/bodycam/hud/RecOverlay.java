package dev.bodycam.hud;

import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

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
 * Hidden while any screen is open, F1 (hideGui) or when the mod /
 * recOverlayEnabled is off. Every block is independently toggleable from
 * the config screen (HUD & Overlay category).
 *
 * On Forge the registration happens in BodycamForge via
 * RegisterGuiOverlaysEvent (registerAboveAll).
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

    private static boolean inWorld = false;
    private static long joinMillis = 0L;
    private static long lastDrain = 0L;
    private static int battery = 100;

    private RecOverlay() {}

    public static void renderOverlay(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            inWorld = false;
            return;
        }
        if (mc.screen != null || mc.options.hideGui) return;

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

        // ---- top-left: blinking ● REC + elapsed session time ----
        if ((now / BLINK_HALF_MS) % 2L == 0L) {
            graphics.fill(MARGIN, MARGIN + 1, MARGIN + 3, MARGIN + 4, COL_RED);
        }
        graphics.drawString(mc.font, "REC", MARGIN + 6, MARGIN, COL_WHITE);
        long secs = Math.max(0L, (now - joinMillis) / 1000L);
        String elapsed = String.format("%02d:%02d:%02d", secs / 3600L, (secs / 60L) % 60L, secs % 60L);
        graphics.drawString(mc.font, elapsed, MARGIN, MARGIN + 11, COL_ELAPSED);

        // ---- top-right: battery icon + % ----
        if (cfg.recOverlayBattery) {
            int col = battery > 50 ? COL_BATT_HI : (battery > 20 ? COL_BATT_MID : COL_BATT_LO);
            int bx = mc.getWindow().getGuiScaledWidth() - MARGIN - 27;
            int by = MARGIN;
            graphics.fill(bx + 10, by + 1, bx + 12, by + 5, 0xFF888888); // cap
            graphics.fill(bx, by, bx + 10, by + 6, 0xFF111111);          // body
            int fw = Math.round(8f * battery / 100f);
            if (fw > 0) graphics.fill(bx + 1, by + 1, bx + 1 + fw, by + 5, col);
            graphics.drawString(mc.font, battery + "%", bx + 15, by, COL_ELAPSED);
        }

        // ---- bottom-centre: clock + date ----
        if (cfg.recOverlayTimestamp) {
            String time = LocalTime.now().format(TIME_FMT);
            String date = LocalDate.now().format(DATE_FMT);
            int w = mc.getWindow().getGuiScaledWidth();
            graphics.drawCenteredString(mc.font, time, w / 2, mc.getWindow().getGuiScaledHeight() - 24, COL_WHITE);
            graphics.drawCenteredString(mc.font, date, w / 2, mc.getWindow().getGuiScaledHeight() - 14, COL_DATE);
        }

        // ---- bottom corners: device label + real resolution/FPS ----
        if (cfg.recOverlayInfo) {
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            graphics.drawString(mc.font, "NEXUS-7 BODYCAM", MARGIN, h - 12, COL_INFO);
            String res = mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight()
                    + " " + mc.getFps() + "FPS";
            int rx = w - MARGIN - mc.font.width(res);
            graphics.drawString(mc.font, res, rx, h - 12, COL_INFO);
        }
    }
}

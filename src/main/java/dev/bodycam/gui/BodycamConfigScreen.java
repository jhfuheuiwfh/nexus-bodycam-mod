package dev.bodycam.gui;

import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class BodycamConfigScreen extends Screen {
    private static final int TOP_BAR = 44;
    private static final int BOTTOM_BAR = 38;
    private static final int ROW_H = 22;
    private static final int HDR_H = 16;
    private static final int GAP = 6;
    private static final int I_HDR = 0;
    private static final int I_SL = 1;
    private static final int I_TG = 2;
    private static final int I_WARN = 3;

    private int colW = 150;
    private int panelX = 4;
    private int panelW = 300;
    private int lx, rx, fullW;
    private int contentTop, contentBot;
    private double scroll = 0;
    private double contentH = 0;

    private final List<Item> items = new ArrayList<>();
    private int curY = 0;
    private int slot = 0;

    private final Screen parent;
    private BodycamConfig config;
    private final List<String> presets = Arrays.asList("SUBTLE", "NORMAL", "INTENSE", "EXTREME",
            "DOCUMENTARY", "HORROR", "TACTICAL", "RAW GOPRO");

    private static final class Item {
        final int type;
        final String key;
        final double min, max;
        int y;
        int col;

        Item(int type, String key, double min, double max) {
            this.type = type;
            this.key = key;
            this.min = min;
            this.max = max;
        }
    }

    public BodycamConfigScreen(Screen parent) {
        super(Text.translatable("bodycam.config.title"));
        this.parent = parent;
        this.config = BodycamConfig.load();
    }

    // ---------- layout builder ----------

    private void hdr(String cat) {
        flushRow();
        Item i = new Item(I_HDR, cat, 0, 0);
        i.y = curY;
        items.add(i);
        curY += HDR_H;
    }

    private void sl(String key, double min, double max) {
        int c = slot == 0 ? 0 : 1;
        Item i = new Item(I_SL, key, min, max);
        i.y = curY;
        i.col = c;
        items.add(i);
        endCol(c);
    }

    private void tg(String key) {
        int c = slot == 0 ? 0 : 1;
        Item i = new Item(I_TG, key, 0, 0);
        i.y = curY;
        i.col = c;
        items.add(i);
        endCol(c);
    }

    private void full(String key) {
        flushRow();
        Item i = new Item(I_TG, key, 0, 0);
        i.y = curY;
        i.col = 2;
        items.add(i);
        curY += ROW_H;
    }

    private void endCol(int c) {
        if (c == 1) {
            slot = 0;
            curY += ROW_H;
        } else {
            slot = 1;
        }
    }

    private void flushRow() {
        if (slot == 1) {
            slot = 0;
            curY += ROW_H;
        }
    }

    private void buildLayout() {
        items.clear();
        curY = 0;
        slot = 0;

        hdr("smoothing");
        sl("stepSmoothing", 0.0, 1.5);   sl("landingDamping", 0.0, 2.0);
        sl("idleShake", 0.0, 1.5);       sl("idleShakeFrequency", 0.1, 5.0);
        sl("bobbing", 0.0, 2.0);         sl("bobbingFrequency", 0.1, 3.0);
        sl("rollLean", 0.0, 2.0);        sl("sprintMultiplier", 0.5, 3.0);
        sl("sneakMultiplier", 0.1, 1.0); tg("taczSwayEnabled");
        tg("inertiaEnabled");            sl("inertiaDelay", 0.02, 0.15);
        sl("inertiaOvershoot", 0.0, 1.0); tg("gaitTransitionEnabled");
        tg("impactRecoveryEnabled");     sl("impactStrength", 0.0, 2.0);
        tg("surfaceFootstepsEnabled");   sl("surfaceFootstepGain", 0.25, 2.0);

        hdr("weapon");
        sl("taczSwayIntensity", 0.0, 2.0); sl("taczSwaySpeed", 0.3, 4.0);
        sl("taczSwayOvershoot", 0.0, 0.6); sl("viewSmoothSpeed", 0.0, 40.0);
        tg("taczLoweringEnabled");         sl("taczLoweringSpeed", 0.5, 5.0);
        tg("taczRunSwayEnabled");          sl("taczRunSwayIntensity", 0.0, 2.0);
        tg("taczRecoilKickEnabled");       sl("taczRecoilKickStrength", 0.0, 3.0);
        tg("taczAdsStabilization");        tg("taczMovePenaltyEnabled");
        tg("taczLandingLagEnabled");       tg("taczWallPullbackEnabled");
        tg("taczReloadNodEnabled");

        hdr("camera");
        tg("chestCamEnabled"); sl("fovWideMult", 1.0, 1.5);
        sl("chestCamHeight", -1.0, 0.0); sl("chestCamPitch", 0.0, 30.0);

        hdr("postfx");
        full("postFXEnabled");
        sl("fisheyeK1", 0.0, 0.5);             sl("fisheyeK2", -0.3, 0.0);
        sl("chromaticAberration", 0.0, 0.01);  sl("lensDirtIntensity", 0.0, 5.0);
        sl("grainStrength", 0.0, 0.15);        sl("grainCellSize", 1.0, 5.0);
        tg("autoExposureEnabled");             sl("autoExposureStrength", 0.0, 1.0);
        tg("flashShockEnabled");               sl("flashShockStrength", 0.0, 1.5);
        tg("lowLightNoiseEnabled");            sl("lowLightNoiseGain", 0.0, 1.5);
        tg("lensContaminationEnabled");        sl("lensCondStrength", 0.0, 1.5);
        sl("lensCleanSpeed", 0.0, 3.0);        tg("ghostFlareEnabled");
        sl("ghostFlareStrength", 0.0, 2.0);    tg("lensProfileEnabled");
        sl("lensProfileStrength", 0.0, 1.5);   tg("dustEnabled");
        sl("dustStrength", 0.0, 1.5);          tg("criticalCamEnabled");
        sl("criticalStrength", 0.0, 2.0);      tg("motionBlurEnabled");
        sl("motionBlurStrength", 0.0, 1.5);        sl("vignetteBlur", 0.0, 1.0);

        hdr("water");
        full("waterEnabled");
        sl("waveHeight", 0.0, 0.5);            sl("waveSpeed", 0.1, 5.0);
        sl("waterOpacity", 0.0, 1.0);          sl("causticsStrength", 0.0, 2.0);
        sl("underwaterFogDensity", 0.0, 0.3);  sl("foamIntensity", 0.0, 2.0);

        hdr("compat");
        full("syncWithShader");
        full("disableVanillaBobbing");

        flushRow();
        Item w = new Item(I_WARN, "warn", 0, 0);
        w.y = curY;
        items.add(w);
        curY += ROW_H;

        contentH = curY;
    }

    // ---------- init / widgets ----------

    @Override
    protected void init() {
        panelX = 4;
        panelW = this.width - 10;
        int inner = panelW - 18;
        colW = Math.min(175, Math.max(100, (inner - GAP) / 2));
        int colsW = colW * 2 + GAP;
        lx = panelX + 6 + Math.max(0, (inner - colsW) / 2);
        rx = lx + colW + GAP;
        fullW = colsW;
        contentTop = TOP_BAR;
        contentBot = Math.max(contentTop + ROW_H * 2, this.height - BOTTOM_BAR);

        int maxScroll = Math.max(0, (int) contentH - (contentBot - contentTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        buildLayout();

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("bodycam.config.save"), b -> {
            config.save();
            close();
        }).dimensions(panelX + panelW - 116, 21, 110, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("bodycam.config.reset"), b -> {
            config = new BodycamConfig();
            config.applyPreset("NORMAL");
            clearAndReinit();
        }).dimensions(panelX + 6, 21, 110, 18).build());

        for (Item i : items) {
            if (i.type != I_SL && i.type != I_TG) continue;
            int sy = contentTop + i.y - (int) scroll;
            if (sy < contentTop || sy + ROW_H > contentBot) continue;
            int x = i.col == 0 ? lx : (i.col == 1 ? rx : lx);
            int w = i.col == 2 ? fullW : colW;
            if (i.type == I_SL) {
                addSliderAt(i.key, i.min, i.max, x, sy, w);
            } else {
                addToggleAt(i.key, x, sy, w);
            }
        }

        this.addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.literal("Preset: " + v))
            .values(presets)
            .initially(config.preset.toUpperCase())
            .build(lx, this.height - BOTTOM_BAR + 7, fullW, 26, Text.literal("Preset: " + config.preset.toUpperCase()),
                (btn, val) -> {
                    config.applyPreset(val);
                    clearAndReinit();
                }));
    }

    private void addToggleAt(String key, int x, int y, int w) {
        this.addDrawableChild(ButtonWidget.builder(toggleLabel(key, getToggle(key)), b -> {
            setToggle(key, !getToggle(key));
            b.setMessage(toggleLabel(key, getToggle(key)));
        }).dimensions(x, y, w, ROW_H).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxScroll = Math.max(0, (int) contentH - (contentBot - contentTop));
        double next = scroll - amount * 16;
        scroll = Math.max(0, Math.min(maxScroll, next));
        clearAndInit();
        return true;
    }

    private void renderDofBlur(net.minecraft.client.gui.DrawContext context) {
        boolean blurred = dev.bodycam.camera.PostEffects.renderBlur(6.0f);
        context.fill(0, 0, this.width, this.height, blurred ? 0x50000000 : 0x90000000);
    }

    private void addSliderAt(String key, double min, double max, int x, int y, int w) {
        double cur = getVal(key);
        double norm = (cur - min) / (max - min);
        this.addDrawableChild(new BodycamSlider(x, y, w, ROW_H,
            Text.translatable("bodycam.config." + key), norm, min, max,
            v -> setVal(key, v)));
    }

    private double getVal(String key) {
        return switch (key) {
            case "stepSmoothing" -> config.stepSmoothing;
            case "landingDamping" -> config.landingDamping;
            case "idleShake" -> config.idleShake;
            case "idleShakeFrequency" -> config.idleShakeFrequency;
            case "bobbing" -> config.bobbing;
            case "bobbingFrequency" -> config.bobbingFrequency;
            case "rollLean" -> config.rollLean;
            case "sprintMultiplier" -> config.sprintMultiplier;
            case "sneakMultiplier" -> config.sneakMultiplier;
            case "taczSwayIntensity" -> config.taczSwayIntensity;
            case "taczSwaySpeed" -> config.taczSwaySpeed;
            case "taczSwayOvershoot" -> config.taczSwayOvershoot;
            case "viewSmoothSpeed" -> config.viewSmoothSpeed;
            case "inertiaDelay" -> config.inertiaDelay;
            case "inertiaOvershoot" -> config.inertiaOvershoot;
            case "impactStrength" -> config.impactStrength;
            case "surfaceFootstepGain" -> config.surfaceFootstepGain;
            case "lowLightNoiseGain" -> config.lowLightNoiseGain;
            case "lensCondStrength" -> config.lensCondStrength;
            case "lensCleanSpeed" -> config.lensCleanSpeed;
            case "ghostFlareStrength" -> config.ghostFlareStrength;
            case "lensProfileStrength" -> config.lensProfileStrength;
            case "dustStrength" -> config.dustStrength;
            case "criticalStrength" -> config.criticalStrength;
            case "motionBlurStrength" -> config.motionBlurStrength;
            case "vignetteBlur" -> config.vignetteBlur;
            case "chestCamHeight" -> config.chestCamHeight;
            case "chestCamPitch" -> config.chestCamPitch;
            case "fovWideMult" -> config.fovWideMult;
            case "taczLoweringSpeed" -> config.taczLoweringSpeed;
            case "taczRunSwayIntensity" -> config.taczRunSwayIntensity;
            case "taczRecoilKickStrength" -> config.taczRecoilKickStrength;
            case "fisheyeK1" -> config.fisheyeK1;
            case "fisheyeK2" -> config.fisheyeK2;
            case "chromaticAberration" -> config.chromaticAberration;
            case "lensDirtIntensity" -> config.lensDirtIntensity;
            case "grainStrength" -> config.grainStrength;
            case "grainCellSize" -> config.grainCellSize;
            case "autoExposureStrength" -> config.autoExposureStrength;
            case "flashShockStrength" -> config.flashShockStrength;
            case "waveHeight" -> config.waveHeight;
            case "waveSpeed" -> config.waveSpeed;
            case "waterOpacity" -> config.waterOpacity;
            case "causticsStrength" -> config.causticsStrength;
            case "underwaterFogDensity" -> config.underwaterFogDensity;
            case "foamIntensity" -> config.foamIntensity;
            default -> 0;
        };
    }

    private void setVal(String key, double v) {
        switch (key) {
            case "stepSmoothing" -> config.stepSmoothing = v;
            case "landingDamping" -> config.landingDamping = v;
            case "idleShake" -> config.idleShake = v;
            case "idleShakeFrequency" -> config.idleShakeFrequency = v;
            case "bobbing" -> config.bobbing = v;
            case "bobbingFrequency" -> config.bobbingFrequency = v;
            case "rollLean" -> config.rollLean = v;
            case "sprintMultiplier" -> config.sprintMultiplier = v;
            case "sneakMultiplier" -> config.sneakMultiplier = v;
            case "taczSwayIntensity" -> config.taczSwayIntensity = v;
            case "taczSwaySpeed" -> config.taczSwaySpeed = v;
            case "taczSwayOvershoot" -> config.taczSwayOvershoot = v;
            case "viewSmoothSpeed" -> config.viewSmoothSpeed = v;
            case "inertiaDelay" -> config.inertiaDelay = v;
            case "inertiaOvershoot" -> config.inertiaOvershoot = v;
            case "impactStrength" -> config.impactStrength = v;
            case "surfaceFootstepGain" -> config.surfaceFootstepGain = v;
            case "lowLightNoiseGain" -> config.lowLightNoiseGain = v;
            case "lensCondStrength" -> config.lensCondStrength = v;
            case "lensCleanSpeed" -> config.lensCleanSpeed = v;
            case "ghostFlareStrength" -> config.ghostFlareStrength = v;
            case "lensProfileStrength" -> config.lensProfileStrength = v;
            case "dustStrength" -> config.dustStrength = v;
            case "criticalStrength" -> config.criticalStrength = v;
            case "motionBlurStrength" -> config.motionBlurStrength = v;
            case "vignetteBlur" -> config.vignetteBlur = v;
            case "chestCamHeight" -> config.chestCamHeight = v;
            case "chestCamPitch" -> config.chestCamPitch = v;
            case "fovWideMult" -> config.fovWideMult = v;
            case "taczLoweringSpeed" -> config.taczLoweringSpeed = v;
            case "taczRunSwayIntensity" -> config.taczRunSwayIntensity = v;
            case "taczRecoilKickStrength" -> config.taczRecoilKickStrength = v;
            case "fisheyeK1" -> config.fisheyeK1 = v;
            case "fisheyeK2" -> config.fisheyeK2 = v;
            case "chromaticAberration" -> config.chromaticAberration = v;
            case "lensDirtIntensity" -> config.lensDirtIntensity = v;
            case "grainStrength" -> config.grainStrength = v;
            case "grainCellSize" -> config.grainCellSize = v;
            case "autoExposureStrength" -> config.autoExposureStrength = v;
            case "flashShockStrength" -> config.flashShockStrength = v;
            case "waveHeight" -> config.waveHeight = v;
            case "waveSpeed" -> config.waveSpeed = v;
            case "waterOpacity" -> config.waterOpacity = v;
            case "causticsStrength" -> config.causticsStrength = v;
            case "underwaterFogDensity" -> config.underwaterFogDensity = v;
            case "foamIntensity" -> config.foamIntensity = v;
        }
        // AUTO SAVE: the game reads the config from disk (mtime cache), so a
        // change only becomes visible in-game once it is written out.
        config.save();
    }

    private boolean getToggle(String key) {
        return switch (key) {
            case "taczSwayEnabled" -> config.taczSwayEnabled;
            case "inertiaEnabled" -> config.inertiaEnabled;
            case "gaitTransitionEnabled" -> config.gaitTransitionEnabled;
            case "impactRecoveryEnabled" -> config.impactRecoveryEnabled;
            case "surfaceFootstepsEnabled" -> config.surfaceFootstepsEnabled;
            case "taczLoweringEnabled" -> config.taczLoweringEnabled;
            case "taczRunSwayEnabled" -> config.taczRunSwayEnabled;
            case "taczRecoilKickEnabled" -> config.taczRecoilKickEnabled;
            case "taczAdsStabilization" -> config.taczAdsStabilization;
            case "taczMovePenaltyEnabled" -> config.taczMovePenaltyEnabled;
            case "taczLandingLagEnabled" -> config.taczLandingLagEnabled;
            case "taczWallPullbackEnabled" -> config.taczWallPullbackEnabled;
            case "taczReloadNodEnabled" -> config.taczReloadNodEnabled;
            case "chestCamEnabled" -> config.chestCamEnabled;
            case "postFXEnabled" -> config.postFXEnabled;
            case "autoExposureEnabled" -> config.autoExposureEnabled;
            case "flashShockEnabled" -> config.flashShockEnabled;
            case "lowLightNoiseEnabled" -> config.lowLightNoiseEnabled;
            case "lensContaminationEnabled" -> config.lensContaminationEnabled;
            case "ghostFlareEnabled" -> config.ghostFlareEnabled;
            case "lensProfileEnabled" -> config.lensProfileEnabled;
            case "dustEnabled" -> config.dustEnabled;
            case "criticalCamEnabled" -> config.criticalCamEnabled;
            case "motionBlurEnabled" -> config.motionBlurEnabled;
            case "waterEnabled" -> config.waterEnabled;
            case "syncWithShader" -> config.syncWithShader;
            case "disableVanillaBobbing" -> config.disableVanillaBobbing;
            default -> false;
        };
    }

    private void setToggle(String key, boolean v) {
        switch (key) {
            case "taczSwayEnabled" -> config.taczSwayEnabled = v;
            case "inertiaEnabled" -> config.inertiaEnabled = v;
            case "gaitTransitionEnabled" -> config.gaitTransitionEnabled = v;
            case "impactRecoveryEnabled" -> config.impactRecoveryEnabled = v;
            case "surfaceFootstepsEnabled" -> config.surfaceFootstepsEnabled = v;
            case "taczLoweringEnabled" -> config.taczLoweringEnabled = v;
            case "taczRunSwayEnabled" -> config.taczRunSwayEnabled = v;
            case "taczRecoilKickEnabled" -> config.taczRecoilKickEnabled = v;
            case "taczAdsStabilization" -> config.taczAdsStabilization = v;
            case "taczMovePenaltyEnabled" -> config.taczMovePenaltyEnabled = v;
            case "taczLandingLagEnabled" -> config.taczLandingLagEnabled = v;
            case "taczWallPullbackEnabled" -> config.taczWallPullbackEnabled = v;
            case "taczReloadNodEnabled" -> config.taczReloadNodEnabled = v;
            case "chestCamEnabled" -> config.chestCamEnabled = v;
            case "postFXEnabled" -> config.postFXEnabled = v;
            case "autoExposureEnabled" -> config.autoExposureEnabled = v;
            case "flashShockEnabled" -> config.flashShockEnabled = v;
            case "lowLightNoiseEnabled" -> config.lowLightNoiseEnabled = v;
            case "lensContaminationEnabled" -> config.lensContaminationEnabled = v;
            case "ghostFlareEnabled" -> config.ghostFlareEnabled = v;
            case "lensProfileEnabled" -> config.lensProfileEnabled = v;
            case "dustEnabled" -> config.dustEnabled = v;
            case "criticalCamEnabled" -> config.criticalCamEnabled = v;
            case "motionBlurEnabled" -> config.motionBlurEnabled = v;
            case "waterEnabled" -> config.waterEnabled = v;
            case "syncWithShader" -> config.syncWithShader = v;
            case "disableVanillaBobbing" -> config.disableVanillaBobbing = v;
        }
        config.save();  // AUTO SAVE (see setVal)
    }

    private Text toggleLabel(String key, boolean val) {
        return Text.translatable("bodycam.config." + key)
            .append(": ")
            .append(Text.translatable(val ? "options.on" : "options.off"));
    }

    private void clearAndReinit() {
        this.clearAndInit();
    }

    @Override
    public void close() {
        config.save();  // AUTO SAVE: leaving with ESC keeps whatever was edited
        this.client.setScreen(this.parent);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderDofBlur(context);

        context.fill(panelX, 2, panelX + panelW, TOP_BAR - 2, 0xB0141419);
        context.fill(panelX, contentTop - 2, panelX + panelW, contentBot + 2, 0x8C0A0A0E);
        context.fill(panelX, this.height - BOTTOM_BAR + 2, panelX + panelW, this.height - 2, 0xB0141419);
        context.fill(panelX, 2, panelX + panelW, 3, 0x50FFFFFF);
        context.fill(panelX, contentTop - 2, panelX + panelW, contentTop - 1, 0x50FFFFFF);
        context.fill(panelX, this.height - BOTTOM_BAR + 2, panelX + panelW, this.height - BOTTOM_BAR + 3, 0x50FFFFFF);

        super.render(context, mouseX, mouseY, delta);

        for (Item i : items) {
            if (i.type != I_HDR && i.type != I_WARN) continue;
            int sy = contentTop + i.y - (int) scroll;
            if (sy < contentTop || sy + HDR_H > contentBot) continue;
            if (i.type == I_HDR) {
                context.fill(lx, sy, lx + 2, sy + 12, 0xFFFFAA00);
                context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("bodycam.config.category." + i.key).formatted(Formatting.GOLD),
                    lx + 6, sy + 2, 0xFFFFFF);
                context.fill(lx + 6, sy + 13, lx + fullW, sy + 14, 0x60FFAA00);
            } else if ("EXTREME".equalsIgnoreCase(config.preset)) {
                context.drawTextWithShadow(this.textRenderer,
                    Text.translatable("bodycam.config.motionSicknessWarning").formatted(Formatting.RED),
                    lx, sy + 2, 0xFF4444);
            }
        }

        int maxScroll = Math.max(0, (int) contentH - (contentBot - contentTop));
        if (maxScroll > 0) {
            int trackTop = contentTop;
            int trackBot = contentBot;
            int thumbH = Math.max(16, (int) ((trackBot - trackTop) * (double) (trackBot - trackTop) / contentH));
            int thumbY = trackTop + (int) ((trackBot - trackTop - thumbH) * (scroll / maxScroll));
            context.fill(panelX + panelW - 6, trackTop, panelX + panelW - 3, trackBot, 0x50000000);
            context.fill(panelX + panelW - 6, thumbY, panelX + panelW - 3, thumbY + thumbH, 0xC0FFFFFF);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    private static class BodycamSlider extends SliderWidget {
        private final Text label;
        private final double min, max;
        private final Consumer<Double> setter;

        BodycamSlider(int x, int y, int w, int h, Text label, double norm, double min, double max, Consumer<Double> setter) {
            super(x, y, w, h, label, norm);
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double real = min + value * (max - min);
            setMessage(label.copy().append(": ").append(Text.literal(String.format("%.2f", real))));
        }

        @Override
        protected void applyValue() {
            double real = min + value * (max - min);
            setter.accept(real);
        }

        @Override
        public void renderButton(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
            int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
            boolean hov = this.isHovered();
            context.fill(x, y, x + w, y + h, hov ? 0xD034343E : 0xC022222A);
            context.fill(x, y, x + w, y + 1, 0x40FFFFFF);
            context.fill(x, y + h - 1, x + w, y + h, 0x40000000);
            int tw = Math.max(0, w - 4);
            int fw = (int) (tw * value);
            context.fill(x + 2, y + h - 5, x + 2 + fw, y + h - 3, 0xFFFFAA00);
            context.fill(x + 2, y + h - 5, x + 2 + tw, y + h - 3, 0x30FFFFFF);
            String s = MinecraftClient.getInstance().textRenderer.trimToWidth(getMessage().getString(), w - 8);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, net.minecraft.text.Text.literal(s),
                x + w / 2, y + 5, 0xFFFFFF);
        }
    }
}

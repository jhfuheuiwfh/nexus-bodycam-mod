package dev.bodycam.camera;

/**
 * Semi-implicit Euler spring (2nd order) with fixed sub-stepping.
 * Single implementation shared by every spring in the mod (step damping,
 * landing kicks, camera inertia, weapon sway, recoil, ...) so the dt clamp
 * and sub-step size can never drift apart between call sites.
 *
 * Position units are whatever the caller feeds (blocks, degrees, ...);
 * omega is the angular frequency (rad/s), zeta the damping ratio.
 */
public final class Spring {
    /** Max simulated dt per call — guards against FPS spikes / long pauses. */
    public static final double MAX_DT = 0.1;
    /** Integration sub-step — keeps stiff springs numerically stable. */
    public static final double SUBSTEP = 0.008;

    public double pos = 0.0;
    public double vel = 0.0;

    public Spring() {}

    public Spring(double pos) {
        this.pos = pos;
    }

    public static double clampDt(double dt) {
        if (!(dt > 0.0)) return 0.0;
        return Math.min(dt, MAX_DT);
    }

    /** Integrate towards {@code target} (plain error). */
    public void step(double dt, double omega, double zeta, double target) {
        integrate(dt, omega, zeta, target, false);
    }

    /** Integrate towards {@code target} with the position error wrapped to (-180,180] — for yaw in degrees. */
    public void stepWrapped(double dt, double omega, double zeta, double target) {
        integrate(dt, omega, zeta, target, true);
    }

    private void integrate(double dt, double omega, double zeta, double target, boolean wrap) {
        double remaining = clampDt(dt);
        if (remaining <= 0.0) return;
        double c = 2.0 * zeta * omega;
        double w2 = omega * omega;
        while (remaining > 1.0e-9) {
            double h = Math.min(remaining, SUBSTEP);
            double err = wrap ? net.minecraft.util.math.MathHelper.wrapDegrees((float) (target - pos)) : (target - pos);
            vel += (w2 * err - c * vel) * h;
            pos += vel * h;
            remaining -= h;
        }
    }

    /** Instantly place the spring (kills velocity) — used on re-init / big jumps. */
    public void snap(double position) {
        pos = position;
        vel = 0.0;
    }

    public void impulse(double dv) {
        vel += dv;
    }

    public void reset() {
        pos = 0.0;
        vel = 0.0;
    }
}

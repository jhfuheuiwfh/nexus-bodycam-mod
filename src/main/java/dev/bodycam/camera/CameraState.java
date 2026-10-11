package dev.bodycam.camera;

public class CameraState {
    public static final CameraState INSTANCE = new CameraState();

    public final Spring stepSpring = new Spring();
    public final Spring landPitchSpring = new Spring();
    public final Spring landRollSpring = new Spring();
    // roadmap 01 — camera mass lag (inertia springs chase raw view angles)
    public final Spring inertiaYawSpring = new Spring();
    public final Spring inertiaPitchSpring = new Spring();
    public boolean inertiaInit = false;
    public double bobPhase = 0.0;
    public double bobVertical = 0.0;
    public double bobRoll = 0.0;
    public double bobYaw = 0.0;
    public double bobPitch = 0.0;
    public double idleNoiseT = 0.0;
    public double rollSmooth = 0.0;
    public float lastYaw = 0.0f;
    public float lastPitch = 0.0f;
    public boolean wasOnGround = true;
    public float fallDistance = 0.0f;
    public double airborneMaxFall = 0.0;
    public double airGate = 1.0;

    public double footRoll = 0.0;
    public double yawLag = 0.0;
    public double leanPitch = 0.0;
    public double breathPitch = 0.0;
    public double breathY = 0.0;
    public double breathGate = 0.0;
    public double breathT = 0.0;
    public double runTime = 0.0;
    public double exhaustion = 0.0;
    public double breathRateMult = 1.0;
    public double microT = 0.0;
    public double jitterYaw = 0.0;
    public double jitterPitch = 0.0;
    public double lastFootPhase = 0.0;
    public int footSide = 1;
    public double lastFootstepT = 0.0;
    public double footInterval = 0.0;
    // roadmap 04 — gait transition state
    public double prevSpeed = 0.0;
    public double speedAccel = 0.0;
    public double settleGate = 1.0;

    // roadmap 06/13 — auto exposure adaptation + flash shock
    public double exposureMult = 1.0;
    public double lightLevel = -1.0;
    public double flashT = 0.0;
    public double flashAmp = 0.0;
    public double flashCooldown = 0.0;
    // roadmap 08 — darkness proxy (shared by low-light grain; CPU-computed 1x/frame)
    public double darkness = 0.0;

    // roadmap 09 — lens contamination envelopes (drops / condensation / smudge)
    public double lensDrops = 0.0;
    public double lensCond = 0.0;
    public double lensSmudge = 0.0;
    public double condCd = 0.0;
    // roadmap 11 — smooth underwater blend (in fast, out slow)
    public double underwaterT = 0.0;
    public boolean wasInWater = false;
    // roadmap 07 — lens ghosting (sun/view alignment envelope)
    public double ghostAmt = 0.0;
    // roadmap 12 — dust motes visible in light
    public double dustAmt = 0.0;
    // roadmap 14 — critical damage instability
    public double criticalAmt = 0.0;
    // roadmap 02 — camera angular velocity for motion blur (deg/s, smoothed)
    public double yawVel = 0.0;
    public double pitchVel = 0.0;
    public double lastCamYaw = Double.NaN;
    public double lastCamPitch = Double.NaN;
    // roadmap 05 — surface-conditioned footfall gain
    public double surfaceGain = 1.0;

    // roadmap 03 — impact recovery: per-source trigger envelopes (falls reuse
    // the landing springs; combat/collision feed them with own cooldowns)
    public double impactCd = 0.0;
    public int impactSide = 1;

    public float prevHealth = -1.0f;
    public double damageShakeT = 0.0;
    public double damageAmp = 0.0;
    public double damageNoiseT = 0.0;

    public float viewYawSmoothed = Float.NaN;
    public float viewPitchSmoothed = Float.NaN;
    public float finalViewYaw = 0.0f;
    public float finalViewPitch = 0.0f;
    public double viewDelta = 0.0;

    private CameraState() {}
}
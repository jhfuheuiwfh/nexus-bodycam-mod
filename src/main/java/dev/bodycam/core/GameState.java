package dev.bodycam.core;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Phase: architecture (roadmap §5). One per-frame snapshot every module can
 * read instead of re-querying the world: sampled ONCE by RenderCoordinator
 * right after the frame delta is known. Keeps world lookups (light level)
 * at one per frame instead of one per subsystem.
 */
public final class GameState {
    public static final GameState INSTANCE = new GameState();

    public ClientPlayerEntity player;
    public double dt = 0.0;
    public double speed = 0.0;
    public double speedNorm = 0.0;
    public boolean sprinting = false;
    public boolean sneaking = false;
    public boolean onGround = true;
    public int lightLevel = -1;
    public double darkness = 0.0;
    public boolean valid = false;

    private GameState() {}

    public static final double WALK_SPEED = 0.216;
    public static final double SPRINT_SPEED = 0.281;

    public static void sample(ClientPlayerEntity player, double dt) {
        GameState s = INSTANCE;
        s.player = player;
        s.dt = dt;
        s.valid = player != null;
        if (player == null) return;

        Vec3d vel = player.getVelocity();
        s.speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        s.speedNorm = MathHelper.clamp(s.speed / SPRINT_SPEED, 0.0, 1.0);
        s.sprinting = player.isSprinting();
        s.sneaking = player.isSneaking();
        s.onGround = player.isOnGround();
        s.lightLevel = MathHelper.clamp(player.getWorld().getLightLevel(player.getBlockPos()), 0, 15);
        s.darkness = Math.pow(1.0 - s.lightLevel / 15.0, 1.4);
    }
}

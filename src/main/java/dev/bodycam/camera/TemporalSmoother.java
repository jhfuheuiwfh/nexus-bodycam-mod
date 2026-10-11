package dev.bodycam.camera;

public class TemporalSmoother {
    private static final int PERLIN_SIZE = 256;
    private static final int[] PERM = new int[PERLIN_SIZE * 2];
    private static final double[] GRAD3 = {
        1,1,0, -1,1,0, 1,-1,0, -1,-1,0,
        1,0,1, -1,0,1, 1,0,-1, -1,0,-1,
        0,1,1, 0,-1,1, 0,1,-1, 0,-1,-1
    };

    static {
        for (int i = 0; i < PERLIN_SIZE; i++) PERM[i] = i;
        java.util.Random r = new java.util.Random(0xC0FFEE);
        for (int i = PERLIN_SIZE - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int tmp = PERM[i]; PERM[i] = PERM[j]; PERM[j] = tmp;
        }
        for (int i = 0; i < PERLIN_SIZE; i++) PERM[PERLIN_SIZE + i] = PERM[i];
    }

    public static double expSmooth(double current, double target, double lambda, double dt) {
        return current + (target - current) * (1.0 - Math.exp(-lambda * dt));
    }

    public static double perlin2D(double x, double y) {
        int X = (int)Math.floor(x) & 255;
        int Y = (int)Math.floor(y) & 255;
        x -= Math.floor(x);
        y -= Math.floor(y);
        double u = fade(x), v = fade(y);
        int A = PERM[X] + Y, B = PERM[X + 1] + Y;
        return lerp(v, lerp(u, grad(PERM[A], x, y), grad(PERM[B], x - 1, y)),
                           lerp(u, grad(PERM[A + 1], x, y - 1), grad(PERM[B + 1], x - 1, y - 1)));
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double grad(int hash, double x, double y) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : 0);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
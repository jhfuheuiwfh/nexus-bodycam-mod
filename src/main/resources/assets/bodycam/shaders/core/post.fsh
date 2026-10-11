#version 150

in vec2 texCoord;

uniform sampler2D ScreenTex;
uniform sampler2D DirtyLensTex;

uniform float FovWideMult;
uniform float FisheyeK1;
uniform float FisheyeK2;
uniform float VignetteCosPow;
uniform float VignetteMech;
uniform float VignetteFloor;
uniform float VignetteCenterGain;
uniform float VignetteBlur;
uniform float CAStrength;
uniform float LensDirtIntensity;
uniform float LensDirtVeiling;
uniform float GrainStrength;
uniform float GrainCellSize;
uniform float Time;
uniform vec2 ScreenSize;
uniform float PostFXEnabled;
uniform float WaterRefraction;
uniform float Underwater;
uniform float Exposure;
uniform float LensDrops;
uniform float LensCond;
uniform float LensSmudge;
uniform float Ghost;
uniform float Dust;
uniform float EdgeProfile;
uniform vec2 BlurVec;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

vec2 fisheyeDistort(vec2 uv, float k1, float k2) {
    vec2 centered = uv - 0.5;
    float r2 = dot(centered, centered);
    float r4 = r2 * r2;
    float factor = 1.0 + k1 * r2 + k2 * r4;
    return centered * factor + 0.5;
}

float vignette(vec2 uv, float cosPow, float mech, float floor, float centerGain) {
    vec2 centered = uv - 0.5;
    float r = length(centered) * 1.4142;
    float cosV = pow(max(0.0, cos(r * 3.14159 * 0.5)), cosPow);
    float mechV = 1.0 - mech * smoothstep(0.75, 1.45, r);
    float v = cosV * mechV;
    v = max(v, floor);
    v = mix(v, centerGain, smoothstep(0.0, 0.3, r));
    return clamp(v, 0.0, 1.5);
}

float lensDirt(vec2 uv, float intensity, float veiling) {
    vec4 texColor = texture(DirtyLensTex, uv);
    float dirt = texColor.a;
    dirt = smoothstep(0.1, 0.6, dirt);
    return dirt * intensity + veiling;
}

float grain(vec2 uv, float strength, float cellSize, float time) {
    vec2 cell = floor(uv * ScreenSize / cellSize);
    float t = floor(time * 30.0);
    float n = hash(cell + t);
    float mono = mix(1.0, 0.12, smoothstep(0.25, 0.85, n));
    float chroma = hash(cell + t + 100.0);
    float g = mix(mono, chroma, 0.25);
    return (g - 0.5) * strength;
}

void main() {
    vec2 uv = texCoord;

    // waterRefraction: ripple the whole sample while in water (roadmap A4)
    if (PostFXEnabled > 0.5 && Underwater > 0.5 && WaterRefraction > 0.0001) {
        float t = Time * 0.8;
        vec2 asp = vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0);
        vec2 ripple = vec2(
            fbm(uv * asp * 7.0 + vec2(t * 0.9, t * 0.5)),
            fbm(uv * asp * 7.0 + vec2(-t * 0.7, t * 1.1) + 19.0)
        ) - 0.5;
        uv += ripple * WaterRefraction;
        uv = clamp(uv, 0.0, 1.0);
    }

    vec4 color = texture(ScreenTex, uv);

    if (PostFXEnabled > 0.5) {
        vec2 distortedUV = fisheyeDistort(uv, FisheyeK1, FisheyeK2);
        if (distortedUV.x < 0.0 || distortedUV.x > 1.0 || distortedUV.y < 0.0 || distortedUV.y > 1.0) {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
            return;
        }

        vec2 duv = distortedUV - 0.5;

        // roadmap 10 — lens profile: chromatic fringing grows toward the edge
        vec2 caOffset = duv * CAStrength * (1.0 + dot(duv, duv) * 2.0 * EdgeProfile);
        float r = texture(ScreenTex, distortedUV + caOffset).r;
        float g = texture(ScreenTex, distortedUV).g;
        float b = texture(ScreenTex, distortedUV - caOffset).b;
        color = vec4(r, g, b, color.a);

        // roadmap 02 — angular motion blur: symmetric smear along the camera's
        // rotation vector (zero while still → the whole block is skipped)
        if (dot(BlurVec, BlurVec) > 1.0e-8) {
            vec3 sum = vec3(0.0);
            for (int i = 1; i <= 4; i++) {
                float t = float(i) / 4.0;
                sum += texture(ScreenTex, distortedUV + BlurVec * t).rgb;
                sum += texture(ScreenTex, distortedUV - BlurVec * t).rgb;
            }
            color.rgb = mix(color.rgb, sum / 8.0, 0.6);
        }

        // roadmap 10 — peripheral softness on wide lens profiles
        if (EdgeProfile > 0.001) {
            float rr = length(duv) * 1.4142;
            float edgeMask = smoothstep(0.35, 0.7, rr) * EdgeProfile;
            vec3 soft = texture(ScreenTex, distortedUV + duv * 0.012).rgb;
            color.rgb = mix(color.rgb, soft, edgeMask * 0.45);
        }

        // roadmap 06/13 — sensor gain at capture: auto exposure + flash shock
        // (state computed CPU-side per frame; asymmetric adaptation lives there)
        color.rgb *= Exposure;

        // roadmap 11 — underwater lens: gradual cold cast as the blend rises
        if (Underwater > 0.001) {
            color.rgb = mix(color.rgb, color.rgb * vec3(0.78, 0.92, 1.05), Underwater * 0.6);
        }

        // roadmap 09 — lens contamination (drops / condensation / smudge)
        if (LensDrops > 0.001) {
            vec2 asp = vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0);
            vec2 gp = distortedUV * asp * 7.0;
            vec2 cell = floor(gp);
            vec2 f = fract(gp);
            float act = step(hash(cell), 0.4 * LensDrops);
            vec2 c = vec2(hash(cell + 3.7), hash(cell + 9.1)) * 0.5 + 0.25;
            vec2 dv = f - c;
            float d = length(dv);
            float drop = act * smoothstep(0.30, 0.06, d);
            if (drop > 0.001) {
                vec2 n = dv / max(d, 1e-4);
                vec2 src = clamp(distortedUV + n * drop * 0.015 * LensDrops, 0.0, 1.0);
                color.rgb = mix(color.rgb, texture(ScreenTex, src).rgb, drop * 0.85);
                color.rgb += drop * 0.05;
            }
        }
        if (LensCond > 0.001) {
            vec2 asp = vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0);
            float m = fbm(distortedUV * asp * 5.0 + vec2(31.0, 17.0));
            float veil = smoothstep(0.35, 0.85, m) * LensCond;
            color.rgb = mix(color.rgb, vec3(0.72, 0.77, 0.82), veil * 0.35);
        }
        if (LensSmudge > 0.001) {
            vec2 asp = vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0);
            float s = fbm(distortedUV * asp * 2.5 + vec2(3.3, 77.0));
            float band = smoothstep(0.45, 0.78, s) * LensSmudge;
            vec3 smear = texture(ScreenTex, clamp(distortedUV + vec2(0.004, 0.0012) * band, 0.0, 1.0)).rgb;
            color.rgb = mix(color.rgb, smear * 1.04 + 0.015, band * 0.5);
        }

        // v1.4.3 — bodycam edge blur: a real wide lens goes out of focus toward
        // the frame border, so the periphery (the vignette ring) is genuinely
        // blurred. 12-tap golden-angle disc, radius grows with the radius and
        // with VignetteBlur (0 = off, 1 = full defocus at the corner).
        if (VignetteBlur > 0.001) {
            float rr = length(duv) * 1.4142;
            float edgeMask = smoothstep(0.45, 1.05, rr) * clamp(VignetteBlur, 0.0, 1.0);
            if (edgeMask > 0.003) {
                float radius = 0.035 * edgeMask;          // ~3.5% of the screen at the corner
                vec2 px2uv = vec2(ScreenSize.y / max(ScreenSize.x, 1.0), 1.0); // keep the disc round
                vec3 sum = vec3(0.0);
                for (int i = 0; i < 12; i++) {
                    float fi = float(i);
                    float ang = fi * 2.39996323;           // golden angle
                    float ring = sqrt((fi + 0.5) / 12.0);  // uniform disc sampling
                    vec2 o = vec2(cos(ang) * px2uv.x, sin(ang)) * ring * radius;
                    sum += texture(ScreenTex, clamp(distortedUV + o, 0.0, 1.0)).rgb;
                }
                color.rgb = mix(color.rgb, sum / 12.0, edgeMask);
            }
        }

        float vig = vignette(distortedUV, VignetteCosPow, VignetteMech, VignetteFloor, VignetteCenterGain);
        color.rgb *= vig;

        float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        float bloomProxy = max(lum, 0.0);
        float dirt = lensDirt(distortedUV, LensDirtIntensity, LensDirtVeiling);
        color.rgb += bloomProxy * dirt * vec3(1.0, 0.95, 0.9);

        // roadmap 07 — lens ghosting: veiling flare toward the sun
        if (Ghost > 0.001) {
            float rr = length(duv) * 1.4142;
            float ring = smoothstep(0.5, 0.72, rr) * (1.0 - smoothstep(0.72, 1.0, rr));
            float core = 1.0 - smoothstep(0.0, 0.5, rr);
            float hot = smoothstep(0.45, 1.0, lum);
            color.rgb += (ring * 0.5 + core * 0.3) * Ghost * (0.35 + 0.65 * hot) * vec3(1.0, 0.96, 0.86);
        }

        // roadmap 12 — dust motes drifting through the light
        if (Dust > 0.001) {
            vec2 asp = vec2(ScreenSize.x / max(ScreenSize.y, 1.0), 1.0);
            float t = Time * 0.05;
            vec2 gp1 = distortedUV * asp * 9.0 + vec2(t, -t * 0.6);
            vec2 cell1 = floor(gp1);
            float h1 = hash(cell1);
            vec2 f1 = fract(gp1) - vec2(hash(cell1 + 1.3), hash(cell1 + 5.7)) * 0.6 - 0.2;
            float mote1 = step(0.92, h1) * (1.0 - smoothstep(0.0, 0.18, length(f1)));
            vec2 gp2 = distortedUV * asp * 14.0 + vec2(-t * 0.7, t * 0.45) + 50.0;
            vec2 cell2 = floor(gp2);
            float h2 = hash(cell2);
            vec2 f2 = fract(gp2) - vec2(hash(cell2 + 8.1), hash(cell2 + 2.9)) * 0.6 - 0.2;
            float mote2 = step(0.94, h2) * (1.0 - smoothstep(0.0, 0.2, length(f2)));
            float pulse = 0.7 + 0.3 * sin(Time * 0.8 + h1 * 6.2831);
            color.rgb += (mote1 * 0.6 + mote2 * 0.4) * pulse * Dust * vec3(1.0, 0.97, 0.9);
        }

        float gr = grain(distortedUV, GrainStrength, GrainCellSize, Time);
        color.rgb = color.rgb + gr * (1.0 - color.rgb * 0.5);
    }

    fragColor = color;
}

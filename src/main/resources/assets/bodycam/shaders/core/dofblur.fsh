#version 150

in vec2 texCoord;

uniform sampler2D ScreenTex;
uniform float BlurRadius;
uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    vec2 px = BlurRadius / max(ScreenSize, vec2(1.0));

    vec4 c = texture(ScreenTex, texCoord) * 0.20;

    c += texture(ScreenTex, texCoord + vec2( 1.0,  0.0) * px) * 0.075;
    c += texture(ScreenTex, texCoord + vec2(-1.0,  0.0) * px) * 0.075;
    c += texture(ScreenTex, texCoord + vec2( 0.0,  1.0) * px) * 0.075;
    c += texture(ScreenTex, texCoord + vec2( 0.0, -1.0) * px) * 0.075;
    c += texture(ScreenTex, texCoord + vec2( 0.7,  0.7) * px) * 0.075;
    c += texture(ScreenTex, texCoord + vec2(-0.7,  0.7) * px) * 0.075;
    c += texture(ScreenTex, texCoord + vec2( 0.7, -0.7) * px) * 0.075;
    c += texture(ScreenTex, texCoord + vec2(-0.7, -0.7) * px) * 0.075;

    c += texture(ScreenTex, texCoord + vec2( 2.1,  0.0) * px) * 0.025;
    c += texture(ScreenTex, texCoord + vec2(-2.1,  0.0) * px) * 0.025;
    c += texture(ScreenTex, texCoord + vec2( 0.0,  2.1) * px) * 0.025;
    c += texture(ScreenTex, texCoord + vec2( 0.0, -2.1) * px) * 0.025;
    c += texture(ScreenTex, texCoord + vec2( 1.5,  1.5) * px) * 0.025;
    c += texture(ScreenTex, texCoord + vec2(-1.5,  1.5) * px) * 0.025;
    c += texture(ScreenTex, texCoord + vec2( 1.5, -1.5) * px) * 0.025;
    c += texture(ScreenTex, texCoord + vec2(-1.5, -1.5) * px) * 0.025;

    fragColor = vec4(c.rgb, 1.0);
}

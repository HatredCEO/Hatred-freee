#version 150

#moj_import <modid:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform float Radius;
uniform float Spread;
uniform float Padding;
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 paddedSize = Size + vec2(Padding * 2.0);
    vec2 localPosition = FragCoord * paddedSize - vec2(Padding);
    vec2 center = Size * 0.5;
    float distance = roundedBoxSDF(center - localPosition, max(center - 1.0, vec2(0.0)), vec4(Radius));
    float fade = 1.0 - smoothstep(0.0, max(Spread, 0.001), max(distance, 0.0));
    float alpha = FragColor.a * fade * 0.72;

    if (alpha <= 0.001) {
        discard;
    }

    OutColor = vec4(FragColor.rgb, alpha) * ColorModulator;
}

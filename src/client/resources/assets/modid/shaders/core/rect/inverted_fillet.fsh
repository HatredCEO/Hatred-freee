#version 150

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform float Side;
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 p = FragCoord * Size;
    vec2 center;
    if (Side < 0.5) {
        center = vec2(0.0, Size.y);
    } else if (Side < 1.5) {
        center = vec2(Size.x, Size.y);
    } else if (Side < 2.5) {
        center = vec2(0.0, 0.0);
    } else {
        center = vec2(Size.x, 0.0);
    }
    float dist = length(p - center);
    float radius = min(Size.x, Size.y);
    float d = radius - dist;

    float alpha = clamp(0.5 - d, 0.0, 1.0);
    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);
    if (finalColor.a <= 0.0) {
        discard;
    }

    OutColor = finalColor * ColorModulator;
}

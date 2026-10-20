#version 150

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform float Padding;
uniform float Thickness;
uniform float Smoothness;
uniform vec4 ColorModulator;

out vec4 OutColor;

float rectangleSDF(vec2 point, vec2 halfSize) {
	vec2 distance = abs(point) - halfSize;
	return length(max(distance, 0.0)) + min(max(distance.x, distance.y), 0.0);
}

void main() {
	vec2 paddedSize = Size + vec2(Padding * 2.0);
	vec2 point = FragCoord * paddedSize - vec2(Padding) - Size * 0.5;
	float distance = abs(rectangleSDF(point, Size * 0.5));
	float halfThickness = Thickness * 0.5;
	float alpha = 1.0 - smoothstep(halfThickness, halfThickness + Smoothness, distance);
	vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha) * ColorModulator;
	if (finalColor.a <= 0.001) {
		discard;
	}
	OutColor = finalColor;
}

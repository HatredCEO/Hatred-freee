#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec4 ResData;
uniform vec4 FireData;
uniform vec4 TrailData;
uniform vec4 FireColor;

float sampleMask(vec2 coordinate) {
	return clamp(texture(Sampler0, coordinate).r, 0.0, 1.0);
}

void main() {
	vec2 texelSize = 1.0 / ResData.xy;
	vec4 glow = texture(Sampler1, texCoord);
	vec4 trail = texture(Sampler2, texCoord);
	float handMask = sampleMask(texCoord);
	float outsideMask = 1.0 - smoothstep(0.015, 0.20, handMask);
	float fireGate = mix(outsideMask, 1.0, clamp(ResData.w, 0.0, 1.0));
	float neighborAlpha = 0.0;
	neighborAlpha = max(neighborAlpha, sampleMask(texCoord + vec2(texelSize.x, 0.0)));
	neighborAlpha = max(neighborAlpha, sampleMask(texCoord - vec2(texelSize.x, 0.0)));
	neighborAlpha = max(neighborAlpha, sampleMask(texCoord + vec2(0.0, texelSize.y)));
	neighborAlpha = max(neighborAlpha, sampleMask(texCoord - vec2(0.0, texelSize.y)));
	float objectEdge = clamp(max(glow.a * 1.12, neighborAlpha) * fireGate, 0.0, 1.0);
	vec3 addition = vec3(0.0);
	float trailVisibility = clamp(0.88 + objectEdge * 0.32, 0.0, 1.0) * fireGate;
	addition += trail.rgb * trailVisibility * TrailData.w * 0.86;
	float glowAlpha = clamp(glow.a * (0.94 + objectEdge * 0.32) * fireGate, 0.0, 0.76);
	addition += glow.rgb * glowAlpha * (0.72 + FireColor.a * 0.26);
	fragColor = vec4(clamp(addition, 0.0, 1.0), 1.0);
}

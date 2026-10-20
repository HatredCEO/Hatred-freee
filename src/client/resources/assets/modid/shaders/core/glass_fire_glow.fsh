#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ResData;
uniform vec4 FireData;
uniform vec4 TrailData;
uniform vec4 FireColor;

const int RINGS = 6;
const int DIRS = 12;
const float TAU = 6.28318530718;

float hash12(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
	vec2 i = floor(p);
	vec2 f = fract(p);
	vec2 u = f * f * (3.0 - 2.0 * f);
	return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
		mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x), u.y);
}

vec3 itemColor(vec3 color) {
	color = clamp(color, 0.0, 1.0);
	float maximum = max(max(color.r, color.g), color.b);
	if (maximum < 0.04) return vec3(0.0);
	float gray = dot(color, vec3(0.299, 0.587, 0.114));
	float saturation = maximum - min(min(color.r, color.g), color.b);
	vec3 vivid = mix(vec3(gray), color, 1.0 + saturation * 0.45);
	return clamp(vivid / max(max(max(vivid.r, vivid.g), vivid.b), 0.20), 0.0, 1.0)
		* min(maximum * 1.18, 1.0);
}

void main() {
	vec2 texelSize = 1.0 / ResData.xy;
	float flowTime = ResData.z * FireData.z;
	float centerAlpha = texture(Sampler1, texCoord).r;
	float outsideMask = 1.0 - smoothstep(0.015, 0.22, centerAlpha);
	float nearby = centerAlpha;
	for (int direction = 0; direction < DIRS; direction++) {
		float angle = float(direction) / float(DIRS) * TAU;
		vec2 ray = vec2(cos(angle), sin(angle)) * texelSize * FireData.x;
		nearby = max(nearby, texture(Sampler1, texCoord + ray).r);
		nearby = max(nearby, texture(Sampler1, texCoord + ray * 0.5).r);
	}
	if (nearby <= 0.001) {
		fragColor = vec4(0.0);
		return;
	}
	float energy = 0.0;
	float colorWeight = 0.0;
	vec3 colorSum = vec3(0.0);

	for (int ring = 1; ring <= RINGS; ring++) {
		float ringProgress = float(ring) / float(RINGS);
		float distancePx = FireData.x * ringProgress;
		float ringWeight = pow(1.0 - ringProgress * 0.86, 1.45);
		for (int direction = 0; direction < DIRS; direction++) {
			float directionProgress = float(direction) / float(DIRS);
			float warp = noise(texCoord * 9.0 + vec2(float(ring) * 3.4,
				float(direction) * 2.2 + flowTime * 0.16));
			float angle = directionProgress * TAU + (warp - 0.5) * 0.34;
			float distance = distancePx * (0.90 + warp * 0.22);
			vec2 coordinate = texCoord + vec2(cos(angle), sin(angle)) * texelSize * distance;
			float sampleAlpha = clamp(texture(Sampler1, coordinate).r, 0.0, 1.0);
			vec3 sampleRgb = texture(Sampler0, coordinate).rgb;
			float weight = sampleAlpha * ringWeight;
			energy += weight;
			colorSum += sampleRgb * weight;
			colorWeight += weight;
		}
	}

	energy = energy / float(RINGS * DIRS) * 4.25;
	float softHalo = smoothstep(0.018, 0.58, energy);
	float edgeNoise = noise(texCoord * vec2(15.0, 21.0) + vec2(flowTime * 0.20, -flowTime * 0.34));
	float alpha = softHalo * outsideMask * FireData.y * (0.86 + edgeNoise * 0.18);
	alpha = clamp(alpha, 0.0, 0.72);
	vec3 color = itemColor(colorSum / max(colorWeight, 0.001));
	color = mix(color, FireColor.rgb, clamp(FireData.w, 0.0, 1.0));
	fragColor = vec4(color * (0.88 + alpha * 0.42), alpha);
}

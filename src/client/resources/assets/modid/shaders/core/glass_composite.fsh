#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform vec2 Resolution;
uniform vec4 GlassData;
uniform vec2 DistortionData;
uniform vec4 TintColor;

in vec2 texCoord;
out vec4 fragColor;

float maskAt(vec2 uv) {
	return texture(Sampler2, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

float softenedMask(vec2 uv, float radius) {
	if (radius < 0.5) return maskAt(uv);
	vec2 pixel = 1.0 / Resolution;
	float sum = maskAt(uv) * 4.0;
	float weight = 4.0;
	for (int index = 0; index < 12; index++) {
		float angle = float(index) * 0.5235987756;
		sum += maskAt(uv + vec2(cos(angle), sin(angle)) * pixel * radius);
		weight += 1.0;
	}
	return sum / weight;
}

vec3 saturation(vec3 color, float amount) {
	float luminance = dot(color, vec3(0.299, 0.587, 0.114));
	return mix(vec3(luminance), color, amount);
}

void main() {
	float sharp = maskAt(texCoord);
	float coverage = GlassData.z < 0.5
		? sharp
		: smoothstep(0.2, 0.72, softenedMask(texCoord, GlassData.z));
	vec3 afterColor = texture(Sampler3, texCoord).rgb;
	if (coverage <= 0.001) {
		fragColor = vec4(afterColor, 1.0);
		return;
	}

	vec2 pixel = 1.0 / Resolution;
	float reach = max(1.0, DistortionData.x);
	float left = softenedMask(texCoord - vec2(pixel.x * reach, 0.0), max(1.0, GlassData.z));
	float right = softenedMask(texCoord + vec2(pixel.x * reach, 0.0), max(1.0, GlassData.z));
	float down = softenedMask(texCoord - vec2(0.0, pixel.y * reach), max(1.0, GlassData.z));
	float up = softenedMask(texCoord + vec2(0.0, pixel.y * reach), max(1.0, GlassData.z));
	vec2 normal = vec2(right - left, up - down);
	vec2 refractedUv = clamp(texCoord + normal * GlassData.w * 0.055, pixel, vec2(1.0) - pixel);

	vec3 glass = texture(Sampler1, refractedUv).rgb;
	glass = saturation(glass, GlassData.x);
	glass = mix(glass, TintColor.rgb, GlassData.y);
	float rim = smoothstep(0.02, 0.35, length(normal)) * (0.08 + GlassData.w * 0.08);
	glass += TintColor.rgb * rim;
	fragColor = vec4(mix(afterColor, glass, coverage), 1.0);
}

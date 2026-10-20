#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 Resolution;
uniform vec4 OutlineData;
uniform vec4 EdgeColor;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

void main() {
	float sharp = texture(Sampler0, texCoord).r;
	float blurred = texture(Sampler1, texCoord).r;
	float outside = max(blurred - sharp * 0.72, 0.0);
	float inside = max(sharp - blurred, 0.0) * 0.35;
	float edge = clamp((outside + inside) * (2.0 + OutlineData.x * 8.0), 0.0, 1.0);
	float shimmer = 0.0;
	if (OutlineData.y > 0.5) {
		float period = max(OutlineData.w, 0.1);
		float position = fract(Time / period) * 1.35 - 0.175;
		shimmer = smoothstep(OutlineData.z, 0.0, abs(texCoord.y - position)) * edge;
	}
	vec3 color = mix(EdgeColor.rgb, vec3(1.0), shimmer * 0.7);
	fragColor = vec4(color, edge * clamp(OutlineData.x * 2.5, 0.0, 1.0));
}

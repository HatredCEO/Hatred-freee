#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform vec4 ResData;
uniform vec4 FireData;
uniform vec4 TrailData;
uniform vec4 FireColor;

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

void main() {
	vec2 texelSize = 1.0 / ResData.xy;
	float flowTime = ResData.z * FireData.z;
	float curlA = noise(texCoord * vec2(4.8, 7.2) + vec2(flowTime * 0.18, -flowTime * 0.28));
	float curlB = noise(texCoord * vec2(10.0, 13.0) + vec2(-flowTime * 0.22, flowTime * 0.16));
	float angle = (curlA * 2.0 - 1.0) * 2.15
		+ sin((texCoord.y + curlB) * 12.0 + flowTime * 1.7) * 0.74;
	float lift = mix(0.42, 1.28, clamp(TrailData.y / 56.0, 0.0, 1.0));
	vec2 flow = vec2(cos(angle) * 0.86, -lift + sin(angle) * 0.42) * texelSize * TrailData.z;

	vec4 previous = texture(Sampler0, texCoord + flow) * 0.58;
	previous += texture(Sampler0, texCoord + flow + vec2(texelSize.x, 0.0) * 1.35) * 0.12;
	previous += texture(Sampler0, texCoord + flow - vec2(texelSize.x, 0.0) * 1.35) * 0.12;
	previous += texture(Sampler0, texCoord + flow + vec2(0.0, texelSize.y) * 1.15) * 0.09;
	previous += texture(Sampler0, texCoord + flow - vec2(0.0, texelSize.y) * 1.15) * 0.09;
	previous *= TrailData.x;

	vec4 glow = texture(Sampler1, texCoord);
	vec3 handRgb = texture(Sampler2, texCoord).rgb;
	float handMask = clamp(texture(Sampler3, texCoord).r, 0.0, 1.0);
	float outsideMask = 1.0 - smoothstep(0.02, 0.24, handMask);
	float broken = noise(texCoord * vec2(16.0, 25.0) + vec2(flowTime * 0.35, -flowTime * 0.82));
	float ribbon = sin((texCoord.x - texCoord.y * 0.34) * 34.0 + flowTime * 4.2 + curlA * 5.5) * 0.5 + 0.5;
	float curlCut = smoothstep(0.20, 0.90, broken) * 0.55 + smoothstep(0.34, 0.98, ribbon) * 0.45;
	float flameAlpha = glow.a * outsideMask * (0.28 + curlCut * 0.42);
	float textureAlpha = handMask * (0.42 + glow.a * 0.18);
	vec3 handTinted = mix(handRgb, FireColor.rgb, clamp(FireData.w, 0.0, 1.0));
	vec3 sourceRgb = handTinted * textureAlpha + glow.rgb * flameAlpha;
	float sourceAlpha = clamp(max(textureAlpha, flameAlpha), 0.0, 0.74);
	float outputAlpha = 1.0 - (1.0 - previous.a) * (1.0 - clamp(sourceAlpha, 0.0, 0.68));
	vec3 outputRgb = previous.rgb + sourceRgb * (1.0 - previous.a * 0.38);
	fragColor = vec4(clamp(outputRgb, 0.0, 1.0), clamp(outputAlpha, 0.0, 0.88));
}

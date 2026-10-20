#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler3;

uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
uniform mat4 ProjMat;
uniform vec4 HiddenChamsColor;
uniform vec4 VisibleChamsColor;
uniform float HiddenOverlayStrength;
uniform float VisibleOverlayStrength;
uniform float FullBright;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in float modelViewDistance;

out vec4 fragColor;

float worldViewDistance(float depth) {
	if (depth >= 0.999999) {
		return 10000.0;
	}
	float ndcDepth = depth * 2.0 - 1.0;
	float den = ndcDepth * ProjMat[2][3] - ProjMat[2][2];
	if (abs(den) < 1.0E-7) {
		return 10000.0;
	}
	return -(ProjMat[3][2] - ndcDepth * ProjMat[3][3]) / den;
}

void main() {
	vec4 textureColor = texture(Sampler0, texCoord0);
	if (textureColor.a < 0.1) {
		discard;
	}

	ivec2 depthSize = textureSize(Sampler3, 0);
	ivec2 depthPixel = clamp(ivec2(gl_FragCoord.xy), ivec2(0), depthSize - ivec2(1));
	float worldDepth = texelFetch(Sampler3, depthPixel, 0).r;
	float worldDist = worldViewDistance(worldDepth);
	float occlusionDistance = modelViewDistance - worldDist;
	float transitionWidth = clamp(fwidth(occlusionDistance) * 1.5, 0.04, 0.20);
	float hiddenAmount = smoothstep(0.10, 0.10 + transitionWidth, occlusionDistance);

	vec4 chamsColor = mix(VisibleChamsColor, HiddenChamsColor, hiddenAmount);
	float overlayStrength = mix(VisibleOverlayStrength, HiddenOverlayStrength, hiddenAmount);
	vec4 color = textureColor * vertexColor * ColorModulator;
	color.rgb = mix(color.rgb, chamsColor.rgb, overlayStrength);
	color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
	color.rgb *= mix(lightMapColor.rgb, vec3(1.0), FullBright);

	vec4 foggedColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
	float fogSuppression = max(FullBright, hiddenAmount * 0.90);
	fragColor = mix(foggedColor, color, fogSuppression);
}

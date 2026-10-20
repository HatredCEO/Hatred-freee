#version 150

#moj_import <minecraft:fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 ChamsColor;
uniform float OverlayStrength;
uniform float FullBright;


in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord0);
    if (textureColor.a < 0.1) {
        discard;
    }

    vec4 color = textureColor * vertexColor * ColorModulator;
    color.rgb = mix(color.rgb, ChamsColor.rgb, OverlayStrength);
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
	color.rgb *= mix(lightMapColor.rgb, vec3(1.0), FullBright);
	vec4 foggedColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
	float fogSuppression = max(FullBright, OverlayStrength > 0.0 ? 0.85 : 0.0);
	fragColor = mix(foggedColor, color, fogSuppression);
}

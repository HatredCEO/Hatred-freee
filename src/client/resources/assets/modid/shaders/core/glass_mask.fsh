#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform vec2 TexelSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
	float depthBefore = texture(Sampler2, texCoord).r;
	float depthAfter = texture(Sampler3, texCoord).r;
	bool isHand = depthBefore - depthAfter > 0.0001;
	if (!isHand) {
		vec3 beforeColor = texture(Sampler0, texCoord).rgb;
		vec3 afterColor = texture(Sampler1, texCoord).rgb;
		vec3 difference = abs(afterColor - beforeColor);
		isHand = max(max(difference.r, difference.g), difference.b) > 0.01;
	}
	float value = isHand ? 1.0 : 0.0;
	fragColor = vec4(value, value, value, value);
}

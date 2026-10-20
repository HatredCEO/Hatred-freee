#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform float Offset;
uniform float UpPass;

in vec2 texCoord;
out vec4 fragColor;

void main() {
	vec2 halfPixel = TexelSize * 0.5 * Offset;
	vec4 sum;
	if (UpPass < 0.5) {
		sum = texture(Sampler0, texCoord) * 4.0;
		sum += texture(Sampler0, texCoord + vec2(-halfPixel.x, -halfPixel.y));
		sum += texture(Sampler0, texCoord + vec2( halfPixel.x, -halfPixel.y));
		sum += texture(Sampler0, texCoord + vec2(-halfPixel.x,  halfPixel.y));
		sum += texture(Sampler0, texCoord + vec2( halfPixel.x,  halfPixel.y));
		fragColor = vec4((sum / 8.0).rgb, sum.a / 8.0);
	} else {
		sum = texture(Sampler0, texCoord + vec2(-2.0 * halfPixel.x, 0.0));
		sum += texture(Sampler0, texCoord + vec2(-halfPixel.x, halfPixel.y)) * 2.0;
		sum += texture(Sampler0, texCoord + vec2(0.0, 2.0 * halfPixel.y));
		sum += texture(Sampler0, texCoord + vec2(halfPixel.x, halfPixel.y)) * 2.0;
		sum += texture(Sampler0, texCoord + vec2(2.0 * halfPixel.x, 0.0));
		sum += texture(Sampler0, texCoord + vec2(halfPixel.x, -halfPixel.y)) * 2.0;
		sum += texture(Sampler0, texCoord + vec2(0.0, -2.0 * halfPixel.y));
		sum += texture(Sampler0, texCoord + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;
		fragColor = sum / 12.0;
	}
}

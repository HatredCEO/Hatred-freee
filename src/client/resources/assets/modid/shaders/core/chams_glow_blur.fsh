#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 TexelSize;
uniform vec2 Direction;
uniform float Radius;

in vec2 texCoord;
out vec4 fragColor;

float gaussian(float offset, float sigma) {
	return exp(-(offset * offset) / (2.0 * sigma * sigma));
}

void main() {
	if (Direction.x == 0.0 && texture(Sampler1, texCoord).a > 0.01) {
		discard;
	}

	float sigma = max(Radius * 0.5, 0.001);
	vec4 center = texture(Sampler0, texCoord);
	center.rgb *= center.a;
	float centerWeight = gaussian(0.0, sigma);
	vec4 blurred = center * centerWeight;
	float totalWeight = centerWeight;

	for (int index = 1; index <= 8; index++) {
		float offset = float(index);
		if (offset > Radius) {
			break;
		}
		float weight = gaussian(offset, sigma);
		vec2 sampleOffset = offset * TexelSize * Direction;
		vec4 left = texture(Sampler0, texCoord - sampleOffset);
		vec4 right = texture(Sampler0, texCoord + sampleOffset);
		left.rgb *= left.a;
		right.rgb *= right.a;
		blurred += (left + right) * weight;
		totalWeight += weight * 2.0;
	}

	blurred /= totalWeight;
	if (blurred.a <= 0.001) {
		discard;
	}
	fragColor = vec4(blurred.rgb / blurred.a, min(blurred.a * 1.785, 1.0));
}

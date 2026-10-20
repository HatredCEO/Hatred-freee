#version 150

uniform sampler2D Sampler0;
uniform vec2 TextureSize;
uniform float Range;
uniform float EdgeStrength;
uniform float Thickness;
uniform vec4 Color;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

void main() {
    vec4 texColor = texture(Sampler0, texCoord);

    float dx = dFdx(texCoord.x) * TextureSize.x;
    float dy = dFdy(texCoord.y) * TextureSize.y;
    float toPixels = max(Range * inversesqrt(dx * dx + dy * dy), 1.0);

    float sigDist = median(texColor.r, texColor.g, texColor.b) - 0.5 + Thickness;
    float alpha = clamp(sigDist * toPixels + 0.5, 0.0, 1.0);

    fragColor = vec4(Color.rgb, Color.a * alpha) * vertexColor;
}

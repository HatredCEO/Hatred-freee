#version 150

in vec3 Position;
in vec4 Color;

uniform float FarDepth;

smooth out vec2 ScreenPosition;
smooth out vec4 FragColor;

void main() {
    ScreenPosition = Position.xy;
    FragColor = Color;
    gl_Position = vec4(Position.xy, FarDepth, 1.0);
}

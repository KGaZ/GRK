#version 330 core

out vec4 fragColor;

uniform vec3 u_lineColor;

void main() {
    fragColor = vec4(u_lineColor, 1.0);
}
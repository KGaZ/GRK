#version 330 core

layout (location = 0) in vec3 aPosA;
layout (location = 1) in vec3 aPosB;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform float u_morphFactor;

void main() {
    vec3 morphedPos = mix(aPosA, aPosB, u_morphFactor);

    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(morphedPos, 1.0);
}
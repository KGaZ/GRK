#version 330 core

// Zmienione layout locations, aby pasowały do VBO z przeplatanymi danymi:
// layout (location = 0) to pozycja A (offset 0)
// layout (location = 1) to pozycja B (offset 3 * Float.BYTES)
layout (location = 0) in vec3 aPosA; // Pozycja wierzchołka z kształtu A
layout (location = 1) in vec3 aPosB; // Pozycja wierzchołka z kształtu B

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform float u_morphFactor; // Współczynnik morfingu (0.0 = A, 1.0 = B)

void main() {
    // Interpoluj pozycje wierzchołków na podstawie u_morphFactor
    vec3 morphedPos = mix(aPosA, aPosB, u_morphFactor);

    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(morphedPos, 1.0);
}
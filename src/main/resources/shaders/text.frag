#version 330 core
in vec2 TexCoords;
out vec4 FragColor;

uniform sampler2D text_texture;
uniform vec3 text_color;

void main() {
    float alpha = texture(text_texture, TexCoords).r;
    FragColor = vec4(text_color, alpha);
}
#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    vec2 texel = vec2(0.0, 1.0 / InSize.y);
    vec3 color = texture(InSampler, texCoord).rgb * 0.22702703;
    color += texture(InSampler, texCoord + texel * 1.38461538).rgb * 0.31621622;
    color += texture(InSampler, texCoord - texel * 1.38461538).rgb * 0.31621622;
    color += texture(InSampler, texCoord + texel * 3.23076923).rgb * 0.07027027;
    color += texture(InSampler, texCoord - texel * 3.23076923).rgb * 0.07027027;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}

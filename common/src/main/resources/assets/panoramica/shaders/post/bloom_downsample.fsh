#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

vec3 sampleInput(vec2 uv) {
    return texture(InSampler, uv).rgb;
}

void main() {
    vec2 texel = 1.0 / InSize;
    vec3 color = sampleInput(texCoord) * 0.2500;
    color += sampleInput(texCoord + vec2(-texel.x, 0.0)) * 0.1250;
    color += sampleInput(texCoord + vec2( texel.x, 0.0)) * 0.1250;
    color += sampleInput(texCoord + vec2(0.0, -texel.y)) * 0.1250;
    color += sampleInput(texCoord + vec2(0.0,  texel.y)) * 0.1250;
    color += sampleInput(texCoord + vec2(-texel.x, -texel.y)) * 0.0625;
    color += sampleInput(texCoord + vec2( texel.x, -texel.y)) * 0.0625;
    color += sampleInput(texCoord + vec2(-texel.x,  texel.y)) * 0.0625;
    color += sampleInput(texCoord + vec2( texel.x,  texel.y)) * 0.0625;

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}

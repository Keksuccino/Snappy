#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomConfig {
    vec4 Bloom;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float EPSILON = 0.00001;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, LUMA);
}

float highlightBrightness(vec3 color) {
    return max(luminance(color), max(max(color.r, color.g), color.b) * 0.74);
}

float softThreshold(float brightness, float threshold, float knee) {
    float soft = brightness - threshold + knee;
    soft = clamp(soft, 0.0, knee * 2.0);
    soft = soft * soft / max(knee * 4.0, EPSILON);
    return max(brightness - threshold, soft) / max(brightness, EPSILON);
}

vec3 sampleScene(vec2 uv) {
    return clamp(texture(InSampler, uv).rgb, 0.0, 1.0);
}

vec3 tentSample(vec2 uv) {
    vec2 texel = 1.0 / InSize;
    vec3 color = sampleScene(uv) * 0.2500;
    color += sampleScene(uv + vec2(-texel.x, 0.0)) * 0.1250;
    color += sampleScene(uv + vec2( texel.x, 0.0)) * 0.1250;
    color += sampleScene(uv + vec2(0.0, -texel.y)) * 0.1250;
    color += sampleScene(uv + vec2(0.0,  texel.y)) * 0.1250;
    color += sampleScene(uv + vec2(-texel.x, -texel.y)) * 0.0625;
    color += sampleScene(uv + vec2( texel.x, -texel.y)) * 0.0625;
    color += sampleScene(uv + vec2(-texel.x,  texel.y)) * 0.0625;
    color += sampleScene(uv + vec2( texel.x,  texel.y)) * 0.0625;
    return color;
}

void main() {
    float amount = clamp(Bloom.x, 0.0, 1.0);
    vec3 color = tentSample(texCoord);
    float brightness = highlightBrightness(color);
    float contribution = softThreshold(brightness, Bloom.y, Bloom.z);
    float brightShape = smoothstep(0.42, 1.0, brightness);

    vec3 glow = color * contribution;
    vec3 warmShoulder = vec3(1.055, 1.020, 0.940);
    vec3 coolFloor = vec3(0.970, 0.990, 1.045);
    glow *= mix(coolFloor, warmShoulder, brightShape * 0.22 + amount * 0.08);
    glow *= mix(0.82, 1.10, amount);

    fragColor = vec4(clamp(glow, 0.0, 1.0), 1.0);
}

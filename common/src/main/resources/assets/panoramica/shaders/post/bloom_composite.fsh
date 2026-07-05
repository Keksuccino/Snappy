#version 330

uniform sampler2D SceneSampler;
uniform sampler2D BloomNearSampler;
uniform sampler2D BloomWideSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 SceneSize;
    vec2 NearBloomSize;
    vec2 WideBloomSize;
};

layout(std140) uniform BloomConfig {
    vec4 Bloom;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, LUMA);
}

vec3 softShoulder(vec3 color, float amount) {
    float overflow = max(max(max(color.r, color.g), color.b) - 1.0, 0.0);
    return color / (1.0 + overflow * amount);
}

void main() {
    vec4 sceneSample = texture(SceneSampler, texCoord);
    vec3 scene = clamp(sceneSample.rgb, 0.0, 1.0);
    float amount = clamp(Bloom.x, 0.0, 1.0);

    vec3 nearBloom = texture(BloomNearSampler, texCoord).rgb;
    vec3 wideBloom = texture(BloomWideSampler, texCoord).rgb;
    vec3 bloom = nearBloom * mix(0.62, 0.86, amount) + wideBloom * mix(0.92, 1.28, amount);
    bloom *= amount * Bloom.w;

    float sceneLuma = luminance(scene);
    float highlightProtection = smoothstep(0.72, 1.0, sceneLuma);
    vec3 additive = scene + bloom;
    vec3 screened = 1.0 - (1.0 - scene) * (1.0 - clamp(bloom, 0.0, 1.0));
    vec3 color = mix(additive, screened, 0.22 + amount * 0.24);
    color = mix(color, scene + bloom * 0.72, highlightProtection * (0.10 + amount * 0.10));
    color = softShoulder(color, 0.18 + amount * 0.28);

    float bloomLuma = luminance(bloom);
    vec3 glowTint = mix(vec3(1.0), vec3(1.035, 1.012, 0.965), smoothstep(0.04, 0.30, bloomLuma) * amount * 0.24);
    color *= glowTint;

    fragColor = vec4(clamp(color, 0.0, 1.0), sceneSample.a);
}

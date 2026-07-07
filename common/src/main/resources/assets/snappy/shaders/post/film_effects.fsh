#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform FilmEffectsConfig {
    vec4 FilmEffects;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float EPSILON = 0.00001;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, LUMA);
}

float hash12(vec2 value) {
    vec3 p3 = fract(vec3(value.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float triangularNoise(vec2 pixel, float seed) {
    vec2 seededPixel = pixel + vec2(seed * 13.17, seed * 7.31);
    return hash12(seededPixel) + hash12(seededPixel + vec2(37.23, 17.91)) - 1.0;
}

vec2 safeUv(vec2 uv) {
    vec2 texel = 0.5 / max(InSize, vec2(1.0));
    return clamp(uv, texel, vec2(1.0) - texel);
}

vec3 applyChromaticAberration(vec3 color, float amount) {
    if (amount <= EPSILON) {
        return color;
    }

    vec2 size = max(InSize, vec2(1.0));
    float aspect = size.x / max(size.y, 1.0);
    vec2 centered = texCoord - vec2(0.5);
    vec2 aspectCentered = vec2(centered.x * aspect, centered.y);
    float radius = clamp(length(aspectCentered) / max(length(vec2(0.5 * aspect, 0.5)), EPSILON), 0.0, 1.0);
    vec2 direction = radius > EPSILON ? centered / max(length(centered), EPSILON) : vec2(0.0);
    float edgeMask = smoothstep(0.02, 0.96, radius);
    float strength = amount * amount * (3.0 - 2.0 * amount);
    float pixelOffset = edgeMask * (amount * 0.90 + strength * 13.50);
    vec2 offset = direction * pixelOffset / size;

    vec2 redUv = safeUv(texCoord + offset * 1.16);
    vec2 cyanUv = safeUv(texCoord - offset * 0.42);
    vec2 blueUv = safeUv(texCoord - offset * 1.08);
    vec3 redSample = texture(InSampler, redUv).rgb;
    vec3 cyanSample = texture(InSampler, cyanUv).rgb;
    vec3 blueSample = texture(InSampler, blueUv).rgb;
    vec3 shifted = vec3(redSample.r, cyanSample.g, blueSample.b);

    float blend = smoothstep(0.0, 0.04, amount) * smoothstep(0.02, 0.38, radius);
    return mix(color, shifted, blend);
}

vec3 applyFilmGrain(vec3 color, float amount) {
    if (amount <= EPSILON) {
        return color;
    }

    vec2 pixel = texCoord * max(InSize, vec2(1.0));
    float seed = floor(FilmEffects.z * 4096.0);
    float luma = luminance(color);

    float fine = triangularNoise(pixel, seed);
    float medium = triangularNoise(floor(pixel * 0.50), seed + 19.0);
    float broad = triangularNoise(floor(pixel * 0.23), seed + 53.0);
    float monochromeGrain = fine * 0.70 + medium * 0.23 + broad * 0.07;

    vec3 chromaGrain = vec3(
            triangularNoise(pixel + vec2(11.0, 3.0), seed + 5.0),
            triangularNoise(pixel + vec2(7.0, 13.0), seed + 29.0),
            triangularNoise(pixel + vec2(17.0, 23.0), seed + 47.0)
    );
    chromaGrain -= vec3((chromaGrain.r + chromaGrain.g + chromaGrain.b) * 0.3333333);

    float shadowBias = mix(1.28, 0.44, smoothstep(0.04, 0.92, luma));
    float midtoneBias = 0.74 + 0.36 * (1.0 - abs(luma * 2.0 - 1.0));
    float highAmount = amount * amount * (3.0 - 2.0 * amount);
    float strength = (amount * 0.018 + highAmount * 0.154) * shadowBias * midtoneBias;
    vec3 grain = vec3(monochromeGrain) + chromaGrain * mix(0.26, 0.44, highAmount);
    vec3 availableRange = mix(color, vec3(1.0) - color, step(vec3(0.0), grain));

    return clamp(color + grain * strength * (0.58 + availableRange * 0.84), 0.0, 1.0);
}

void main() {
    vec4 scene = texture(InSampler, texCoord);
    vec3 color = clamp(scene.rgb, 0.0, 1.0);

    color = applyChromaticAberration(color, clamp(FilmEffects.y, 0.0, 1.0));
    color = applyFilmGrain(color, clamp(FilmEffects.x, 0.0, 1.0));

    fragColor = vec4(color, scene.a);
}

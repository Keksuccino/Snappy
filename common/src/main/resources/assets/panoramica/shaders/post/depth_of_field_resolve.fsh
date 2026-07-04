#version 330

uniform sampler2D ColorSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 ColorSize;
    vec2 DepthSize;
};

layout(std140) uniform DepthOfFieldConfig {
    mat4 InvProjection;
    vec4 Lens;
    vec4 Depth;
};

const int SAMPLE_COUNT = 12;
const float GOLDEN_ANGLE = 2.39996323;
const float TAU = 6.28318531;
const float SENSOR_HEIGHT_METERS = 0.024;
const float EPSILON = 0.00001;

out vec4 fragColor;

float viewDistance(vec2 uv) {
    float depth = texture(DepthSampler, uv).r;
    float ndcZ = mix(depth * 2.0 - 1.0, depth, step(0.5, Depth.y));
    vec4 clip = vec4(uv * 2.0 - 1.0, ndcZ, 1.0);
    vec4 view = InvProjection * clip;
    view.xyz /= max(abs(view.w), EPSILON);
    return clamp(length(view.xyz), 0.0, Depth.x);
}

float circleOfConfusion(float distanceFromLens) {
    float focusDistance = max(Lens.x, 0.05);
    float focalLengthMeters = clamp(Lens.y, 1.0, 300.0) * 0.001;
    float aperture = max(Lens.z, 0.7);
    float safeFocus = max(focusDistance, focalLengthMeters + 0.001);
    float safeDistance = max(distanceFromLens, focalLengthMeters + 0.001);
    float cocMeters = abs((focalLengthMeters * focalLengthMeters) / (aperture * (safeFocus - focalLengthMeters)) * ((safeDistance - safeFocus) / safeDistance));
    return clamp(cocMeters / SENSOR_HEIGHT_METERS * OutSize.y * Depth.z, 0.0, Lens.w);
}

float interleavedGradientNoise(vec2 pixel) {
    return fract(52.9829189 * fract(dot(pixel, vec2(0.06711056, 0.00583715))));
}

void main() {
    vec2 texel = 1.0 / OutSize;
    vec4 centerColor = texture(ColorSampler, texCoord);
    float centerDistance = viewDistance(texCoord);
    float centerCoc = circleOfConfusion(centerDistance);
    float resolveAmount = smoothstep(0.85, 3.75, centerCoc);

    if (resolveAmount <= EPSILON) {
        fragColor = centerColor;
        return;
    }

    float resolveRadius = mix(0.70, 1.90, smoothstep(2.0, Lens.w, centerCoc));
    float rotation = interleavedGradientNoise(floor(texCoord * OutSize) + vec2(19.0, 7.0)) * TAU;
    vec3 colorSum = centerColor.rgb * 1.65;
    float weightSum = 1.65;

    for (int i = 0; i < SAMPLE_COUNT; i++) {
        float sampleIndex = float(i) + 0.5;
        float diskRadius = sqrt(sampleIndex / float(SAMPLE_COUNT));
        float angle = sampleIndex * GOLDEN_ANGLE + rotation;
        vec2 direction = vec2(cos(angle), sin(angle));
        vec2 sampleUv = clamp(texCoord + direction * resolveRadius * diskRadius * texel, texel * 0.5, 1.0 - texel * 0.5);
        float sampleCoc = circleOfConfusion(viewDistance(sampleUv));
        float cocSimilarity = 1.0 - smoothstep(1.75, 7.0, abs(sampleCoc - centerCoc));
        float sampleBlur = smoothstep(0.65, 2.50, sampleCoc);
        float spatialWeight = exp(-diskRadius * diskRadius * 2.15);
        float weight = spatialWeight * cocSimilarity * sampleBlur;

        if (weight > EPSILON) {
            colorSum += texture(ColorSampler, sampleUv).rgb * weight;
            weightSum += weight;
        }
    }

    vec3 resolvedColor = colorSum / max(weightSum, EPSILON);
    fragColor = vec4(mix(centerColor.rgb, resolvedColor, resolveAmount * 0.86), centerColor.a);
}

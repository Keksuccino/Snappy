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

const int SAMPLE_COUNT = 28;
const float GOLDEN_ANGLE = 2.39996323;
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

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec2 texel = 1.0 / OutSize;
    vec4 centerColor = texture(ColorSampler, texCoord);
    float centerDistance = viewDistance(texCoord);
    float centerCoc = circleOfConfusion(centerDistance);

    vec3 colorSum = centerColor.rgb;
    float weightSum = 1.0;
    float blurPresence = smoothstep(0.12, 1.25, centerCoc);

    for (int i = 0; i < SAMPLE_COUNT; i++) {
        float sampleIndex = float(i) + 0.5;
        float diskRadius = sqrt(sampleIndex / float(SAMPLE_COUNT));
        float angle = sampleIndex * GOLDEN_ANGLE;
        vec2 direction = vec2(cos(angle), sin(angle));
        float tapRadius = diskRadius * Lens.w;
        vec2 sampleUv = clamp(texCoord + direction * tapRadius * texel, texel * 0.5, 1.0 - texel * 0.5);

        float sampleDistance = viewDistance(sampleUv);
        float sampleCoc = circleOfConfusion(sampleDistance);
        float foreground = step(sampleDistance, centerDistance);
        float backgroundWeight = smoothstep(tapRadius - 1.0, tapRadius + 1.0, centerCoc) * blurPresence;
        float foregroundWeight = smoothstep(tapRadius - 1.0, tapRadius + 1.0, sampleCoc) * foreground;
        float weight = max(backgroundWeight, foregroundWeight);
        weight *= mix(1.0, Depth.w, foreground);

        if (weight > EPSILON) {
            vec4 sampleColor = texture(ColorSampler, sampleUv);
            float highlight = smoothstep(0.72, 1.05, luminance(sampleColor.rgb));
            weight *= mix(1.0, 1.16, highlight * smoothstep(2.0, Lens.w, sampleCoc));
            colorSum += sampleColor.rgb * weight;
            weightSum += weight;
        }
    }

    vec3 dofColor = colorSum / max(weightSum, EPSILON);
    fragColor = vec4(dofColor, centerColor.a);
}

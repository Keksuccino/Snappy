#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlueOrangeConfig {
    vec4 RedMatrix;
    vec4 GreenMatrix;
    vec4 BlueMatrix;
    vec4 Adjustments;
    vec4 ShadowTone;
    vec4 MidTone;
    vec4 HighlightTone;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, LUMA);
}

vec3 applyMatrix(vec3 color) {
    vec4 sampleColor = vec4(color, 1.0);
    return vec3(
        dot(sampleColor, RedMatrix),
        dot(sampleColor, GreenMatrix),
        dot(sampleColor, BlueMatrix)
    );
}

vec3 preserveLuminanceMix(vec3 base, vec3 graded, float amount) {
    float baseLuma = max(luminance(base), 0.0001);
    float gradedLuma = max(luminance(graded), 0.0001);
    return mix(base, graded * (baseLuma / gradedLuma), amount);
}

vec3 contrastAroundPivot(vec3 color, float contrast, float pivot) {
    return (color - vec3(pivot)) * contrast + vec3(pivot);
}

void main() {
    vec4 diffuseColor = texture(InSampler, texCoord);
    vec3 color = clamp(diffuseColor.rgb, 0.0, 1.0);

    color = clamp(applyMatrix(color), 0.0, 1.0);
    color = pow(color, vec3(Adjustments.z));

    float baseLuma = luminance(color);
    float shadowMask = 1.0 - smoothstep(0.09, 0.50, baseLuma);
    float midMask = smoothstep(0.12, 0.45, baseLuma) * (1.0 - smoothstep(0.55, 0.88, baseLuma));
    float highlightMask = smoothstep(0.36, 0.92, baseLuma);

    vec3 shadowGrade = color * vec3(0.52, 0.58, 0.96) + ShadowTone.rgb * (0.018 + baseLuma * 0.92);
    color = mix(color, shadowGrade, ShadowTone.a * shadowMask);

    vec3 midGrade = color * MidTone.rgb;
    color = preserveLuminanceMix(color, midGrade, MidTone.a * midMask);

    vec3 highlightGrade = color * HighlightTone.rgb + HighlightTone.rgb * (0.045 * highlightMask);
    color = preserveLuminanceMix(color, highlightGrade, HighlightTone.a * highlightMask);

    float gradedLuma = luminance(color);
    color = mix(vec3(gradedLuma), color, Adjustments.x);
    color = contrastAroundPivot(color, Adjustments.y, 0.43);
    color *= Adjustments.w;

    float warmth = highlightMask * (1.0 - shadowMask * 0.45);
    color += HighlightTone.rgb * (0.014 * warmth);
    color -= ShadowTone.rgb * (0.012 * shadowMask);

    fragColor = vec4(clamp(color, 0.0, 1.0), diffuseColor.a);
}

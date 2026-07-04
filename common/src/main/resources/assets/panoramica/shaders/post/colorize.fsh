#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ColorizeConfig {
    vec4 RedMatrix;
    vec4 GreenMatrix;
    vec4 BlueMatrix;
    vec4 Adjustments;
    vec4 Tint;
    vec4 ShadowTint;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

out vec4 fragColor;

void main() {
    vec4 diffuseColor = texture(InSampler, texCoord);
    vec3 color = clamp(diffuseColor.rgb, 0.0, 1.0);
    vec4 sampleColor = vec4(color, 1.0);

    color = vec3(
        dot(sampleColor, RedMatrix),
        dot(sampleColor, GreenMatrix),
        dot(sampleColor, BlueMatrix)
    );

    float luma = dot(color, LUMA);
    color = mix(vec3(luma), color, Adjustments.x);
    color = (color - 0.5) * Adjustments.y + 0.5;
    color *= Adjustments.z;
    color = mix(color, color * Tint.rgb, Tint.a);

    float shadowWeight = 1.0 - smoothstep(0.0, 0.65, luma);
    color += ShadowTint.rgb * ShadowTint.a * shadowWeight;
    color += vec3(Adjustments.w * shadowWeight);

    fragColor = vec4(clamp(color, 0.0, 1.0), diffuseColor.a);
}

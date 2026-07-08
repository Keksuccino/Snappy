#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform ColorAdjustmentConfig {
    vec4 Adjustments;
    vec4 ColorBalance;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float EPSILON = 0.00001;

out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, LUMA);
}

vec3 applyGamma(vec3 color, float amount) {
    float gamma = exp2(-amount * 1.05);
    return pow(clamp(color, 0.0, 1.0), vec3(gamma));
}

vec3 applyOverexposure(vec3 color, float amount) {
    float stops = amount < 0.0 ? amount * 1.25 : amount * 1.70;
    vec3 exposed = color * exp2(stops);

    float positive = max(amount, 0.0);
    float highlightMask = smoothstep(0.62, 1.08, luminance(exposed));
    float bleachAmount = positive * highlightMask;
    vec3 bleached = mix(exposed, vec3(luminance(exposed)), bleachAmount * 0.42);
    vec3 softShoulder = vec3(1.0) - exp(-bleached * 1.22);

    exposed = mix(exposed, softShoulder, bleachAmount * 0.48);
    exposed += vec3(0.035 * bleachAmount);
    return exposed;
}

vec3 applyContrast(vec3 color, float amount) {
    float contrast = exp2(amount * 0.76);
    float positive = max(amount, 0.0);
    float pivot = mix(0.50, 0.43, positive);
    vec3 contrasted = (color - vec3(pivot)) * contrast + vec3(pivot);

    float shadowWeight = positive * (1.0 - smoothstep(0.04, 0.42, luminance(color)));
    contrasted -= vec3(0.018 * shadowWeight);
    return contrasted;
}

vec3 applySaturation(vec3 color, float amount) {
    float saturation = amount < 0.0 ? 1.0 + amount : 1.0 + amount * 0.85;
    float luma = luminance(color);
    return mix(vec3(luma), color, saturation);
}

vec3 applyColorBalance(vec3 color, vec3 balance) {
    vec3 original = color;
    vec3 multiplier = vec3(1.0);
    vec3 positive = max(balance, vec3(0.0));
    vec3 negative = max(-balance, vec3(0.0));

    multiplier += positive * vec3(0.34);
    multiplier -= (positive.gbr + positive.brg) * vec3(0.15);
    multiplier += (negative.gbr + negative.brg) * vec3(0.20);
    multiplier -= negative * vec3(0.18);

    vec3 balanced = color * max(multiplier, vec3(0.0));
    float originalLuma = luminance(original);
    float balancedLuma = max(luminance(balanced), EPSILON);
    balanced *= mix(1.0, originalLuma / balancedLuma, 0.34);
    return balanced;
}

void main() {
    vec4 diffuseColor = texture(InSampler, texCoord);
    vec3 color = clamp(diffuseColor.rgb, 0.0, 1.0);

    color = applyGamma(color, Adjustments.w);
    color = applyOverexposure(color, Adjustments.z);
    color = applyContrast(color, Adjustments.y);
    color = applySaturation(color, Adjustments.x);
    color = applyColorBalance(color, clamp(ColorBalance.rgb, vec3(-1.0), vec3(1.0)));

    fragColor = vec4(clamp(color, 0.0, 1.0), diffuseColor.a);
}

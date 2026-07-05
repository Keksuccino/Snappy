#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform StylizeConfig {
    vec4 Stylize;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

out vec4 fragColor;

struct SceneFeatures {
    vec3 average;
    float luma;
    float edge;
    float fineEdge;
    float rawEdge;
    float gx;
    float gy;
};

vec2 texelSize() {
    return 1.0 / max(InSize, vec2(1.0));
}

vec3 sampleScene(vec2 pixelOffset) {
    vec2 uv = clamp(texCoord + pixelOffset * texelSize(), vec2(0.0), vec2(1.0));
    return clamp(texture(InSampler, uv).rgb, 0.0, 1.0);
}

float luminance(vec3 color) {
    return dot(color, LUMA);
}

float hash12(vec2 value) {
    vec3 p3 = fract(vec3(value.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 saturateColor(vec3 color, float saturation) {
    float luma = luminance(color);
    return mix(vec3(luma), color, saturation);
}

vec3 contrastAround(vec3 color, float contrast, float pivot) {
    return (color - vec3(pivot)) * contrast + vec3(pivot);
}

vec3 posterize(vec3 color, float levels) {
    return (floor(clamp(color, 0.0, 1.0) * levels) + 0.5) / levels;
}

float strokePattern(vec2 pixel, vec2 direction, float spacing, float thickness) {
    float stripe = abs(fract(dot(pixel, normalize(direction)) / spacing) - 0.5);
    return 1.0 - smoothstep(thickness, thickness + 0.075, stripe);
}

float paperGrain(vec2 pixel) {
    float fine = hash12(floor(pixel));
    float broad = hash12(floor(pixel * 0.33));
    return (fine - 0.5) * 0.72 + (broad - 0.5) * 0.28;
}

SceneFeatures analyzeScene(vec3 center) {
    vec3 tl = sampleScene(vec2(-1.0, -1.0));
    vec3 tc = sampleScene(vec2(0.0, -1.0));
    vec3 tr = sampleScene(vec2(1.0, -1.0));
    vec3 ml = sampleScene(vec2(-1.0, 0.0));
    vec3 mr = sampleScene(vec2(1.0, 0.0));
    vec3 bl = sampleScene(vec2(-1.0, 1.0));
    vec3 bc = sampleScene(vec2(0.0, 1.0));
    vec3 br = sampleScene(vec2(1.0, 1.0));

    float tlL = luminance(tl);
    float tcL = luminance(tc);
    float trL = luminance(tr);
    float mlL = luminance(ml);
    float mrL = luminance(mr);
    float blL = luminance(bl);
    float bcL = luminance(bc);
    float brL = luminance(br);

    float gx = -tlL - 2.0 * mlL - blL + trL + 2.0 * mrL + brL;
    float gy = -tlL - 2.0 * tcL - trL + blL + 2.0 * bcL + brL;
    float rawEdge = length(vec2(gx, gy));

    vec3 average = center * 4.0 + ml + mr + tc + bc + (tl + tr + bl + br) * 0.5;
    average /= 10.0;

    float luma = luminance(center);
    float edge = smoothstep(0.095, 0.33, rawEdge);
    float fineEdge = smoothstep(0.028, 0.18, rawEdge);
    return SceneFeatures(average, luma, edge, fineEdge, rawEdge, gx, gy);
}

float sketchHatching(float luma, vec2 pixel, float strength) {
    float shade = 1.0 - smoothstep(0.08, 0.88, luma);
    float hatchA = strokePattern(pixel + vec2(1.7, -0.4), vec2(1.0, 0.42), 7.4, 0.030);
    float hatchB = strokePattern(pixel + vec2(-2.3, 3.1), vec2(0.34, 1.0), 9.8, 0.026);
    float hatchC = strokePattern(pixel + vec2(4.0, 1.0), vec2(1.0, -0.62), 12.6, 0.022);

    float result = hatchA * smoothstep(0.12, 0.60, shade);
    result += hatchB * smoothstep(0.34, 0.76, shade) * 0.72;
    result += hatchC * smoothstep(0.58, 0.92, shade) * 0.55;
    return clamp(result * strength, 0.0, 1.0);
}

vec3 applyColorSketch(SceneFeatures features, vec2 pixel) {
    float grain = paperGrain(pixel);
    vec3 paper = vec3(0.965, 0.938, 0.872) + vec3(grain * 0.030);
    vec3 color = pow(max(features.average, vec3(0.0)), vec3(0.82));
    color = saturateColor(contrastAround(color, 1.08, 0.48), 0.82);
    color = clamp(color * vec3(1.06, 1.03, 0.98), 0.0, 1.0);

    float hatch = sketchHatching(features.luma, pixel, 0.72);
    float line = clamp(features.edge * 0.82 + features.fineEdge * 0.28 + hatch * 0.48, 0.0, 1.0);
    vec3 ink = vec3(0.075, 0.068, 0.060);
    vec3 base = mix(paper, color, 0.86);
    base *= 1.0 - hatch * 0.14;
    base += vec3(grain * 0.018);

    vec3 result = mix(base, ink, clamp(line * 0.74, 0.0, 0.88));
    result = mix(result, color, (1.0 - features.edge) * 0.08);
    return clamp(result, 0.0, 1.0);
}

vec3 applyPencilSketch(SceneFeatures features, vec2 pixel) {
    float grain = paperGrain(pixel);
    vec3 paper = vec3(0.958, 0.952, 0.925) + vec3(grain * 0.034);
    float tone = pow(features.luma, 0.72);
    float shade = 1.0 - smoothstep(0.04, 0.94, tone);
    float hatch = sketchHatching(tone, pixel, 0.96);
    float line = clamp(features.edge * 1.04 + features.fineEdge * 0.48 + hatch * 0.66, 0.0, 1.0);

    vec3 graphite = vec3(0.060, 0.063, 0.067);
    vec3 softShade = mix(paper, vec3(0.58, 0.58, 0.56), shade * 0.58);
    softShade *= 1.0 - hatch * 0.20;
    vec3 result = mix(softShade, graphite, clamp(line * 0.82, 0.0, 0.92));
    result += vec3(grain * 0.015);
    return clamp(result, 0.0, 1.0);
}

vec3 applyCartoon(SceneFeatures features) {
    vec3 color = saturateColor(features.average, 1.22);
    color = contrastAround(pow(max(color, vec3(0.0)), vec3(0.92)), 1.16, 0.48);

    float luma = max(luminance(color), 0.001);
    float toonLuma = 0.16;
    toonLuma += 0.18 * step(0.20, luma);
    toonLuma += 0.22 * step(0.40, luma);
    toonLuma += 0.22 * step(0.62, luma);
    toonLuma += 0.18 * step(0.82, luma);

    vec3 cel = color * (toonLuma / luma);
    cel = posterize(clamp(cel, 0.0, 1.0), 5.5);
    cel = clamp(cel * vec3(1.04, 1.02, 1.00) + vec3(0.018), 0.0, 1.0);

    float outline = smoothstep(0.13, 0.34, features.rawEdge);
    float interior = smoothstep(0.050, 0.18, features.rawEdge) * (1.0 - outline);
    vec3 lineColor = mix(vec3(0.025, 0.027, 0.034), cel * 0.28, 0.16);
    vec3 result = mix(cel, lineColor, clamp(outline * 0.88 + interior * 0.22, 0.0, 0.92));
    return clamp(result, 0.0, 1.0);
}

float chromeStripe(float value, float width) {
    float distanceToLine = abs(fract(value) - 0.5);
    return 1.0 - smoothstep(width, width + 0.055, distanceToLine);
}

vec3 applyChrome(SceneFeatures features, vec2 pixel) {
    vec3 normal = normalize(vec3(-features.gx * 2.9, -features.gy * 2.9, 1.0));
    vec3 viewDirection = vec3(0.0, 0.0, 1.0);
    vec3 reflection = reflect(-viewDirection, normal);

    float horizon = clamp(reflection.y * 0.5 + 0.5, 0.0, 1.0);
    float sceneShape = pow(features.luma, 0.76);
    float broadReflection = mix(0.16, 0.82, smoothstep(0.04, 0.96, horizon));
    float darkBand = chromeStripe(reflection.y * 3.8 + sceneShape * 0.42 + texCoord.y * 0.60, 0.18);
    float brightBand = chromeStripe(reflection.y * 7.1 - sceneShape * 0.20 + texCoord.x * 0.22, 0.095);
    float microBand = chromeStripe((pixel.y + reflection.x * 48.0) / 34.0, 0.040);

    vec3 lightA = normalize(vec3(-0.55, 0.58, 0.60));
    vec3 lightB = normalize(vec3(0.62, -0.20, 0.76));
    float specularA = pow(max(dot(normal, lightA), 0.0), 58.0) * 0.82;
    float specularB = pow(max(dot(normal, lightB), 0.0), 30.0) * 0.34;
    float rim = pow(clamp(1.0 - normal.z, 0.0, 1.0), 2.2) * 0.28;

    float metal = broadReflection;
    metal -= darkBand * 0.24;
    metal += brightBand * 0.22 + microBand * 0.045;
    metal += specularA + specularB + rim;
    metal = mix(metal, 1.0 - exp(-metal * 1.08), 0.32);

    float crease = smoothstep(0.16, 0.42, features.rawEdge);
    metal *= 1.0 - crease * 0.48;
    metal += smoothstep(0.55, 0.96, features.luma) * 0.08;
    metal += paperGrain(pixel) * 0.008;
    return vec3(clamp(metal, 0.025, 1.0));
}

void main() {
    vec4 diffuseColor = texture(InSampler, texCoord);
    vec3 center = clamp(diffuseColor.rgb, 0.0, 1.0);
    SceneFeatures features = analyzeScene(center);
    vec2 pixel = texCoord * max(InSize, vec2(1.0));
    int style = int(Stylize.x + 0.5);

    vec3 color = center;
    if (style == 1) {
        color = applyColorSketch(features, pixel);
    } else if (style == 2) {
        color = applyPencilSketch(features, pixel);
    } else if (style == 3) {
        color = applyCartoon(features);
    } else if (style == 4) {
        color = applyChrome(features, pixel);
    }

    fragColor = vec4(clamp(color, 0.0, 1.0), diffuseColor.a);
}

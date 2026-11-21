#version 330

uniform sampler2D InSampler;

layout(std140) uniform ColorVisionConfig {
    vec4 Red;
    vec4 Green;
    vec4 Blue;
    vec4 Redistribution;
};

in vec2 texCoord;
out vec4 fragColor;

vec3 linearRgb(vec3 srgb) {
    return mix(srgb / 12.92, pow((srgb + 0.055) / 1.055, vec3(2.4)), step(0.04045, srgb));
}

vec3 displayRgb(vec3 linear) {
    return mix(linear * 12.92, 1.055 * pow(linear, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, linear));
}

void main() {
    vec4 inputColor = texture(InSampler, texCoord);
    vec3 original = linearRgb(inputColor.rgb);
    vec3 simulated = vec3(dot(Red.xyz, original), dot(Green.xyz, original), dot(Blue.xyz, original));
    vec3 lost = original - simulated;
    float signal = Redistribution.r == 0.0 ? lost.r : (Redistribution.g == 0.0 ? lost.g : lost.b);
    vec3 adjusted = clamp(original + Redistribution.xyz * signal, 0.0, 1.0);
    fragColor = vec4(displayRgb(adjusted), inputColor.a);
}

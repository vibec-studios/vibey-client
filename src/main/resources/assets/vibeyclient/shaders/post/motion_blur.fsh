#version 330

uniform sampler2D InSampler;
uniform sampler2D PreviousSampler;

layout(std140) uniform MotionBlurConfig {
    float HistoryWeight;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 now = texture(InSampler, texCoord).rgb;
    vec3 before = texture(PreviousSampler, texCoord).rgb;
    fragColor = vec4(mix(now, before, HistoryWeight), 1.0);
}

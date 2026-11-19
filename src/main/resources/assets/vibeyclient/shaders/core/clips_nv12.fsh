#version 330

// Clips: the main picture as NV12 (BT.709, limited range), letterboxed into OUT_W x OUT_H, packed into an RGBA8
// target of OUT_W/4 x OUT_H*3/2 texels so that its bytes ARE the NV12 frame: rows 0..OUT_H-1 hold four Y samples per
// texel, the rows below hold U,V,U,V of two chroma samples. Row 0 is the top of the picture (it is read back first).

uniform sampler2D InSampler;

out vec4 fragColor;

vec3 src(vec2 p) {
    vec2 size = vec2(textureSize(InSampler, 0));
    float srcAspect = size.x / size.y;
    float outAspect = float(OUT_W) / float(OUT_H);
    vec2 uv = p;
    if (srcAspect > outAspect) {
        float h = outAspect / srcAspect;
        uv.y = (p.y - (1.0 - h) * 0.5) / h;
    } else {
        float w = srcAspect / outAspect;
        uv.x = (p.x - (1.0 - w) * 0.5) / w;
    }
    if (uv.x < 0.0 || uv.y < 0.0 || uv.x > 1.0 || uv.y > 1.0) return vec3(0.0);
    return texture(InSampler, vec2(uv.x, 1.0 - uv.y)).rgb;
}

float luma(vec3 c) {
    return (16.0 + 219.0 * dot(c, vec3(0.2126, 0.7152, 0.0722))) / 255.0;
}

vec2 chroma(vec3 c) {
    return vec2(128.0 + 224.0 * dot(c, vec3(-0.1146, -0.3854, 0.5)),
                128.0 + 224.0 * dot(c, vec3(0.5, -0.4542, -0.0458))) / 255.0;
}

void main() {
    ivec2 t = ivec2(gl_FragCoord.xy);
    float w = float(OUT_W);
    float h = float(OUT_H);
    float x0 = float(t.x * 4);
    if (t.y < OUT_H) {
        float row = (float(t.y) + 0.5) / h;
        fragColor = vec4(luma(src(vec2((x0 + 0.5) / w, row))), luma(src(vec2((x0 + 1.5) / w, row))),
                         luma(src(vec2((x0 + 2.5) / w, row))), luma(src(vec2((x0 + 3.5) / w, row))));
    } else {
        float row = (float((t.y - OUT_H) * 2) + 1.0) / h;
        fragColor = vec4(chroma(src(vec2((x0 + 1.0) / w, row))), chroma(src(vec2((x0 + 3.0) / w, row))));
    }
}

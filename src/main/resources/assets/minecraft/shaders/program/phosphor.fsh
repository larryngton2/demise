#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;

uniform float Weight = 0.5;

void main() {
    vec4 CurrTexel = texture2D(DiffuseSampler, texCoord);
    vec4 PrevTexel = texture2D(PrevSampler, texCoord);

    gl_FragColor = vec4(mix(PrevTexel.rgb, CurrTexel.rgb, Weight), 1.0);
}
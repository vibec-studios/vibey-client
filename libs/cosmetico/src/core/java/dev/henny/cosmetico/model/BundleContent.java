package dev.henny.cosmetico.model;

import java.util.Map;

public record BundleContent(Texture[] textures, Model model, Map<String, Animation> animations, Sprite[] sprites, Sound[] sounds) {}

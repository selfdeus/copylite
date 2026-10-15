package com.goldhider.copycats.client.assembly;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Sustituye a SpriteShiftEntry de Catnip: conversión entre UV de atlas y fracción 0..1 del sprite. */
public final class SpriteUtil {
    public static float getUnInterpolatedU(TextureAtlasSprite sprite, float u) {
        float f = sprite.getU1() - sprite.getU0();
        return (u - sprite.getU0()) / f;
    }

    public static float getUnInterpolatedV(TextureAtlasSprite sprite, float v) {
        float f = sprite.getV1() - sprite.getV0();
        return (v - sprite.getV0()) / f;
    }

    private SpriteUtil() {
    }
}

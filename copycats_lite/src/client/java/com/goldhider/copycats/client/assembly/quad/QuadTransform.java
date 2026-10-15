package com.goldhider.copycats.client.assembly.quad;

import com.goldhider.copycats.client.assembly.MutableQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Transforms a quad by mutating its vertices or other properties.
 */
public interface QuadTransform {
    /**
     * Transforms the quad by mutating its vertices or other properties.
     * <p>
     * Return false to discard the quad.
     */
    boolean transformQuad(MutableQuad quad, TextureAtlasSprite sprite);
}

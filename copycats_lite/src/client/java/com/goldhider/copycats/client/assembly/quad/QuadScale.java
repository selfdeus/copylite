package com.goldhider.copycats.client.assembly.quad;

import com.goldhider.copycats.client.assembly.MutableQuad;
import com.goldhider.copycats.client.assembly.MutableVec3;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Scales a quad around a pivot point.
 *
 * @param pivot The pivot point to scale around, in block space.
 * @param scale The scale to apply, where (1, 1, 1) is the original size.
 */
public record QuadScale(MutableVec3.AsPivot pivot, MutableVec3.AsScale scale) implements QuadTransform {

    @Override
    public boolean transformQuad(MutableQuad quad, TextureAtlasSprite sprite) {
        for (int i = 0; i < 4; i++) {
            quad.vertices.get(i).xyz.subtract(this.pivot).multiply(this.scale).add(this.pivot);
        }
        return true;
    }
}

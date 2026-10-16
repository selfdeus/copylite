package com.goldhider.copycats.client.model;

import com.goldhider.copycats.client.assembly.*;
import com.goldhider.copycats.client.assembly.quad.QuadAutoCull;
import com.goldhider.copycats.client.assembly.quad.QuadTransform;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del contexto de ensamblado para la Renderer API de Fabric (26.3).
 * <p>
 * La fuente es una malla con los quads del modelo copiado (un cubo completo) y el destino es el emisor real.
 * Reemplaza a CopycatRenderContextFabric del original, que dependía de Create.
 */
public class MeshRenderContext extends CopycatRenderContext.Base<MeshView, QuadEmitter> {
    private static final QuadTransform[] NO_TRANSFORMS = new QuadTransform[0];

    public MeshRenderContext(MeshView source, QuadEmitter destination, String key) {
        super(source, destination, key);
    }

    private static SpriteFinder spriteFinder(QuadView quad) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(quad.atlas().getId()).spriteFinder();
    }

    @Override
    public void assemblePiece(AssemblyTransform assemblyTransform, MutableVec3 offset, MutableAABB select, MutableCullFace cull) {
        assemblePiece(assemblyTransform, offset, select, cull, NO_TRANSFORMS);
    }

    @Override
    public void assemblePiece(AssemblyTransform assemblyTransform, MutableVec3 offset, MutableAABB select, MutableCullFace cull, QuadTransform... transforms) {
        assemblyTransform.apply(select);
        assemblyTransform.apply(offset);
        assemblyTransform.apply(cull);
        AABB aabb = select.toAABB();
        Vec3 move = offset.toVec3().subtract(select.minX, select.minY, select.minZ);
        source().forEach(quad -> {
            if (cull.isCulled(quad.lightFace())) {
                return;
            }
            assembleQuad(quad, destination(), aabb, move, assemblyTransform, transforms);
        });
    }

    @Override
    public void assembleAll() {
        source().forEach(quad -> {
            destination().copyFrom(quad);
            destination().emit();
        });
    }

    @Override
    public void assembleRaw(AABB crop, Vec3 move) {
        assembleRaw(crop, move, NO_TRANSFORMS);
    }

    @Override
    public void assembleRaw(AABB crop, Vec3 move, QuadTransform... transforms) {
        source().forEach(quad -> assembleQuad(quad, destination(), crop, move, AssemblyTransform.IDENTITY, transforms));
    }

    private static void assembleQuad(QuadView src, QuadEmitter dest, AABB crop, Vec3 move, AssemblyTransform assemblyTransform, QuadTransform... transforms) {
        TextureAtlasSprite sprite = spriteFinder(src).find(src);

        dest.copyFrom(src);
        if (!CropUtil.cropAndMove(dest, crop, move)) {
            return;
        }

        MutableQuad mutableQuad = getMutableQuad(dest);
        assemblyTransform.apply(mutableQuad);
        mutableQuad.undoMutate();
        for (QuadTransform transform : transforms) {
            if (!transform.transformQuad(mutableQuad, sprite)) {
                return;
            }
        }
        if (!mutableQuad.disableFinalAutoCull) {
            if (!QuadAutoCull.BLOCK.transformQuad(mutableQuad, sprite)) {
                return;
            }
        }
        mutableQuad.mutate();

        for (int i = 0; i < 4; i++) {
            MutableVertex vertex = mutableQuad.vertices.get(i);
            dest.pos(i, (float) vertex.xyz.x, (float) vertex.xyz.y, (float) vertex.xyz.z);
            dest.uv(i, vertex.uv.u, vertex.uv.v);
        }
        dest.nominalFace(mutableQuad.computeLightFace());
        dest.cullFace(mutableQuad.cullFace);
        dest.emit();
    }

    private static MutableQuad getMutableQuad(QuadView quad) {
        List<MutableVertex> vertices = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            MutableVec3 xyz = new MutableVec3(quad.x(i), quad.y(i), quad.z(i));
            MutableUV uv = new MutableUV(quad.u(i), quad.v(i));
            vertices.add(new MutableVertex(xyz, uv));
        }
        return new MutableQuad(vertices, quad.lightFace());
    }
}

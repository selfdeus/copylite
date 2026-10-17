package com.goldhider.copycats.client.model.core;

import com.goldhider.copycats.block.CopycatLayerBlock;
import com.goldhider.copycats.client.assembly.AssemblyTransform;
import com.goldhider.copycats.client.assembly.CopycatRenderContext;
import com.goldhider.copycats.client.model.CopycatModelCore;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

import static com.goldhider.copycats.client.assembly.CopycatRenderContext.*;
import static com.goldhider.copycats.client.assembly.MutableCullFace.*;

/**
 * Capa de 1 a 8 px*2 de grosor. Igual que la losa, se arma con dos piezas tomadas de los extremos del cubo copiado
 * (cada una de la mitad del grosor), así la textura de los laterales se ve como una rebanada fina del bloque original.
 * Con 8 capas son dos piezas de 8 px: el cubo completo.
 */
public class CopycatLayerModelCore extends CopycatModelCore {

    @Override
    public void emitCopycatQuads(String key, BlockState state, CopycatRenderContext context, BlockState material) {
        Direction facing = state.getValue(CopycatLayerBlock.FACING);
        double half = state.getValue(CopycatLayerBlock.LAYERS); // grosor total = 2 px por capa; cada pieza = la mitad

        if (facing.getAxis().isHorizontal()) {
            AssemblyTransform transform = t -> t.rotateY((int) facing.toYRot());
            context.assemblePiece(
                    transform,
                    vec3(0, 0, 0),
                    aabb(16, 16, half),
                    cull(SOUTH)
            );
            context.assemblePiece(
                    transform,
                    vec3(0, 0, half),
                    aabb(16, 16, half).move(0, 0, 16 - half),
                    cull(NORTH)
            );
        } else {
            AssemblyTransform transform = t -> t.flipY(facing.getAxisDirection() == AxisDirection.NEGATIVE);
            context.assemblePiece(
                    transform,
                    vec3(0, 0, 0),
                    aabb(16, half, 16),
                    cull(UP)
            );
            context.assemblePiece(
                    transform,
                    vec3(0, half, 0),
                    aabb(16, half, 16).move(0, 16 - half, 0),
                    cull(DOWN)
            );
        }
    }
}

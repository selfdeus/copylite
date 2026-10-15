package com.goldhider.copycats.client.model.core;

import com.goldhider.copycats.block.CopycatSlabBlock;
import com.goldhider.copycats.client.assembly.AssemblyTransform;
import com.goldhider.copycats.client.assembly.CopycatRenderContext;
import com.goldhider.copycats.client.model.CopycatModelCore;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import static com.goldhider.copycats.client.assembly.CopycatRenderContext.*;
import static com.goldhider.copycats.client.assembly.MutableCullFace.*;

/**
 * Losa de un solo material. Cada mitad se arma con dos piezas de 4 px tomadas de los extremos del cubo copiado,
 * así la textura de los laterales se ve como una rebanada fina del bloque original.
 * Lógica de piezas basada en CopycatMultiSlabModelCore de Copycats+.
 */
public class CopycatSlabModelCore extends CopycatModelCore {

    @Override
    public void emitCopycatQuads(String key, BlockState state, CopycatRenderContext context, BlockState material) {
        Axis axis = state.getValue(CopycatSlabBlock.AXIS);
        SlabType type = state.getValue(CopycatSlabBlock.SLAB_TYPE);

        // Losa doble: cubo completo, sin costura ni bordes repetidos en medio.
        if (type == SlabType.DOUBLE) {
            context.assembleAll();
            return;
        }

        // BOTTOM = lado negativo del eje (POSITIVE en este sistema de ensamblado), TOP = lado positivo.
        if (type != SlabType.TOP) {
            assembleHalf(context, Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE));
        }
        if (type != SlabType.BOTTOM) {
            assembleHalf(context, Direction.fromAxisAndDirection(axis, AxisDirection.NEGATIVE));
        }
    }

    private static void assembleHalf(CopycatRenderContext context, Direction facing) {
        if (facing.getAxis().isHorizontal()) {
            AssemblyTransform transform = t -> t.rotateY((int) facing.toYRot());
            context.assemblePiece(
                    transform,
                    vec3(0, 0, 0),
                    aabb(16, 16, 4),
                    cull(SOUTH)
            );
            context.assemblePiece(
                    transform,
                    vec3(0, 0, 4),
                    aabb(16, 16, 4).move(0, 0, 12),
                    cull(NORTH)
            );
        } else {
            AssemblyTransform transform = t -> t.flipY(facing.getAxisDirection() == AxisDirection.NEGATIVE);
            context.assemblePiece(
                    transform,
                    vec3(0, 0, 0),
                    aabb(16, 4, 16),
                    cull(UP)
            );
            context.assemblePiece(
                    transform,
                    vec3(0, 4, 0),
                    aabb(16, 4, 16).move(0, 12, 0),
                    cull(DOWN)
            );
        }
    }
}

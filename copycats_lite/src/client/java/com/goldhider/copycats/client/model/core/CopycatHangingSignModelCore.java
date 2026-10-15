package com.goldhider.copycats.client.model.core;

import com.goldhider.copycats.client.model.BoxBuilder;
import com.goldhider.copycats.client.model.CopycatBoxCore;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Letrero colgante de techo: tabla de 14x10x2 px con el material copiado y, arriba, dos cadenas (cada una son dos
 * planos cruzados) o, si está "attached", una placa plana. Gira en pasos de 22.5 grados según ROTATION_16.
 * El texto lo dibuja el renderizador de la block entity.
 */
public class CopycatHangingSignModelCore extends CopycatBoxCore {

    @Override
    public void emitBoxes(BlockState state, BoxBuilder boxes) {
        int rotation = state.getValue(BlockStateProperties.ROTATION_16);
        boolean attached = state.getValue(BlockStateProperties.ATTACHED);

        // Mismo giro que aplica el renderizador de vanilla: -22.5 grados por paso.
        boxes.yaw(-rotation * 22.5);

        boxes.box(1, 0, 7, 15, 10, 9);

        if (attached) {
            boxes.box(2, 10, 7.5, 14, 16, 8.5);
        } else {
            for (double cx : new double[]{3, 13}) {
                boxes.chainPlane(cx, 10, 16, 45, 0);
                boxes.chainPlane(cx, 10, 16, -45, 1);
            }
        }
    }
}

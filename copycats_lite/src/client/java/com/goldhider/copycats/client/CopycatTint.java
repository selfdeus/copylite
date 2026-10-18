package com.goldhider.copycats.client;

import com.goldhider.copycats.CCRegistry;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tinte de bioma para los copycats: el color de cada capa de tinte se calcula con las fuentes de tinte
 * del bloque copiado (césped, hojas, agua...), en la posición del copycat.
 * Los quads copiados conservan su índice de tinte, que apunta a la misma posición de esta lista.
 */
public final class CopycatTint {

	public static void register() {
		BlockColorRegistry.register(CopycatTint::collect,
				CCRegistry.COPYCAT_BLOCK, CCRegistry.COPYCAT_SLAB, CCRegistry.COPYCAT_STAIRS, CCRegistry.COPYCAT_LAYER,
				CCRegistry.COPYCAT_FENCE, CCRegistry.COPYCAT_WALL, CCRegistry.COPYCAT_BEAM, CCRegistry.COPYCAT_HANGING_SIGN,
				CCRegistry.COPYCAT_SHELF);
	}

	private static void collect(BlockState state, BlockAndTintGetter level, BlockPos pos, IntList tintValues) {
		if (!(level instanceof FabricBlockGetter getter)
				|| !(getter.getBlockEntityRenderData(pos) instanceof BlockState material)) {
			return;
		}
		for (BlockTintSource source : Minecraft.getInstance().getBlockColors().getTintSources(material)) {
			tintValues.add(source.colorInWorld(material, level, pos));
		}
	}

	private CopycatTint() {
	}
}

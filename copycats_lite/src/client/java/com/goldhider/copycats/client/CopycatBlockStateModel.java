package com.goldhider.copycats.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Modelo del copycat: si hay material, emite los quads del modelo del material;
 * si no, emite el modelo base (textura "vacía").
 *
 * Basado en FrameBlockStateModel del testmod de Fabric API 0.162.0+26.3.
 *
 * Limitación conocida de este primer corte: el material no recibe tinte de bioma (césped, hojas)
 * y solo se copia el cubo completo (las formas llegan en el siguiente paso).
 */
public class CopycatBlockStateModel implements BlockStateModel {
	private final BlockStateModel baseModel;

	public CopycatBlockStateModel(BlockStateModel baseModel) {
		this.baseModel = baseModel;
	}

	/** Lee el material desde los datos de render (inmutables) de la block entity. */
	private static @Nullable BlockState getMaterial(BlockAndTintGetter level, BlockPos pos) {
		if (((FabricBlockGetter) level).getBlockEntityRenderData(pos) instanceof BlockState material) {
			return material;
		}
		return null;
	}

	private static BlockStateModel modelOf(BlockState state) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		BlockState material = getMaterial(level, pos);
		if (material == null) {
			baseModel.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}
		modelOf(material).emitQuads(emitter, level, pos, material, random, cullTest);
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return this;
		}

		Object subkey = modelOf(material).createGeometryKey(level, pos, material, random);
		if (subkey == null) {
			return null;
		}

		record Key(Object subkey) {
		}

		return new Key(subkey);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return baseModel.particleMaterial(level, pos, state);
		}
		return modelOf(material).particleMaterial(level, pos, material);
	}

	@Override
	@BakedQuad.MaterialFlags
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		@BakedQuad.MaterialFlags int flags = baseModel.materialFlags(level, pos, state, random);

		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return flags;
		}
		return flags | modelOf(material).materialFlags(level, pos, material, random);
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
		// La Renderer API de Fabric hace esto innecesario: no se añaden partes.
	}

	@Override
	public Material.Baked particleMaterial() {
		return baseModel.particleMaterial();
	}

	@Override
	public @BakedQuad.MaterialFlags int materialFlags() {
		// Puede renderizar cualquier submodelo (animado o translúcido), así que se declara el caso general.
		return BakedQuad.FLAG_ANIMATED | BakedQuad.FLAG_TRANSLUCENT;
	}

	public record Unbaked(BlockStateModel.Unbaked baseModel) implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				BlockStateModel.Unbaked.CODEC.fieldOf("base_model").forGetter(Unbaked::baseModel)
		).apply(instance, Unbaked::new));

		@Override
		public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
			return CODEC;
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			baseModel.resolveDependencies(resolver);
		}

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			return new CopycatBlockStateModel(baseModel.bake(baker));
		}
	}
}

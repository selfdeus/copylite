package com.goldhider.copycats;

import com.goldhider.copycats.block.CopycatBlock;
import com.goldhider.copycats.block.CopycatBlockEntity;
import com.goldhider.copycats.block.CopycatLayerBlock;
import com.goldhider.copycats.block.CopycatSlabBlock;
import com.goldhider.copycats.block.CopycatStairsBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Registro de bloques, items y block entities.
 * Sigue el mismo patrón que el testmod de Fabric API (fabric-renderer-api-v1) para 26.3.
 */
public final class CCRegistry {
	public static final CopycatBlock COPYCAT_BLOCK = register("copycat_block", CopycatBlock::new, props());
	public static final CopycatSlabBlock COPYCAT_SLAB = register("copycat_slab", CopycatSlabBlock::new, props());
	public static final CopycatStairsBlock COPYCAT_STAIRS = register("copycat_stairs", CopycatStairsBlock::new, props());
	public static final CopycatLayerBlock COPYCAT_LAYER = register("copycat_layer", CopycatLayerBlock::new, props());

	public static final BlockItem COPYCAT_ITEM = registerBlockItem("copycat_block", COPYCAT_BLOCK);
	public static final BlockItem COPYCAT_SLAB_ITEM = registerBlockItem("copycat_slab", COPYCAT_SLAB);
	public static final BlockItem COPYCAT_STAIRS_ITEM = registerBlockItem("copycat_stairs", COPYCAT_STAIRS);
	public static final BlockItem COPYCAT_LAYER_ITEM = registerBlockItem("copycat_layer", COPYCAT_LAYER);

	/** Un único tipo de block entity compartido por todos los copycats. */
	public static final BlockEntityType<CopycatBlockEntity> COPYCAT_BE_TYPE = register("copycat",
			FabricBlockEntityTypeBuilder.create(CopycatBlockEntity::new, COPYCAT_BLOCK, COPYCAT_SLAB, COPYCAT_STAIRS, COPYCAT_LAYER).build());

	private static BlockBehaviour.Properties props() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).strength(1.0F);
	}

	private static <T extends Block> T register(String path, Function<BlockBehaviour.Properties, T> constructor, BlockBehaviour.Properties settings) {
		Identifier id = CopycatsLite.id(path);
		return Registry.register(BuiltInRegistries.BLOCK, id, constructor.apply(settings.setId(ResourceKey.create(Registries.BLOCK, id))));
	}

	private static BlockItem registerBlockItem(String path, Block block) {
		return registerItem(path, settings -> new BlockItem(block, settings));
	}

	private static <T extends Item> T registerItem(String path, Function<Item.Properties, T> itemFunction) {
		ResourceKey<Item> resourceKey = ResourceKey.create(Registries.ITEM, CopycatsLite.id(path));
		return Registry.register(BuiltInRegistries.ITEM, resourceKey, itemFunction.apply(new Item.Properties().setId(resourceKey)));
	}

	private static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CopycatsLite.id(path), blockEntityType);
	}

	public static void init() {
	}

	private CCRegistry() {
	}
}

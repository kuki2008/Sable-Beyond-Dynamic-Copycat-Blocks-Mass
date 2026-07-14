package com.kuki.sabledyncats.massMultipliers;

import java.util.Set;

import com.copycatsplus.copycats.content.copycat.board.CopycatBoardBlock;
import com.copycatsplus.copycats.content.copycat.byte_panel.CopycatBytePanelBlock;
import com.copycatsplus.copycats.content.copycat.bytes.CopycatByteBlock;

import com.kuki.sabledyncats.massMultipliers.differents.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class CopycatsDefinitions {

    private static final String MODID = "copycats";

    public static void init() {

        Set<Block> special = Set.of(
                get("copycat_board"),
                get("copycat_byte"),
                get("copycat_byte_panel"),
                get("copycat_cogwheel"),
                get("copycat_large_cogwheel"),
                get("copycat_half_layer"),
                get("copycat_stacked_half_layer"),
                get("copycat_vertical_half_layer"),
                get("copycat_layer"),
                get("copycat_slice"),
                get("copycat_slab"),
                get("copycat_vertical_slice"),
                get("copycat_corner_slice"),
                get("copycat_slope_layer")
        );


        for (Block block : BuiltInRegistries.BLOCK) {

            ResourceLocation id =
                    BuiltInRegistries.BLOCK.getKey(block);

            if (!MODID.equals(id.getNamespace()))
                continue;

            if (!id.getPath().startsWith("copycat_"))
                continue;

            if (special.contains(block))
                continue;

            registerCopycatSerializer(block);
        }


        registerSpecialCopycats();
    }

    private static void registerSpecialCopycats() {

        registerCopycatSerializer(
                get("copycat_board"),
                BinaryCopycatMassProperties.of(
                        CopycatBoardBlock.UP,
                        CopycatBoardBlock.DOWN,
                        CopycatBoardBlock.NORTH,
                        CopycatBoardBlock.EAST,
                        CopycatBoardBlock.SOUTH,
                        CopycatBoardBlock.WEST
                )
        );

        registerCopycatSerializer(
                get("copycat_byte"),
                BinaryCopycatMassProperties.of(
                        CopycatByteBlock.TOP_NE,
                        CopycatByteBlock.TOP_SE,
                        CopycatByteBlock.TOP_SW,
                        CopycatByteBlock.TOP_NW,
                        CopycatByteBlock.BOTTOM_NE,
                        CopycatByteBlock.BOTTOM_SE,
                        CopycatByteBlock.BOTTOM_SW,
                        CopycatByteBlock.BOTTOM_NW
                )
        );

        registerCopycatSerializer(
                get("copycat_byte_panel"),
                BinaryCopycatMassProperties.of(
                        CopycatBytePanelBlock.TOP_LEFT,
                        CopycatBytePanelBlock.TOP_RIGHT,
                        CopycatBytePanelBlock.BOTTOM_RIGHT,
                        CopycatBytePanelBlock.BOTTOM_LEFT
                )
        );

        registerCopycatSerializer(
                get("copycat_cogwheel"),
                CopycatCogwheelMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_large_cogwheel"),
                CopycatCogwheelMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_half_layer"),
                CopycatHalfLayeredMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_stacked_half_layer"),
                CopycatHalfLayeredMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_vertical_half_layer"),
                CopycatHalfLayeredMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_layer"),
                CopycatLayeredMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_slice"),
                CopycatLayeredMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_vertical_slice"),
                CopycatLayeredMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_corner_slice"),
                CopycatLayeredMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_slab"),
                CopycatSlabMassProperties::new
        );

        registerCopycatSerializer(
                get("copycat_slope_layer"),
                CopycatLayeredMassProperties::new
        );
    }


    private static Block get(String path) {
        return BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(MODID, path)
        );
    }


    private static void registerCopycatSerializer(
            Block block
    ) {
        registerCopycatSerializer(
                block,
                CopycatBlockMassProperties::new
        );
    }


    private static void registerCopycatSerializer(
            Block block,
            AbstractMimiBlockMassProperties.Factory<?> factory
    ) {
        if (block != null && block != Blocks.AIR) {
            BlockMassPropertiesHandler.registerCustomSerializer(
                    block,
                    AbstractMimiBlockMassProperties.createMimiSerializer(factory)
            );
        }
    }
}
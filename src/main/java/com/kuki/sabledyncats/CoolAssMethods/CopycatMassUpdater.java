package com.kuki.sabledyncats.CoolAssMethods;

import com.copycatsplus.copycats.foundation.copycat.multistate.IMultiStateCopycatBlockEntity;
import com.kuki.sabledyncats.Config;
import com.kuki.sabledyncats.SableDynoCats;
import com.kuki.sabledyncats.massMultipliers.AbstractMimiBlockMassProperties;
import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesHandler;
import com.kuki.sabledyncats.massMultipliers.BlockMassPropertiesProvider;
import com.kuki.sabledyncats.massMultipliers.CopycatElementCounter;
import com.kuki.sabledyncats.massMultipliers.differents.CopycatLayeredMassProperties;
import com.kuki.sabledyncats.massMultipliers.differents.MultiStateCopycatMassProperties;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;

public final class CopycatMassUpdater {

    private static final ResourceLocation COPYCAT_BASE_ID =
            ResourceLocation.parse("create:copycat_base");

    private static final Block COPYCAT_BASE =
            BuiltInRegistries.BLOCK.get(COPYCAT_BASE_ID);

    public static void update(BlockEntity be, BlockState material) {

        Debugger.info("========== COPYCAT MASS UPDATE ==========");

        Level level = be.getLevel();

        if (level == null || level.isClientSide()) {
            Debugger.info("Skipped (client/null level)");
            return;
        }

        BlockState state = be.getBlockState();
        BlockPos pos = be.getBlockPos();

        Debugger.info("Block      : {}", BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        Debugger.info("State      : {}", state);
        Debugger.info("MaterialArg: {}", material);

        BlockMassPropertiesProvider provider =
                BlockMassPropertiesHandler.getProperties(state);

        Debugger.info("Provider   : {}", provider.getClass().getSimpleName());

        double multiplier = Config.enableMassMultipliers
                ? provider.massMultiplier(level, state, pos, true)
                : 1;

        Debugger.info("Multiplier : {}", multiplier);

        double totalMass = 0;

        if (provider instanceof CopycatElementCounter ccount) {
            int elementCount = ccount.getElementCount(level, state, pos);
            Debugger.info("ElementCount = {}", elementCount);
            int remaining = elementCount;
            if (be instanceof IMultiStateCopycatBlockEntity msbe) {

                Map<String, BlockState> materials =
                        msbe.getMaterialItemStorage().getMaterialMap();

                Debugger.info("MaterialMap size = {}", materials.size());

                int i = 0;

                for (Map.Entry<String, BlockState> entry : materials.entrySet()) {

                    if (remaining <= 0)
                        break;

                    BlockState part = entry.getValue();

                    Debugger.info(
                            "#{} key={} block={}",
                            i++,
                            entry.getKey(),
                            BuiltInRegistries.BLOCK.getKey(part.getBlock())
                    );

                    if (part == null || part.isAir()) {
                        Debugger.info(" -> skipped (air)");
                        continue;
                    }

                    if (part.getBlock() != COPYCAT_BASE) {

                        double partMass = getMass(part);
                        double added = partMass * multiplier;

                        totalMass += added;

                        Debugger.info(
                                " -> {} * {} = {} (total {})",
                                partMass,
                                multiplier,
                                added,
                                totalMass
                        );
                        remaining--;
                    }
                }
            } else {
                if (material != null
                        && !material.isAir()
                        && material.getBlock() != COPYCAT_BASE) {
                    totalMass += getMass(material) * multiplier * remaining;
                    Debugger.info(
                            " -> {} * {} * {} = {} (total {})",
                            getMass(material),
                            multiplier,
                            remaining,
                            getMass(material) * multiplier * remaining,
                            totalMass
                    );
                }
            }
            if (Config.includeCopycatMass) {

                double mass = getMass(state);

                totalMass += mass * multiplier * remaining;

                Debugger.info(
                        " -> copycat mass {} * {} * {} = {} (total {})",
                        remaining,
                        mass,
                        multiplier,
                        mass * multiplier * remaining,
                        totalMass
                );

            } else {
                Debugger.info(" -> ignored copycat mass");
            }
        } else {

            Debugger.info("Simple provider");

            if (material != null
                    && !material.isAir()
                    && material.getBlock() != COPYCAT_BASE) {

                double added = getMass(material) * multiplier;

                totalMass += added;

                Debugger.info(
                        "Material mass {} * {} = {}",
                        getMass(material),
                        multiplier,
                        added
                );

            }
            if (Config.includeCopycatMass) {

                double added = getMass(state) * multiplier;

                totalMass += added;

                Debugger.info(
                        "Copycat mass {}",
                        added
                );
            }
        }

        Debugger.info("FINAL MASS = {}", totalMass);
        Debugger.info("=========================================");

        DynamicMass.setBlockMass(level, pos, totalMass);
    }

    private static double getMass(BlockState state) {
        return ((BlockStateExtension) state)
                .sable$getProperty(
                        PhysicsBlockPropertyTypes.MASS.get()
                );
    }
}
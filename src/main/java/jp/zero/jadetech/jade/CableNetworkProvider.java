package jp.zero.jadetech.jade;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.common.blockentity.CableBlockEntity;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.utils.GTUtil;
import jp.zero.jadetech.JadeTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server side of the cable network summary.
 *
 * <p>GregTech's own Jade provider already shows a single cable's voltage
 * and amperage, so this does not repeat it. What no display shows is the
 * network as a whole: how many cables it spans, its highest voltage tier,
 * which machines hang off it, and how much EU its battery buffers hold.
 * This walks the cable network (same breadth-first pattern as the pipe
 * totals) and collects exactly that.</p>
 *
 * <p>An endpoint is a GregTech machine on a face the cable is connected
 * to, deduplicated by position so a machine touching several cables is
 * only counted once.</p>
 */
public enum CableNetworkProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    /** Safety cap so a pathological network can never stall the server. */
    private static final int MAX_CABLES = 4096;
    /** Max endpoint lines, so a huge base cannot flood the tooltip. */
    private static final int MAX_ENDPOINTS = 6;

    public static final String COUNT_KEY = "cable_net_count";
    public static final String MAX_TIER_KEY = "cable_net_max_tier";
    public static final String ENDPOINTS_KEY = "cable_endpoints";
    public static final String SUPPLY_KEY = "cable_supply";
    public static final String MAX_SUPPLY_AMPS_KEY = "cable_max_supply_amps";
    public static final String MAX_SUPPLY_TIER_KEY = "cable_max_supply_tier";
    public static final String MIN_CABLE_AMPS_KEY = "cable_min_amps";
    public static final String BATTERY_EU_KEY = "cable_battery_eu";
    public static final String BATTERY_CAP_KEY = "cable_battery_cap";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof CableBlockEntity start)) {
            return;
        }
        Level level = accessor.getLevel();
        if (level == null) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<CableBlockEntity> queue = new ArrayDeque<>();
        visited.add(start.getBlockPos());
        queue.add(start);

        Set<BlockPos> endpointPositions = new HashSet<>();
        Map<Block, Integer> endpoints = new HashMap<>();
        ListTag supply = new ListTag();
        Map<Long, Long> potentialAmpsByVoltage = new HashMap<>();
        long maxVoltage = 0;
        long minCableAmps = Long.MAX_VALUE;
        long batteryEu = 0;
        long batteryCap = 0;
        int cableCount = 0;

        while (!queue.isEmpty() && cableCount < MAX_CABLES) {
            CableBlockEntity cable = queue.poll();
            cableCount++;
            maxVoltage = Math.max(maxVoltage, cable.getMaxVoltage());
            minCableAmps = Math.min(minCableAmps, cable.getMaxAmperage());

            for (Direction dir : Direction.values()) {
                if (!cable.isConnected(dir)) {
                    continue;
                }
                BlockPos nextPos = cable.getBlockPos().relative(dir);
                BlockEntity neighbour = level.getBlockEntity(nextPos);
                if (neighbour instanceof CableBlockEntity next) {
                    if (visited.add(nextPos) && next.isConnected(dir.getOpposite())) {
                        queue.add(next);
                    }
                } else if (neighbour instanceof MetaMachine machine && endpointPositions.add(nextPos)) {
                    Block block = machine.getBlockState().getBlock();
                    endpoints.merge(block, 1, Integer::sum);
                    if (machine instanceof BatteryBufferMachine buffer) {
                        batteryEu += buffer.getEnergyStored();
                        batteryCap += buffer.getEnergyCapacity();
                    }
                    // Output side of this machine. Generator containers
                    // are recipe-driven: getOutputAmperage() returns the
                    // amperage of the recipe the machine has (last) run,
                    // or its rated output when it never ran one. Summed
                    // per voltage, that is the maximum the connected
                    // sources could push into this network as it is
                    // currently built - the number the flow warning uses.
                    long outAmps = 0;
                    long outVoltage = 0;
                    for (IEnergyContainer container
                            : machine.getTraitHolder().getTraitsByInterface(IEnergyContainer.class)) {
                        if (container.getOutputAmperage() > 0) {
                            outAmps += container.getOutputAmperage();
                            outVoltage = Math.max(outVoltage, container.getOutputVoltage());
                            potentialAmpsByVoltage.merge(container.getOutputVoltage(),
                                    container.getOutputAmperage(), Long::sum);
                        }
                    }
                    // The supply list is stricter: it shows what is being
                    // produced right now, so a workable machine must be
                    // actively running, and a battery buffer must hold
                    // energy to give.
                    RecipeLogic recipeLogic = machine.getTrait(RecipeLogic.class);
                    boolean producing = recipeLogic == null || recipeLogic.isActive();
                    if (machine instanceof BatteryBufferMachine buffer) {
                        producing = buffer.getEnergyStored() > 0;
                    }
                    if (outAmps > 0 && producing) {
                        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
                        if (blockId != null) {
                            CompoundTag tag = new CompoundTag();
                            tag.putString("block", blockId.toString());
                            tag.putLong("amps", outAmps);
                            tag.putString("tier", GTValues.VNF[GTUtil.getTierByVoltage(outVoltage)]);
                            supply.add(tag);
                        }
                    }
                }
            }
        }

        if (cableCount <= 1 && endpoints.isEmpty()) {
            return;
        }

        data.putInt(COUNT_KEY, cableCount);
        data.putString(MAX_TIER_KEY, GTValues.VNF[GTUtil.getTierByVoltage(maxVoltage)]);

        if (!endpoints.isEmpty()) {
            ListTag list = new ListTag();
            endpoints.entrySet().stream()
                    .sorted(Map.Entry.<Block, Integer>comparingByValue().reversed())
                    .limit(MAX_ENDPOINTS)
                    .forEach(entry -> {
                        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(entry.getKey());
                        if (blockId == null) {
                            return;
                        }
                        CompoundTag tag = new CompoundTag();
                        tag.putString("block", blockId.toString());
                        tag.putInt("count", entry.getValue());
                        list.add(tag);
                    });
            if (!list.isEmpty()) {
                data.put(ENDPOINTS_KEY, list);
            }
        }

        if (!supply.isEmpty()) {
            data.put(SUPPLY_KEY, supply);
        }

        if (!potentialAmpsByVoltage.isEmpty() && minCableAmps != Long.MAX_VALUE) {
            // The largest same-voltage source group decides the worst
            // case: amperage only adds up within one voltage.
            Map.Entry<Long, Long> worst = null;
            for (Map.Entry<Long, Long> entry : potentialAmpsByVoltage.entrySet()) {
                if (worst == null || entry.getValue() > worst.getValue()
                        || (entry.getValue().equals(worst.getValue()) && entry.getKey() > worst.getKey())) {
                    worst = entry;
                }
            }
            data.putLong(MAX_SUPPLY_AMPS_KEY, worst.getValue());
            data.putString(MAX_SUPPLY_TIER_KEY, GTValues.VNF[GTUtil.getTierByVoltage(worst.getKey())]);
            data.putLong(MIN_CABLE_AMPS_KEY, minCableAmps);
        }

        if (batteryCap > 0) {
            data.putLong(BATTERY_EU_KEY, batteryEu);
            data.putLong(BATTERY_CAP_KEY, batteryCap);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return JadeTech.id("cable_network");
    }
}

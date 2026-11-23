package jp.zero.pipetotals.jade;

import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.common.blockentity.FluidPipeBlockEntity;
import jp.zero.pipetotals.PipeTotals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server side of the network totals display.
 *
 * <p>Walks the fluid pipe network connected to the pipe being looked at
 * (breadth-first over neighbouring FluidPipeBlockEntities, following each
 * pipe's connection state so wrench-disconnected neighbours are not
 * crossed), sums every visited pipe's tanks per fluid, and stores the
 * result in the Jade server data for {@link PipeTotalComponent} to draw.</p>
 *
 * <p>Fluids are summed by fluid type only; NBT variants are merged.</p>
 */
public enum PipeNetworkTotalsProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    /** Safety cap so a pathological network can never stall the server. */
    private static final int MAX_PIPES = 4096;

    public static final String DATA_KEY = "network_fluids";
    public static final String PIPE_COUNT_KEY = "network_pipes";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof FluidPipeBlockEntity start)) {
            return;
        }
        Level level = accessor.getLevel();
        if (level == null) {
            return;
        }

        Map<Fluid, Long> totals = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<FluidPipeBlockEntity> queue = new ArrayDeque<>();
        visited.add(start.getBlockPos());
        queue.add(start);

        int pipeCount = 0;
        while (!queue.isEmpty() && pipeCount < MAX_PIPES) {
            FluidPipeBlockEntity pipe = queue.poll();
            pipeCount++;

            for (CustomFluidTank tank : pipe.getFluidTanks()) {
                if (tank.getFluidAmount() > 0 && !tank.getFluid().isEmpty()) {
                    totals.merge(tank.getFluid().getFluid(), (long) tank.getFluidAmount(), Long::sum);
                }
            }

            for (Direction dir : Direction.values()) {
                if (!pipe.isConnected(dir)) {
                    continue;
                }
                BlockPos nextPos = pipe.getBlockPos().relative(dir);
                if (!visited.add(nextPos)) {
                    continue;
                }
                if (level.getBlockEntity(nextPos) instanceof FluidPipeBlockEntity next
                        && next.isConnected(dir.getOpposite())) {
                    queue.add(next);
                }
            }
        }

        if (totals.isEmpty()) {
            return;
        }

        ListTag list = new ListTag();
        totals.entrySet().stream()
                .sorted(Map.Entry.<Fluid, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(entry.getKey());
                    if (fluidId == null) {
                        return;
                    }
                    CompoundTag fluidTag = new CompoundTag();
                    fluidTag.putString("fluid", fluidId.toString());
                    fluidTag.putLong("amount", entry.getValue());
                    list.add(fluidTag);
                });

        data.put(DATA_KEY, list);
        data.putInt(PIPE_COUNT_KEY, pipeCount);
    }

    @Override
    public ResourceLocation getUid() {
        return PipeTotals.id("pipe_network_totals");
    }
}

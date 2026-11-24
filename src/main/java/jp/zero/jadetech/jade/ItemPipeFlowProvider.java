package jp.zero.jadetech.jade;

import com.gregtechceu.gtceu.common.blockentity.ItemPipeBlockEntity;
import jp.zero.jadetech.JadeTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Server side of the item pipe flow display.
 *
 * <p>GregTech item pipes never hold items: transfers are simulated, and
 * each pipe only keeps a per-route counter of items currently in transit
 * (which decays at the pipe's transfer rate). The item types are not stored
 * anywhere, so a per-item breakdown is impossible — but the counters can be
 * summed over the whole network to show whether a line is moving or
 * stalled. This walks the item pipe network exactly like the fluid totals
 * do and sums {@link ItemPipeBlockEntity#getTransferredItemCount()}.</p>
 */
public enum ItemPipeFlowProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    /** Safety cap so a pathological network can never stall the server. */
    private static final int MAX_PIPES = 4096;

    public static final String TOTAL_KEY = "item_flow_total";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ItemPipeBlockEntity start)) {
            return;
        }
        Level level = accessor.getLevel();
        if (level == null) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<ItemPipeBlockEntity> queue = new ArrayDeque<>();
        visited.add(start.getBlockPos());
        queue.add(start);

        long total = 0;
        int pipeCount = 0;
        while (!queue.isEmpty() && pipeCount < MAX_PIPES) {
            ItemPipeBlockEntity pipe = queue.poll();
            pipeCount++;
            total += pipe.getTransferredItems();

            for (Direction dir : Direction.values()) {
                if (!pipe.isConnected(dir)) {
                    continue;
                }
                BlockPos nextPos = pipe.getBlockPos().relative(dir);
                if (!visited.add(nextPos)) {
                    continue;
                }
                if (level.getBlockEntity(nextPos) instanceof ItemPipeBlockEntity next
                        && next.isConnected(dir.getOpposite())) {
                    queue.add(next);
                }
            }
        }

        if (total > 0) {
            data.putLong(TOTAL_KEY, total);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return JadeTech.id("item_pipe_flow");
    }
}

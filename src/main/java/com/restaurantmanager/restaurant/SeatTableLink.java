package com.restaurantmanager.restaurant;

import net.minecraft.core.BlockPos;

public class SeatTableLink {
    private final BlockPos chairPos;
    private final BlockPos tablePos;

    public SeatTableLink(BlockPos chairPos, BlockPos tablePos) {
        this.chairPos = chairPos.immutable();
        this.tablePos = tablePos.immutable();
    }

    public BlockPos getChairPos() {
        return chairPos;
    }

    public BlockPos getTablePos() {
        return tablePos;
    }

    public boolean hasChair(BlockPos pos) {
        return chairPos.equals(pos);
    }

    public boolean contains(BlockPos pos) {
        return chairPos.equals(pos) || tablePos.equals(pos);
    }
}

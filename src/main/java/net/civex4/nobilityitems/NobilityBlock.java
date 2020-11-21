package net.civex4.nobilityitems;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A custom block
 *
 * @author Earthcomputer
 */
public class NobilityBlock {
    private final String internalName;
    private final BlockData blockData;
    private final NobilityItem item;
    private final boolean waterlogged;

    public NobilityBlock(String internalName, BlockData blockData, NobilityItem item) {
        this.internalName = internalName;
        this.blockData = blockData;
        this.item = item;
        if (item != null) {
            item.setBlock(this);
        }
        this.waterlogged = blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
    }

    public NobilityBlock(String internalName, ConfigurationSection map) {
        this.internalName = internalName;
        String blockStr = map.getString("blockData");
        if (blockStr == null) {
            throw new IllegalArgumentException("Nobility block \"" + internalName + "\" missing required property \"blockData\"");
        }
        this.blockData = Bukkit.createBlockData(blockStr);
        if (!UnobtainableBlocks.isUnobtainable(blockData)) {
            throw new IllegalArgumentException("Nobility block \"" + internalName + "\" has block data \"" + blockStr + "\" which is not unobtainable");
        }
        if (map.getBoolean("hasItem", false)) {
            this.item = ItemManager.getItem(internalName);
            this.item.setBlock(this);
        } else {
            this.item = null;
        }
        this.waterlogged = map.getBoolean("waterlogged", blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged());
    }

    public String getInternalName() {
        return internalName;
    }

    public BlockData getBlockData() {
        return blockData;
    }

    public boolean hasItem() {
        return item != null;
    }

    public NobilityItem getItem() {
        return item;
    }

    public boolean isWaterlogged() {
        return waterlogged;
    }

    public boolean equalsBlock(Block block) {
        return equalsBlock(block.getBlockData());
    }

    public boolean equalsBlock(BlockData block) {
        return block.equals(blockData);
    }

    @Override
    public int hashCode() {
        return internalName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof NobilityBlock)) return false;
        NobilityBlock that = (NobilityBlock) o;
        return this.internalName.equals(that.internalName);
    }
}

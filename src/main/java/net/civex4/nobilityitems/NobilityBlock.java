package net.civex4.nobilityitems;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A custom block
 *
 * @author Earthcomputer
 */
public class NobilityBlock {
    private final String internalName;
    private final BlockData blockData;
    private final String parentModel;
    private final Map<String, String> textures = new LinkedHashMap<>();
    private final NobilityItem item;

    public NobilityBlock(String internalName, BlockData blockData, NobilityItem item) {
        this.internalName = internalName;
        this.blockData = blockData;
        this.parentModel = "block/cube_all";
        this.textures.put("all", "block/" + internalName);
        this.item = item;
        if (item != null) {
            item.setBlock(this);
        }
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
        this.parentModel = map.getString("parentModel", "block/cube_all");
        ConfigurationSection texturesSection = map.getConfigurationSection("textures");
        if (texturesSection == null) {
            String texture = map.getString("texture", internalName);
            if (texture == null || !texture.startsWith("block/")) {
                throw new IllegalArgumentException("Textures in NobilityBlock \"" + internalName + "\" must start with \"block/\"");
            }
            this.textures.put("all", texture);
        } else {
            texturesSection.getValues(false).forEach((key, val) -> textures.put(key, "block/" + val));
        }
        if (map.getBoolean("hasItem", false)) {
            this.item = ItemManager.getItem(internalName);
            this.item.setBlock(this);
        } else {
            this.item = null;
        }
    }

    public String getInternalName() {
        return internalName;
    }

    public BlockData getBlockData() {
        return blockData;
    }

    public String getParentModel() {
        return parentModel;
    }

    public Map<String, String> getTextures() {
        return textures;
    }

    public boolean hasItem() {
        return item != null;
    }

    public NobilityItem getItem() {
        return item;
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

package net.civex4.nobilityitems;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bamboo;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.CommandBlock;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Jigsaw;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.StructureBlock;
import org.bukkit.block.data.type.TNT;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.block.data.type.Wall;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class UnobtainableBlocks {
    static boolean isUnobtainable(BlockData block) {
        return unobtainableBlocks.contains(block);
    }

    static Iterable<BlockData> getUnobtainableBlocks() {
        return unobtainableBlocks;
    }

    static Map<String, List<?>> getAllProperties(Material material) {
        return allProperties.get(material);
    }

    private static final boolean[] BOOLEANS = {false, true};
    private static final List<Boolean> BOOLEAN_LIST = Arrays.asList(false, true);
    private static final BlockFace[] CARDINAL_FACES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};
    private static final BlockFace[] ALL_FACES = {BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};

    private static final Set<BlockData> unobtainableBlocks = new LinkedHashSet<>();
    private static final Map<Material, Map<String, List<?>>> allProperties = new HashMap<>();
    static {
        for (Material material : Registry.MATERIAL) {
            if (!material.isBlock()) {
                continue;
            }

            BlockData blockData = material.createBlockData();

            if (blockData instanceof Slab) {
                allProperties.put(material, ImmutableMap.of("type", Arrays.asList(Slab.Type.values()), "waterlogged", BOOLEAN_LIST));
                if (material == Material.PETRIFIED_OAK_SLAB) {
                    for (Slab.Type type : Slab.Type.values()) {
                        for (boolean waterlogged : BOOLEANS) {
                            Slab slab = (Slab) material.createBlockData();
                            slab.setType(type);
                            slab.setWaterlogged(waterlogged);
                            unobtainableBlocks.add(slab);
                        }
                    }
                } else {
                    Slab slab = (Slab) blockData;
                    slab.setType(Slab.Type.DOUBLE);
                    slab.setWaterlogged(true);
                    unobtainableBlocks.add(slab);
                }
            }
            else if (blockData instanceof Wall) {
                allProperties.put(material, ImmutableMap.<String, List<?>>builder()
                        .put("north", Arrays.asList(Wall.Height.values()))
                        .put("south", Arrays.asList(Wall.Height.values()))
                        .put("west", Arrays.asList(Wall.Height.values()))
                        .put("east", Arrays.asList(Wall.Height.values()))
                        .put("up", BOOLEAN_LIST)
                        .put("waterlogged", BOOLEAN_LIST)
                        .build());
                for (Wall.Height northHeight : Wall.Height.values()) {
                    for (Wall.Height southHeight : Wall.Height.values()) {
                        for (Wall.Height westHeight : Wall.Height.values()) {
                            for (Wall.Height eastHeight : Wall.Height.values()) {
                                boolean straight = (northHeight == Wall.Height.NONE && southHeight == Wall.Height.NONE && westHeight != Wall.Height.NONE && eastHeight != Wall.Height.NONE)
                                        || (northHeight != Wall.Height.NONE && southHeight != Wall.Height.NONE && westHeight == Wall.Height.NONE && eastHeight == Wall.Height.NONE);
                                if (!straight) {
                                    for (boolean waterlogged : BOOLEANS) {
                                        Wall wall = (Wall) material.createBlockData();
                                        wall.setHeight(BlockFace.NORTH, northHeight);
                                        wall.setHeight(BlockFace.SOUTH, southHeight);
                                        wall.setHeight(BlockFace.WEST, westHeight);
                                        wall.setHeight(BlockFace.EAST, eastHeight);
                                        wall.setUp(false);
                                        wall.setWaterlogged(waterlogged);
                                        unobtainableBlocks.add(wall);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (blockData instanceof TNT) {
                allProperties.put(material, ImmutableMap.of("unstable", BOOLEAN_LIST));
                ((TNT) blockData).setUnstable(true);
                unobtainableBlocks.add(blockData);
            }
            else if (blockData instanceof Campfire) {
                allProperties.put(material, ImmutableMap.of(
                        "facing", Arrays.asList(CARDINAL_FACES),
                        "signal_fire", BOOLEAN_LIST,
                        "lit", BOOLEAN_LIST,
                        "waterlogged", BOOLEAN_LIST
                ));
                for (BlockFace facing : CARDINAL_FACES) {
                    for (boolean signalFire : BOOLEANS) {
                        Campfire campfire = (Campfire) material.createBlockData();
                        campfire.setFacing(facing);
                        campfire.setSignalFire(signalFire);
                        campfire.setLit(true);
                        campfire.setWaterlogged(true);
                        unobtainableBlocks.add(campfire);
                    }
                }
            }
            else if (blockData instanceof CommandBlock) {
                allProperties.put(material, ImmutableMap.of(
                        "facing", Arrays.asList(ALL_FACES),
                        "conditional", BOOLEAN_LIST
                ));
                for (BlockFace facing : ALL_FACES) {
                    for (boolean conditional : BOOLEANS) {
                        CommandBlock commandBlock = (CommandBlock) material.createBlockData();
                        commandBlock.setFacing(facing);
                        commandBlock.setConditional(conditional);
                        unobtainableBlocks.add(commandBlock);
                    }
                }
            }
            else if (blockData instanceof StructureBlock) {
                allProperties.put(material, ImmutableMap.of("mode", Arrays.asList(StructureBlock.Mode.values())));
                for (StructureBlock.Mode mode : StructureBlock.Mode.values()) {
                    StructureBlock structureBlock = (StructureBlock) material.createBlockData();
                    structureBlock.setMode(mode);
                    unobtainableBlocks.add(structureBlock);
                }
            }
            else if (blockData instanceof Jigsaw) {
                allProperties.put(material, ImmutableMap.of("orientation", Arrays.asList(Jigsaw.Orientation.values())));
                for (Jigsaw.Orientation orientation : Jigsaw.Orientation.values()) {
                    Jigsaw jigsaw = (Jigsaw) material.createBlockData();
                    jigsaw.setOrientation(orientation);
                    unobtainableBlocks.add(jigsaw);
                }
            }
            // Lectern block entity may replace the invalid blockstate we place, so don't use this
//                else if (blockData instanceof Lectern) {
//                    allProperties.put(material, ImmutableMap.of(
//                            "facing", Arrays.asList(CARDINAL_FACES),
//                            "powered", BOOLEAN_LIST,
//                            "has_book", BOOLEAN_LIST
//                    ));
//                    for (BlockFace facing : CARDINAL_FACES) {
//                        Lectern lectern = (Lectern) material.createBlockData();
//                        lectern.setFacing(facing);
//                        lectern.setPowered(true);
//                        //lectern.setHasBook(false); TODO: uncomment when spigot adds this method
//                        unobtainableBlocks.add(lectern);
//                    }
//                }
            else if (blockData instanceof Door && material == Material.IRON_DOOR) {
                allProperties.put(material, ImmutableMap.of(
                        "hinge", Arrays.asList(Door.Hinge.values()),
                        "half", Arrays.asList(Bisected.Half.values()),
                        "facing", Arrays.asList(CARDINAL_FACES),
                        "open", BOOLEAN_LIST,
                        "powered", BOOLEAN_LIST
                ));
                for (Door.Hinge hinge : Door.Hinge.values()) {
                    for (Bisected.Half half : Bisected.Half.values()) {
                        for (BlockFace facing : CARDINAL_FACES) {
                            for (boolean open : BOOLEANS) {
                                boolean powered = !open;
                                Door door = (Door) material.createBlockData();
                                door.setHinge(hinge);
                                door.setHalf(half);
                                door.setFacing(facing);
                                door.setOpen(open);
                                door.setPowered(powered);
                                unobtainableBlocks.add(door);
                            }
                        }
                    }
                }
            }
            else if (blockData instanceof TrapDoor && material == Material.IRON_TRAPDOOR) {
                allProperties.put(material, ImmutableMap.of(
                        "half", Arrays.asList(Bisected.Half.values()),
                        "facing", Arrays.asList(CARDINAL_FACES),
                        "waterlogged", BOOLEAN_LIST,
                        "open", BOOLEAN_LIST,
                        "powered", BOOLEAN_LIST
                ));
                for (Bisected.Half half : Bisected.Half.values()) {
                    for (BlockFace facing : CARDINAL_FACES) {
                        for (boolean waterlogged : BOOLEANS) {
                            for (boolean open : BOOLEANS) {
                                boolean powered = !open;
                                TrapDoor trapdoor = (TrapDoor) material.createBlockData();
                                trapdoor.setHalf(half);
                                trapdoor.setFacing(facing);
                                trapdoor.setWaterlogged(waterlogged);
                                trapdoor.setOpen(open);
                                trapdoor.setPowered(powered);
                                unobtainableBlocks.add(trapdoor);
                            }
                        }
                    }
                }
            }
            else if (blockData instanceof Bamboo) {
                allProperties.put(material, ImmutableMap.of(
                        "age", Arrays.asList(0, 1),
                        "leaves", Arrays.asList(Bamboo.Leaves.values()),
                        "stage", Arrays.asList(0, 1)
                ));
                // Only large leaves are compatible with stage 1
                for (Bamboo.Leaves leaves : new Bamboo.Leaves[] { Bamboo.Leaves.NONE, Bamboo.Leaves.SMALL }) {
                    for (int age = 0; age <= 1; age++) {
                        Bamboo bamboo = (Bamboo) material.createBlockData();
                        bamboo.setLeaves(leaves);
                        bamboo.setAge(age);
                        bamboo.setStage(1);
                        unobtainableBlocks.add(bamboo);
                    }
                }
                // Large leaves are incompatible with age 0
                for (int stage = 0; stage <= 1; stage++) {
                    Bamboo bamboo = (Bamboo) material.createBlockData();
                    bamboo.setLeaves(Bamboo.Leaves.LARGE);
                    bamboo.setAge(0);
                    bamboo.setStage(stage);
                    unobtainableBlocks.add(bamboo);
                }
            }
        }
    }
}

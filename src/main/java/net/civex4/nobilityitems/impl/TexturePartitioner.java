package net.civex4.nobilityitems.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

public class TexturePartitioner {
    private static final Comparator<Point> H_COMPARATOR = Comparator.<Point>comparingInt(p -> p.y).thenComparingInt(p -> p.x);
    private static final Comparator<Point> V_COMPARATOR = Comparator.<Point>comparingInt(p -> p.x).thenComparingInt(p -> p.y);

    public static Result partitionTexture(InputStream textureFile, String textureFileName, float minU, float minV, float maxU, float maxV) {
        if (minU < 0) {
            minU = 0;
        } else if (minU > 1) {
            minU = 1;
        }
        if (minV < 0) {
            minV = 0;
        } else if (minV > 1) {
            minV = 1;
        }

        BufferedImage image;
        try {
            image = ImageIO.read(textureFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        int width = image.getWidth(), height = image.getHeight();
        if (width == 0 || height == 0) {
            return null;
        }

        // get alpha from image
        int[] alpha = new int[width * height];
        WritableRaster alphaRaster = image.getAlphaRaster();
        if (alphaRaster == null) {
            Bukkit.getLogger().severe("Image " + textureFileName + " doesn't have an alpha channel");
            return null;
        }
        alphaRaster.getPixels(0, 0, width, height, alpha);
        int minX = (int) (minU * width);
        int maxX = (int) (maxU * width);
        int minY = (int) (minV * height);
        int maxY = (int) (maxV * height);
        Arrays.fill(alpha, 0, minY * width, 0);
        Arrays.fill(alpha, maxY * width, alpha.length, 0);
        if (minX > 0 || maxX < width) {
            for (int y = minY; y < maxY; y++) {
                Arrays.fill(alpha, y * width, minX + y * width, 0);
                Arrays.fill(alpha, maxX + y * width, (y + 1) * width, 0);
            }
        }

        List<Polygon> polygons = loadInitialPolygons(alpha, width, height);

        List<Rectangle> rectangles = new ArrayList<>();

        for (Polygon polygon : polygons) {
            addRectangleDecomposition(alpha, width, height, polygon, rectangles);
        }

        return new Result(width, height, rectangles);
    }

    private static List<Polygon> loadInitialPolygons(int[] alpha, int width, int height) {
        // color each pixel according to its region
        int[] color = new int[width * height];
        List<BitSet> colorAdjacencyMatrix = new ArrayList<>();
        colorAdjacencyMatrix.add(new BitSet());

        int numColors;
        {
            int currentColor = 1; // start color indexes at 1, so that 0 can encode unassigned color
            Set<Point> nextInRegion = new HashSet<>();
            Set<Point> nextRegion = new HashSet<>();
            nextRegion.add(new Point(0, 0));
            while (!nextRegion.isEmpty()) {
                colorAdjacencyMatrix.add(new BitSet());

                nextInRegion.add(nextRegion.iterator().next());
                while (!nextInRegion.isEmpty()) {
                    Iterator<Point> itr = nextInRegion.iterator();
                    Point point = itr.next();
                    itr.remove();
                    nextRegion.remove(point);

                    int idx = point.x + point.y * width;
                    color[idx] = currentColor;
                    if (point.x == 0 || point.y == 0 || point.x == width - 1 || point.y == height - 1) {
                        if (alpha[idx] == 0) {
                            colorAdjacencyMatrix.get(0).set(currentColor);
                            colorAdjacencyMatrix.get(currentColor).set(0);
                        }
                    }

                    if (point.x != 0 && color[idx - 1] != currentColor) {
                        if ((alpha[idx] == 0) == (alpha[idx - 1] == 0)) {
                            nextInRegion.add(new Point(point.x - 1, point.y));
                        } else if (color[idx - 1] == 0) {
                            nextRegion.add(new Point(point.x - 1, point.y));
                        } else {
                            colorAdjacencyMatrix.get(color[idx - 1]).set(currentColor);
                            colorAdjacencyMatrix.get(currentColor).set(color[idx - 1]);
                        }
                    }
                    if (point.y != 0 && color[idx - width] != currentColor) {
                        if ((alpha[idx] == 0) == (alpha[idx - width] == 0)) {
                            nextInRegion.add(new Point(point.x, point.y - 1));
                        } else if (color[idx - width] == 0) {
                            nextRegion.add(new Point(point.x, point.y - 1));
                        } else {
                            colorAdjacencyMatrix.get(color[idx - width]).set(currentColor);
                            colorAdjacencyMatrix.get(currentColor).set(color[idx - width]);
                        }
                    }
                    if (point.x != width - 1 && color[idx + 1] != currentColor) {
                        if ((alpha[idx] == 0) == (alpha[idx + 1] == 0)) {
                            nextInRegion.add(new Point(point.x + 1, point.y));
                        } else if (color[idx + 1] == 0) {
                            nextRegion.add(new Point(point.x + 1, point.y));
                        } else {
                            colorAdjacencyMatrix.get(color[idx + 1]).set(currentColor);
                            colorAdjacencyMatrix.get(currentColor).set(color[idx + 1]);
                        }
                    }
                    if (point.y != height - 1 && color[idx + width] != currentColor) {
                        if ((alpha[idx] == 0) == (alpha[idx + width] == 0)) {
                            nextInRegion.add(new Point(point.x, point.y + 1));
                        } else if (color[idx + width] == 0) {
                            nextRegion.add(new Point(point.x, point.y + 1));
                        } else {
                            colorAdjacencyMatrix.get(color[idx + width]).set(currentColor);
                            colorAdjacencyMatrix.get(currentColor).set(color[idx + width]);
                        }
                    }
                }
                currentColor++;
            }
            numColors = currentColor - 1;
        }

        // compute the distance of every color from the edge
        int[] distanceFromEdge = new int[numColors + 1];
        int maxDistanceFromEdge = 0;
        {
            boolean changed;
            do {
                changed = false;
                for (int col = 0; col <= numColors; col++) {
                    BitSet neighbors = colorAdjacencyMatrix.get(col);
                    for (int neighbor = neighbors.nextSetBit(0); neighbor != -1; neighbor = neighbors.nextSetBit(neighbor + 1)) {
                        if (neighbor != 0) {
                            if (distanceFromEdge[neighbor] == 0 || distanceFromEdge[neighbor] > distanceFromEdge[col] + 1) {
                                distanceFromEdge[neighbor] = distanceFromEdge[col] + 1;
                                changed = true;
                                if (distanceFromEdge[col] + 1 > maxDistanceFromEdge) {
                                    maxDistanceFromEdge = distanceFromEdge[col] + 1;
                                }
                            }
                        }
                    }
                }
            } while (changed);
        }

        // compute an array of unique parents of polygons.
        // transparent areas with nonzero unique parents are "holes" of that parent.
        // any shape which is not a child is considered to be a parent in this section, this resolves all the problems with holes with 2 parents.
        int[] uniqueParents = new int[numColors + 1];
        for (int dist = maxDistanceFromEdge; dist > 1; dist--) {
            for (int col = 1; col <= numColors; col++) {
                if (distanceFromEdge[col] == dist) {
                    int uniqueParent = 0;
                    BitSet neighbors = colorAdjacencyMatrix.get(col);
                    for (int neighbor = neighbors.nextSetBit(0); neighbor != -1; neighbor = neighbors.nextSetBit(neighbor + 1)) {
                        if (uniqueParents[neighbor] != col) {
                            if (uniqueParent == 0) {
                                uniqueParent = neighbor;
                            } else {
                                uniqueParent = 0;
                                break;
                            }
                        }
                    }
                    uniqueParents[col] = uniqueParent;
                }
            }
        }

        IntPredicate isOutsideRegion = col -> (distanceFromEdge[col] & 1) != 0 && uniqueParents[col] == 0;


        // find all boundary edges of the colored regions.
        // if a region is not an outside region, then an edge is a boundary edge if it is not one with a child.

        //noinspection unchecked
        BiMap<Point, Point>[] horizontalEdges = new BiMap[numColors + 1]; // 1-indexed
        Arrays.setAll(horizontalEdges, i -> HashBiMap.create());
        //noinspection unchecked
        BiMap<Point, Point>[] verticalEdges = new BiMap[numColors + 1]; // 1-indexed
        Arrays.setAll(verticalEdges, i -> HashBiMap.create());

        for (int x = 0; x < width; x++) {
            if (alpha[x] != 0) {
                horizontalEdges[color[x]].put(new Point(x, 0), new Point(x + 1, 0));
            }
            if (alpha[alpha.length - width + x] != 0) {
                horizontalEdges[color[alpha.length - width + x]].put(new Point(x, height), new Point(x + 1, height));
            }
        }
        for (int y = 1; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int colorHere = color[x + y * width];
                int colorUp = color[x + (y - 1) * width];
                if (colorHere != colorUp) {
                    if (!isOutsideRegion.test(colorHere) && uniqueParents[colorUp] != colorHere) {
                        horizontalEdges[colorHere].put(new Point(x, y), new Point(x + 1, y));
                    }

                    if (!isOutsideRegion.test(colorUp) && uniqueParents[colorHere] != colorUp) {
                        horizontalEdges[colorUp].put(new Point(x, y), new Point(x + 1, y));
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            if (alpha[y * width] != 0) {
                verticalEdges[color[y * width]].put(new Point(0, y), new Point(0, y + 1));
            }
            if (alpha[(y + 1) * width - 1] != 0) {
                verticalEdges[color[(y + 1) * width - 1]].put(new Point(width, y), new Point(width, y + 1));
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 1; x < width; x++) {
                int colorHere = color[x + y * width];
                int colorLeft = color[(x - 1) + y * width];
                if (colorHere != colorLeft) {
                    if (!isOutsideRegion.test(colorHere) && uniqueParents[colorLeft] != colorHere) {
                        verticalEdges[colorHere].put(new Point(x, y), new Point(x, y + 1));
                    }

                    if (!isOutsideRegion.test(colorLeft) && uniqueParents[colorHere] != colorLeft) {
                        verticalEdges[colorLeft].put(new Point(x, y), new Point(x, y + 1));
                    }
                }
            }
        }

        // construct polygon vertices and hole vertices from these edges
        List<List<Point>> polygonVertices = new ArrayList<>();
        Map<Integer, Integer> polygonsByColor = new HashMap<>();
        List<List<Point>> holeVertices = new ArrayList<>();
        Map<Integer, Integer> colorsByHole = new HashMap<>();
        for (int col = 1; col <= numColors; col++) {
            if (isOutsideRegion.test(col)) {
                continue;
            }
            List<Point> points = new ArrayList<>();
            Point firstPoint = horizontalEdges[col].keySet().iterator().next();
            Point v;
            while ((v = horizontalEdges[col].inverse().get(firstPoint)) != null) {
                firstPoint = v;
            }
            boolean clockwise = color[firstPoint.x + firstPoint.y * width] == col;
            Point point = firstPoint;
            do {
                points.add(point);
                BiMap<Point, Point> horizontal = horizontalEdges[col].containsKey(point) ? horizontalEdges[col] : horizontalEdges[col].inverse();
                while ((v = horizontal.get(point)) != null) {
                    point = v;
                }
                points.add(point);
                BiMap<Point, Point> vertical = verticalEdges[col].containsKey(point) ? verticalEdges[col] : verticalEdges[col].inverse();
                while ((v = vertical.get(point)) != null) {
                    point = v;
                }
            } while (!point.equals(firstPoint));
            boolean transparent = (distanceFromEdge[col] & 1) != 0;
            if (clockwise != transparent) {
                Collections.reverse(points);
            }
            if (transparent) {
                colorsByHole.put(holeVertices.size(), col);
                holeVertices.add(points);
            } else {
                polygonsByColor.put(col, polygonVertices.size());
                polygonVertices.add(points);
            }
        }

        // move these into polygon structs
        List<Polygon> polygons = new ArrayList<>();
        for (List<Point> outerBoundary : polygonVertices) {
            polygons.add(new Polygon(outerBoundary));
        }
        for (int holeIdx = 0; holeIdx < holeVertices.size(); holeIdx++) {
            polygons.get(polygonsByColor.get(uniqueParents[colorsByHole.get(holeIdx)])).holes.add(holeVertices.get(holeIdx));
        }

        return polygons;
    }

    private static void addRectangleDecomposition(int[] alpha, int width, int height, Polygon polygon, List<Rectangle> rectanglesOut) {
        // Algorithm taken from research paper:
        // Soltan, Valeriu, and Gorpinevich, Alexei. "Minimum Dissection of a Rectilinear Polygon with Arbitrary Holes into Rectangles." Discrete & Computational Geometry 9.1 (1993): 57-79. Web.

        List<LineSegment> horizontalEffectiveChords = new ArrayList<>();
        List<LineSegment> verticalEffectiveChords = new ArrayList<>();
        getEffectiveChords(polygon, horizontalEffectiveChords, verticalEffectiveChords);

        List<LineSegment> horizontalLines = new ArrayList<>();
        List<LineSegment> verticalLines = new ArrayList<>();
        getAdmissibleFamilyOfMaximumCardinality(horizontalEffectiveChords, verticalEffectiveChords, horizontalLines, verticalLines);

        eliminateConcaveVerticesAndAddPolygonLines(polygon, horizontalLines, verticalLines);

        linesToRectangles(alpha, width, height, horizontalLines, verticalLines, rectanglesOut);
    }

    private static void getEffectiveChords(Polygon polygon, List<LineSegment> horizontalEffectiveChordsOut, List<LineSegment> verticalEffectiveChordsOut) {
        List<LineSegment> edges = new ArrayList<>();
        edges.add(new LineSegment(polygon.outerBoundary.get(polygon.outerBoundary.size() - 1), polygon.outerBoundary.get(0)));
        for (int i = 1; i < polygon.outerBoundary.size(); i++) {
            edges.add(new LineSegment(polygon.outerBoundary.get(i - 1), polygon.outerBoundary.get(i)));
        }
        for (List<Point> hole : polygon.holes) {
            edges.add(new LineSegment(hole.get(hole.size() - 1), hole.get(0)));
            for (int i = 1; i < hole.size(); i++) {
                edges.add(new LineSegment(hole.get(i - 1), hole.get(i)));
            }
        }

        // get horizontal effective chords
        addEffectiveChordsByAxis(edges, horizontalEffectiveChordsOut, point -> point.x, point -> point.y, false);
        // get vertical effective chords
        addEffectiveChordsByAxis(edges, verticalEffectiveChordsOut, point -> point.y, point -> point.x, true);
    }

    // The comments and variable names in this method assume we are finding horizontal chords, but it is used for vertical chords too by swapping x and y.
    // The reversed parameter says whether to flip the meaning of clockwise/counterclockwise. When switching x/y, the handedness of the coordinate system
    // changes, and clockwise/counterclockwise need swapping.
    private static void addEffectiveChordsByAxis(List<LineSegment> edges, List<LineSegment> effectiveChordsOut, ToIntFunction<Point> x, ToIntFunction<Point> y, boolean flipped) {
        edges.sort(Comparator.<LineSegment>comparingInt(edge -> y.applyAsInt(edge.a)) // higher top comes first
                .thenComparingInt(edge -> x.applyAsInt(edge.a)) // then further left comes first
                .thenComparingInt(edge -> x.applyAsInt(edge.b)) // then vertical comes first
        );
        List<LineSegment> currentVerticalEdges = new ArrayList<>(); // sorted by x coordinate
        List<LineSegment> verticalEdgesEndingHere = new ArrayList<>(); // sorted by x coordinate
        int currentY = -1;
        for (int idx = 0; idx < edges.size() - 1; idx++) {
            LineSegment edge = edges.get(idx);

            if (y.applyAsInt(edge.a) != currentY) {
                currentY = y.applyAsInt(edge.a);
                // prune
                verticalEdgesEndingHere.clear();
                Iterator<LineSegment> itr = currentVerticalEdges.iterator();
                while (itr.hasNext()) {
                    LineSegment e = itr.next();
                    if (y.applyAsInt(e.b) <= y.applyAsInt(edge.a)) {
                        itr.remove();
                        verticalEdgesEndingHere.add(e);
                    }
                }
            }

            int verticalEdgesIndex = Collections.binarySearch(currentVerticalEdges, edge, Comparator.comparingInt(e -> x.applyAsInt(e.b)));
            assert verticalEdgesIndex < 0; // already having an edge at this x coordinate would imply an overlapping vertical edge, which is illegal
            verticalEdgesIndex = -verticalEdgesIndex - 1;
            if (x.applyAsInt(edge.a) == x.applyAsInt(edge.b)) {
                currentVerticalEdges.add(verticalEdgesIndex, edge);
            } else {
                LineSegment nextEdge = edges.get(idx + 1);
                LineSegment verticalEdge = null; // There will always be a vertical edge attached to the end of this edge
                if (x.applyAsInt(nextEdge.a) == x.applyAsInt(nextEdge.b)) {
                    // nextEdge vertical, either attached to this edge or its at x coordinate we're looking for, in either case it's okay to go one further
                    if (x.applyAsInt(nextEdge.a) == x.applyAsInt(edge.b)) {
                        verticalEdge = nextEdge;
                    }
                    if (idx != edges.size() - 2) {
                        nextEdge = edges.get(idx + 2);
                    }
                }
                // if we didn't find the vertical edge, it's coming from above
                if (verticalEdge == null) {
                    int index = Collections.binarySearch(verticalEdgesEndingHere, edge, Comparator.comparingInt(e -> x.applyAsInt(e.b)));
                    assert index >= 0; // There must be an edge connected to this one
                    verticalEdge = verticalEdgesEndingHere.get(index);
                }
                // make sure the next edge is actually on the same y coordinate as this edge
                if (y.applyAsInt(nextEdge.a) == y.applyAsInt(edge.a)) {
                    // make sure they aren't two horizontal edges right on top of each other
                    if (x.applyAsInt(nextEdge.a) != x.applyAsInt(edge.b)) {
                        // make sure we aren't going through any vertical lines
                        if (verticalEdgesIndex >= currentVerticalEdges.size() || x.applyAsInt(currentVerticalEdges.get(verticalEdgesIndex).a) >= x.applyAsInt(nextEdge.a)) {
                            // check we aren't going outside the polygon
                            if (verticalEdge.reversed == flipped) {
                                effectiveChordsOut.add(new LineSegment(edge.b, nextEdge.a));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void getAdmissibleFamilyOfMaximumCardinality(List<LineSegment> horizontalEffectiveChords, List<LineSegment> verticalEffectiveChords, List<LineSegment> horizontalAdmissibleFamilyOut, List<LineSegment> verticalAdmissibleFamilyOut) {
        // Sub-algorithm taken from research paper:
        // Imai, Hiroshi, and Asano, Takao. "EFFICIENT ALGORITHMS FOR GEOMETRIC GRAPH SEARCH PROBLEMS." SIAM Journal on Computing 15.2 (1986): 478-94. Web.

        List<BitSet> horizontalAdjacency = new ArrayList<>();
        for (int i = 0; i < horizontalEffectiveChords.size(); i++) {
            horizontalAdjacency.add(new BitSet());
        }
        List<BitSet> verticalAdjacency = new ArrayList<>();
        for (int i = 0; i < verticalEffectiveChords.size(); i++) {
            verticalAdjacency.add(new BitSet());
        }

        for (int hIndex = 0; hIndex < horizontalEffectiveChords.size(); hIndex++) {
            LineSegment hChord = horizontalEffectiveChords.get(hIndex);
            for (int vIndex = 0; vIndex < verticalEffectiveChords.size(); vIndex++) {
                LineSegment vChord = verticalEffectiveChords.get(vIndex);
                if (vChord.a.x >= hChord.a.x && vChord.a.x <= hChord.b.x && hChord.a.y >= vChord.a.y && hChord.a.y <= vChord.b.y) {
                    horizontalAdjacency.get(hIndex).set(vIndex);
                    verticalAdjacency.get(vIndex).set(hIndex);
                }
            }
        }

        BiMap<Integer, Integer> maximumMatching = HashBiMap.create();
        {
            BitSet alreadyVisitedLeft = new BitSet();
            BitSet alreadyVisitedRight = new BitSet();
            for (int hIndex = 0; hIndex < horizontalEffectiveChords.size(); hIndex++) {
                alreadyVisitedLeft.clear();
                alreadyVisitedRight.clear();
                dfsMaximumMatching(hIndex, horizontalAdjacency, maximumMatching, alreadyVisitedLeft, alreadyVisitedRight);
                if (maximumMatching.size() == verticalEffectiveChords.size()) {
                    // early exit, maximum matching can't be greater than the number of vertical effective chords
                    break;
                }
            }
        }

        // The old 2013 revision of the Wikipedia article on König's theorem is way easier to get an algorithm from than the current one:
        // https://en.wikipedia.org/w/index.php?title=K%C5%91nig%27s_theorem_(graph_theory)&oldid=570989886

        BitSet indSetLeft = new BitSet();
        BitSet indSetRight = new BitSet();
        BitSet indSetIterationLeft = new BitSet();
        BitSet indSetIterationRight = new BitSet();
        BitSet vertexCoverLeft = new BitSet();
        BitSet vertexCoverRight = new BitSet();
        BitSet vertexCoverIterationLeft = new BitSet();
        BitSet vertexCoverIterationRight = new BitSet();

        for (int left = 0; left < horizontalEffectiveChords.size(); left++) {
            if (!maximumMatching.containsKey(left)) {
                indSetIterationLeft.set(left);
            }
        }
        for (int right = 0; right < verticalEffectiveChords.size(); right++) {
            if (!maximumMatching.containsValue(right)) {
                indSetIterationRight.set(right);
            }
        }

        while (true) {
            indSetLeft.or(indSetIterationLeft);
            indSetRight.or(indSetIterationRight);

            boolean foundVertex = false;
            for (int left = indSetIterationLeft.nextSetBit(0); left != -1; left = indSetIterationLeft.nextSetBit(left + 1)) {
                BitSet adjacent = horizontalAdjacency.get(left);
                for (int right = adjacent.nextSetBit(0); right != -1; right = adjacent.nextSetBit(right + 1)) {
                    if (!Integer.valueOf(right).equals(maximumMatching.get(left)) && !indSetRight.get(right) && !vertexCoverRight.get(right)) {
                        vertexCoverIterationRight.set(right);
                        foundVertex = true;
                    }
                }
            }
            for (int right = indSetIterationRight.nextSetBit(0); right != -1; right = indSetIterationRight.nextSetBit(right + 1)) {
                BitSet adjacent = verticalAdjacency.get(right);
                for (int left = adjacent.nextSetBit(0); left != -1; left = adjacent.nextSetBit(left + 1)) {
                    if (!Integer.valueOf(left).equals(maximumMatching.inverse().get(right)) && !indSetLeft.get(left) && !vertexCoverLeft.get(left)) {
                        vertexCoverIterationLeft.set(left);
                        foundVertex = true;
                    }
                }
            }
            indSetIterationLeft.clear();
            indSetIterationRight.clear();

            if (!foundVertex) {
                for (int left = indSetLeft.nextClearBit(0); left < horizontalEffectiveChords.size(); left = indSetLeft.nextClearBit(left + 1)) {
                    if (!vertexCoverLeft.get(left)) {
                        vertexCoverIterationLeft.set(left);
                        foundVertex = true;
                        break;
                    }
                }
                if (!foundVertex) {
                    for (int right = indSetRight.nextClearBit(0); right < verticalEffectiveChords.size(); right = indSetRight.nextClearBit(right + 1)) {
                        if (!vertexCoverRight.get(right)) {
                            vertexCoverIterationRight.set(right);
                            foundVertex = true;
                            break;
                        }
                    }
                    if (!foundVertex) {
                        break;
                    }
                }
            }

            vertexCoverLeft.or(vertexCoverIterationLeft);
            vertexCoverRight.or(vertexCoverIterationRight);

            for (int left = vertexCoverIterationLeft.nextSetBit(0); left != -1; left = vertexCoverIterationLeft.nextSetBit(left + 1)) {
                Integer right = maximumMatching.get(left);
                assert right != null;
                indSetIterationRight.set(right);
            }
            for (int right = vertexCoverIterationRight.nextSetBit(0); right != -1; right = vertexCoverIterationRight.nextSetBit(right + 1)) {
                Integer left = maximumMatching.inverse().get(right);
                assert left != null;
                indSetIterationLeft.set(left);
            }
            vertexCoverIterationLeft.clear();
            vertexCoverIterationRight.clear();
        }

        for (int left = indSetLeft.nextSetBit(0); left != -1; left = indSetLeft.nextSetBit(left + 1)) {
            LineSegment line = horizontalEffectiveChords.get(left);
            horizontalAdmissibleFamilyOut.add(line);
        }
        for (int right = indSetRight.nextSetBit(0); right != -1; right = indSetRight.nextSetBit(right + 1)) {
            LineSegment line = verticalEffectiveChords.get(right);
            verticalAdmissibleFamilyOut.add(line);
        }
    }

    private static boolean dfsMaximumMatching(int left, List<BitSet> leftAdjacency, BiMap<Integer, Integer> maximumMatching, BitSet alreadyVisitedLeft, BitSet alreadyVisitedRight) {
        alreadyVisitedLeft.set(left);
        BitSet leftAdjacent = leftAdjacency.get(left);
        for (int right = leftAdjacent.nextSetBit(0); right != -1; right = leftAdjacent.nextSetBit(right + 1)) {
            if (alreadyVisitedRight.get(right)) {
                continue;
            }
            if (!maximumMatching.containsValue(right)) {
                maximumMatching.put(left, right);
                return true;
            }

            int nextLeft = maximumMatching.inverse().get(right);
            if (!alreadyVisitedLeft.get(nextLeft)) {
                alreadyVisitedRight.set(right);
                maximumMatching.remove(nextLeft);
                maximumMatching.put(left, right);
                if (dfsMaximumMatching(nextLeft, leftAdjacency, maximumMatching, alreadyVisitedLeft, alreadyVisitedRight)) {
                    return true;
                }
                // undo
                alreadyVisitedRight.clear(right);
                maximumMatching.remove(left);
                maximumMatching.put(nextLeft, right);
            }
        }
        alreadyVisitedLeft.clear(left);
        return false;
    }

    private static void eliminateConcaveVerticesAndAddPolygonLines(Polygon polygon, List<LineSegment> hAdditionalLinesInOut, List<LineSegment> vAdditionalLinesInOut) {
        List<LineSegment> existingHLines = new ArrayList<>();
        List<LineSegment> existingVLines = new ArrayList<>();
        for (int idx = 0; idx < polygon.outerBoundary.size(); idx++) {
            int prevIdx = idx == 0 ? polygon.outerBoundary.size() - 1 : idx - 1;
            Point a = polygon.outerBoundary.get(prevIdx);
            Point b = polygon.outerBoundary.get(idx);
            if (a.x == b.x) {
                existingVLines.add(new LineSegment(a, b));
            } else {
                existingHLines.add(new LineSegment(a, b));
            }
        }
        for (List<Point> hole : polygon.holes) {
            for (int idx = 0; idx < hole.size(); idx++) {
                int prevIdx = idx == 0 ? hole.size() - 1 : idx - 1;
                Point a = hole.get(prevIdx);
                Point b = hole.get(idx);
                if (a.x == b.x) {
                    existingVLines.add(new LineSegment(a, b));
                } else {
                    existingHLines.add(new LineSegment(a, b));
                }
            }
        }
        //noinspection ConstantConditions,StaticPseudoFunctionalStyleMethod
        List<Point> existingVPointsView = Lists.transform(existingVLines, line -> line.a);
        existingHLines.sort((a, b) -> H_COMPARATOR.compare(a.a, b.a));
        existingVLines.sort((a, b) -> V_COMPARATOR.compare(a.a, b.a));

        //noinspection ConstantConditions,StaticPseudoFunctionalStyleMethod
        List<Point> hPointsView = Lists.transform(hAdditionalLinesInOut, line -> line.a);
        //noinspection ConstantConditions,StaticPseudoFunctionalStyleMethod
        List<Point> vPointsView = Lists.transform(vAdditionalLinesInOut, line -> line.a);

        eliminateConcaveVerticesOnBoundary(polygon.outerBoundary, hAdditionalLinesInOut, vAdditionalLinesInOut, existingVLines, existingVPointsView, hPointsView, vPointsView);
        for (List<Point> hole : polygon.holes) {
            eliminateConcaveVerticesOnBoundary(hole, hAdditionalLinesInOut, vAdditionalLinesInOut, existingVLines, existingVPointsView, hPointsView, vPointsView);
        }

        hAdditionalLinesInOut.addAll(existingHLines);
        hAdditionalLinesInOut.sort((a, b) -> H_COMPARATOR.compare(a.a, b.a));
        vAdditionalLinesInOut.addAll(existingVLines);
        vAdditionalLinesInOut.sort((a, b) -> V_COMPARATOR.compare(a.a, b.a));
    }

    private static void eliminateConcaveVerticesOnBoundary(List<Point> boundary,
                                                           List<LineSegment> hAdditionalLinesInOut, List<LineSegment> vAdditionalLines,
                                                           List<LineSegment> existingVLines, List<Point> existingVPointsView,
                                                           List<Point> hPointsView, List<Point> vPointsView) {
        for (int idx = 0; idx < boundary.size(); idx++) {
            Point point = boundary.get(idx);

            int prevIdx = idx == 0 ? boundary.size() - 1 : idx - 1;
            Point prevPoint = boundary.get(prevIdx);
            int nextIdx = idx == boundary.size() - 1 ? 0 : idx + 1;
            Point nextPoint = boundary.get(nextIdx);

            int dx1 = point.x - prevPoint.x;
            int dy1 = point.y - prevPoint.y;
            int dx2 = nextPoint.x - point.x;
            int dy2 = nextPoint.y - point.y;
            int det = dx1 * dy2 - dy1 * dx2;
            if (det < 0) {
                // this corner is convex
                continue;
            }

            int hIndex = Collections.binarySearch(hPointsView, point, H_COMPARATOR);
            int vIndex = Collections.binarySearch(vPointsView, point, V_COMPARATOR);
            if (hIndex >= 0 || vIndex >= 0) {
                // an effective chord already sorted out this vertex
                continue;
            }
            hIndex = -hIndex - 1;
            if (hIndex > 0 && hAdditionalLinesInOut.get(hIndex - 1).b.equals(point)) {
                // an effective chord already sorted out this vertex
                continue;
            }
            vIndex = -vIndex - 1;
            if (vIndex > 0 && vAdditionalLines.get(vIndex - 1).b.equals(point)) {
                // an effective chord already sorted out this vertex
                continue;
            }

            int existingIndex = Collections.binarySearch(existingVPointsView, point, V_COMPARATOR);
            if (existingIndex < 0) {
                // we are on the bottom of a vertical edge, it will be immediately before the insertion point in the list
                existingIndex = -existingIndex - 2;
            } else if (existingIndex > 0 && existingVLines.get(existingIndex - 1).b.equals(point)) {
                // we are on both the bottom and the top of a vertical edge. Can happen when a hole touches the outer boundary at a corner
                continue;
            }

            if (prevPoint.x < point.x || nextPoint.x < point.x) {
                // look right
                LineSegment candidateA = null;
                int aIndex;
                for (aIndex = vIndex; aIndex < vAdditionalLines.size(); aIndex++) {
                    LineSegment candidate = vAdditionalLines.get(aIndex);
                    if (candidate.a.y <= point.y && candidate.b.y >= point.y) {
                        candidateA = candidate;
                        break;
                    }
                }
                LineSegment candidateB = null;
                int bIndex;
                for (bIndex = existingIndex + 1; bIndex < existingVLines.size(); bIndex++) {
                    LineSegment candidate = existingVLines.get(bIndex);
                    if (candidateA != null && candidate.a.x >= candidateA.a.x) {
                        // it will be further right than the candidate we already found, so no point searching
                        break;
                    }
                    if (candidate.a.y <= point.y && candidate.b.y >= point.y) {
                        candidateB = candidate;
                        break;
                    }
                }
                assert candidateA != null || candidateB != null;
                LineSegment destLine;
                List<LineSegment> destList;
                int destIndex;
                if (candidateB == null || (candidateA != null && candidateA.a.x < candidateB.a.x)) {
                    destLine = candidateA;
                    destList = vAdditionalLines;
                    destIndex = aIndex;
                } else {
                    destLine = candidateB;
                    destList = existingVLines;
                    destIndex = bIndex;
                }
                Point destPoint = new Point(destLine.a.x, point.y);
                if (point.y != destLine.a.y && point.y != destLine.b.y) {
                    // split destLine into 2 separate lines
                    destList.set(destIndex, new LineSegment(destPoint, destLine.b));
                    destList.add(destIndex, new LineSegment(destLine.a, destPoint));
                }
                hAdditionalLinesInOut.add(hIndex, new LineSegment(point, destPoint));
            } else {
                // look left
                LineSegment candidateA = null;
                int aIndex;
                for (aIndex = vIndex - 1; aIndex >= 0; aIndex--) {
                    LineSegment candidate = vAdditionalLines.get(aIndex);
                    if (candidate.a.y <= point.y && candidate.b.y >= point.y) {
                        candidateA = candidate;
                        break;
                    }
                }
                LineSegment candidateB = null;
                int bIndex;
                for (bIndex = existingIndex - 1; bIndex >= 0; bIndex--) {
                    LineSegment candidate = existingVLines.get(bIndex);
                    if (candidateA != null && candidate.a.x <= candidateA.a.x) {
                        // it will be further left than the candidate we already found, so no point searching
                        break;
                    }
                    if (candidate.a.y <= point.y && candidate.b.y >= point.y) {
                        candidateB = candidate;
                        break;
                    }
                }
                assert candidateA != null || candidateB != null;
                LineSegment destLine;
                List<LineSegment> destList;
                int destIndex;
                if (candidateB == null || (candidateA != null && candidateA.a.x > candidateB.a.x)) {
                    destLine = candidateA;
                    destList = vAdditionalLines;
                    destIndex = aIndex;
                } else {
                    destLine = candidateB;
                    destList = existingVLines;
                    destIndex = bIndex;
                }
                Point destPoint = new Point(destLine.a.x, point.y);
                if (point.y != destLine.a.y && point.y != destLine.b.y) {
                    // split destLine into 2 separate lines
                    destList.set(destIndex, new LineSegment(destPoint, destLine.b));
                    destList.add(destIndex, new LineSegment(destLine.a, destPoint));
                }
                int indexToAdd = Collections.binarySearch(hPointsView, destPoint, H_COMPARATOR);
                assert indexToAdd < 0; // if this is not true then this concave angle should have been an effective chord
                indexToAdd = -indexToAdd - 1;
                hAdditionalLinesInOut.add(indexToAdd, new LineSegment(destPoint, point));
            }
        }
    }

    private static void linesToRectangles(int[] alpha, int width, int height, List<LineSegment> horizontalLines, List<LineSegment> verticalLines, List<Rectangle> rectanglesOut) {
        //noinspection ConstantConditions,StaticPseudoFunctionalStyleMethod
        List<Point> hPointsView = Lists.transform(horizontalLines, line -> line.a);
        //noinspection ConstantConditions,StaticPseudoFunctionalStyleMethod
        List<Point> vPointsView = Lists.transform(verticalLines, line -> line.a);

        for (int hIndex = 0; hIndex < horizontalLines.size(); hIndex++) {
            LineSegment hLine = horizontalLines.get(hIndex);
            // if the top-left pixel is transparent, and there is an attached vertical line, we have a rectangle
            if (hLine.a.y < height && alpha[hLine.a.x + hLine.a.y * width] != 0) {
                int vIndex = Collections.binarySearch(vPointsView, hLine.a, V_COMPARATOR);
                if (vIndex >= 0) {
                    LineSegment rightHLine = hLine;
                    int rightHIndex = hIndex;
                    while (Collections.binarySearch(vPointsView, rightHLine.b, V_COMPARATOR) < 0) {
                        rightHIndex++;
                        LineSegment lastLine = rightHLine;
                        rightHLine = horizontalLines.get(rightHIndex);
                        assert lastLine.b.equals(rightHLine.a);
                    }
                    LineSegment bottomVLine = verticalLines.get(vIndex);
                    int bottomVIndex = vIndex;
                    while (Collections.binarySearch(hPointsView, bottomVLine.b, H_COMPARATOR) < 0) {
                        bottomVIndex++;
                        LineSegment lastLine = bottomVLine;
                        bottomVLine = verticalLines.get(bottomVIndex);
                        assert lastLine.b.equals(bottomVLine.a);
                    }

                    rectanglesOut.add(new Rectangle(hLine.a.x, hLine.a.y, rightHLine.b.x, bottomVLine.b.y));
                }
            }
        }
    }

    private static final class Point {
        public final int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int hashCode() {
            return 73 * x + y;
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (other == null) return false;
            if (other.getClass() != Point.class) return false;
            Point that = (Point) other;
            return this.x == that.x && this.y == that.y;
        }
    }

    private static final class LineSegment {
        public final Point a;
        public final Point b;
        public final boolean reversed;

        public LineSegment(Point a, Point b) {
            assert a.x == b.x || a.y == b.y;
            this.reversed = a.x == b.x ? a.y > b.y : a.x > b.x;
            this.a = reversed ? b : a;
            this.b = reversed ? a : b;
        }
    }

    private static final class Polygon {
        // counterclockwise order
        private final List<Point> outerBoundary;
        // each in a clockwise order
        private final List<List<Point>> holes = new ArrayList<>();

        public Polygon(List<Point> outerBoundary) {
            this.outerBoundary = outerBoundary;
        }
    }

    public static final class Rectangle {
        public final int x1, y1, x2, y2;

        private Rectangle(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    public static final class Result {
        public final int width, height;
        public final List<Rectangle> rectangles;

        private Result(int width, int height, List<Rectangle> rectangles) {
            this.width = width;
            this.height = height;
            this.rectangles = rectangles;
        }
    }

}

package com.campus.express.algorithm;

import com.campus.express.entity.Package;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于A*算法的校园路径规划
 * 对应项目方案文档：智能路径规划算法设计
 */
@Service
public class CampusPathPlanningService {

    // 校园地图网格：0-可通行，1-障碍物
    private static final int[][] CAMPUS_GRID = {
        {0, 0, 0, 1, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0},
        {0, 1, 0, 0, 0, 1, 0},
        {0, 0, 0, 1, 1, 0, 0},
        {0, 1, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 1, 0, 0, 0}
    };

    private static final Map<String, PathPoint> KEY_LOCATIONS = Map.of(
        "STATION_1", new PathPoint(0, 0),
        "DORM_1", new PathPoint(6, 0),
        "DORM_2", new PathPoint(0, 6),
        "DORM_3", new PathPoint(6, 6),
        "CAFETERIA", new PathPoint(3, 3)
    );

    // 8方向移动
    private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DY = {-1, -1, -1, 0, 0, 1, 1, 1};

    public List<PathPoint> findShortestPath(String startId, String endId) {
        PathPoint start = KEY_LOCATIONS.get(startId);
        PathPoint end = KEY_LOCATIONS.get(endId);

        if (start == null || end == null) {
            throw new IllegalArgumentException("起点或终点不存在: " + startId + " -> " + endId);
        }

        PriorityQueue<PathNode> openList = new PriorityQueue<>();
        Set<PathPoint> closedList = new HashSet<>();
        Map<PathPoint, PathNode> allNodes = new HashMap<>();

        PathNode startNode = new PathNode(start, null, 0, heuristic(start, end));
        openList.add(startNode);
        allNodes.put(start, startNode);

        while (!openList.isEmpty()) {
            PathNode current = openList.poll();
            closedList.add(current.point);

            if (current.point.equals(end)) {
                return reconstructPath(current);
            }

            for (PathPoint neighbor : getNeighbors(current.point)) {
                if (closedList.contains(neighbor) || !isWalkable(neighbor)) continue;

                double moveCost = (Math.abs(neighbor.x() - current.point.x()) +
                    Math.abs(neighbor.y() - current.point.y()) == 2) ? 1.4 : 1.0;
                double gScore = current.gScore + moveCost;

                PathNode neighborNode = allNodes.get(neighbor);
                if (neighborNode == null) {
                    neighborNode = new PathNode(neighbor, current, gScore, heuristic(neighbor, end));
                    openList.add(neighborNode);
                    allNodes.put(neighbor, neighborNode);
                } else if (gScore < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.gScore = gScore;
                    neighborNode.fScore = gScore + neighborNode.hScore;
                    openList.remove(neighborNode);
                    openList.add(neighborNode);
                }
            }
        }
        return Collections.emptyList();
    }

    public PathOptimizationResult optimizePathForDelivery(List<PathPoint> path, Package.PackageSize size) {
        PathOptimizationResult result = new PathOptimizationResult();
        result.setOriginalPath(path);

        List<PathPoint> optimizedPath = path;
        if (size == Package.PackageSize.LARGE) {
            optimizedPath = path.stream()
                .filter(p -> isWideEnough(p, 2.0))
                .collect(Collectors.toList());
            if (optimizedPath.isEmpty()) optimizedPath = path;
        }
        result.setOptimizedPath(optimizedPath);
        result.setTotalDistance(calculatePathDistance(optimizedPath));
        result.setEstimatedTime(estimateDeliveryTime(optimizedPath, size));

        return result;
    }

    public Set<String> getAvailableDestinations() {
        return KEY_LOCATIONS.keySet();
    }

    private double heuristic(PathPoint a, PathPoint b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    private List<PathPoint> reconstructPath(PathNode endNode) {
        List<PathPoint> path = new ArrayList<>();
        PathNode current = endNode;
        while (current != null) {
            path.add(0, current.point);
            current = current.parent;
        }
        return path;
    }

    private List<PathPoint> getNeighbors(PathPoint point) {
        List<PathPoint> neighbors = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            int nx = point.x() + DX[i];
            int ny = point.y() + DY[i];
            if (nx >= 0 && nx < CAMPUS_GRID.length && ny >= 0 && ny < CAMPUS_GRID[0].length) {
                neighbors.add(new PathPoint(nx, ny));
            }
        }
        return neighbors;
    }

    private boolean isWalkable(PathPoint point) {
        return CAMPUS_GRID[point.x()][point.y()] == 0;
    }

    private boolean isWideEnough(PathPoint point, double minWidth) {
        return true; // 简化实现
    }

    private double calculatePathDistance(List<PathPoint> path) {
        double distance = 0;
        for (int i = 1; i < path.size(); i++) {
            PathPoint a = path.get(i - 1);
            PathPoint b = path.get(i);
            double dx = Math.abs(a.x() - b.x());
            double dy = Math.abs(a.y() - b.y());
            distance += (dx + dy == 2) ? 1.4 * 50 : 1.0 * 50; // 每格约50米
        }
        return distance;
    }

    private double estimateDeliveryTime(List<PathPoint> path, Package.PackageSize size) {
        double distance = calculatePathDistance(path);
        double baseSpeed = 80; // 米/分钟
        if (size == Package.PackageSize.LARGE) baseSpeed *= 0.7;
        else if (size == Package.PackageSize.MEDIUM) baseSpeed *= 0.9;
        return distance / baseSpeed;
    }

    @lombok.Data
    private static class PathNode implements Comparable<PathNode> {
        PathPoint point;
        PathNode parent;
        double gScore;
        double hScore;
        double fScore;

        PathNode(PathPoint point, PathNode parent, double gScore, double hScore) {
            this.point = point;
            this.parent = parent;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }

        @Override
        public int compareTo(PathNode o) {
            return Double.compare(this.fScore, o.fScore);
        }
    }

    @lombok.Data
    public static class PathOptimizationResult {
        private List<PathPoint> originalPath;
        private List<PathPoint> optimizedPath;
        private double totalDistance;
        private double estimatedTime;
        private List<String> warnings = new ArrayList<>();
    }
}

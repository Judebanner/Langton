import java.awt.Point;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Grid {
    private final int width;
    private final int height;
    private final Map<Point, Boolean> blackCells;

    public Grid(int width, int height) {
        this.width = width;
        this.height = height;
        this.blackCells = new ConcurrentHashMap<>();
    }

    public boolean getCellState(int x, int y) {
        return blackCells.getOrDefault(new Point(x, y), false);
    }

    public void setCellState(int x, int y, boolean isBlack) {
        Point cell = new Point(x, y);
        if (isBlack) {
            blackCells.put(cell, true);
        } else {
            blackCells.remove(cell);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Set<Point> getAllPoints() {
        return blackCells.keySet();
    }
}

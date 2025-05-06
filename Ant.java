import java.awt.Point;

public class Ant {
    private Point position;
    private Direction direction;

    public Ant(int x, int y) {
        this.position = new Point(x, y);
        this.direction = Direction.NORTH;
    }

    // Thread-safe movement for parallel execution
    public synchronized void move(Grid grid) {
        boolean isBlack = grid.getCellState(position.x, position.y);

        if (isBlack) {
            grid.setCellState(position.x, position.y, false); // Flip to white
            direction = direction.turnLeft();                 // Turn 90° counterclockwise
        } else {
            grid.setCellState(position.x, position.y, true);  // Flip to black
            direction = direction.turnRight();                // Turn 90° clockwise
        }

        // Move forward with wrapping
        int newX = (position.x + direction.dx + grid.getWidth()) % grid.getWidth();
        int newY = (position.y + direction.dy + grid.getHeight()) % grid.getHeight();
        position = new Point(newX, newY);
    }

    public Point getPosition() {
        return position;
    }
}

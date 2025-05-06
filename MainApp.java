

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MainApp extends Application {
    private static final int GRID_SIZE = 200;
    private static final int CELL_SIZE = 4;
    private static final int MAX_STEPS = 20000;
    private static final int MAX_ANTS = 20;
    private static final int TRAIL_LENGTH = 50;

    private final List<Ant> ants = new ArrayList<>();
    private final Map<Ant, Color> antColors = new HashMap<>();
    private final Color[] availableColors = { Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
            Color.CYAN, Color.YELLOW, Color.BROWN, Color.PINK, Color.DARKGRAY };

    private final Grid grid = new Grid(GRID_SIZE, GRID_SIZE);
    private final ForkJoinPool pool = new ForkJoinPool();

    private int step = 0;
    private boolean isRunning = false;

    private Label antCountLabel;
    private Label stepLabel;
    private Canvas canvas;
    private GraphicsContext gc;
    private CheckBox arrowToggle;
    private CheckBox gridLinesToggle;

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
        gc = canvas.getGraphicsContext2D();

        addAntAt(GRID_SIZE / 2, GRID_SIZE / 2);

        Button playPause = new Button("Play");
        playPause.setOnAction(e -> {
            isRunning = !isRunning;
            playPause.setText(isRunning ? "Pause" : "Play");
        });

        Button addAntBtn = new Button("Add Ant");
        addAntBtn.setOnAction(e -> {
            if (ants.size() < MAX_ANTS) {
                int x = (int) (Math.random() * GRID_SIZE);
                int y = (int) (Math.random() * GRID_SIZE);
                addAntAt(x, y);
                draw(gc);
            }
        });

        Button removeAntBtn = new Button("Remove Ant");
        removeAntBtn.setOnAction(e -> {
            if (!ants.isEmpty()) {
                Ant removed = ants.remove(ants.size() - 1);
                antColors.remove(removed);
                draw(gc);
            }
        });

        Button resetBtn = new Button("Reset");
        resetBtn.setOnAction(e -> {
            ants.clear();
            antColors.clear();
            grid.clear();
            step = 0;
            addAntAt(GRID_SIZE / 2, GRID_SIZE / 2);
            draw(gc);
            playPause.setText("Play");
            isRunning = false;
        });

        Button saveBtn = new Button("Save Image");
        saveBtn.setOnAction(e -> {
            WritableImage image = canvas.snapshot(new SnapshotParameters(), null);
            File file = new File("langtons_ant_output.png");
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        Slider speedSlider = new Slider(1, 2000, 100);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);

        arrowToggle = new CheckBox("Show Direction");
        arrowToggle.setSelected(true);

        gridLinesToggle = new CheckBox("Grid Lines");
        gridLinesToggle.setSelected(true);

        antCountLabel = new Label("Ants: " + ants.size());
        stepLabel = new Label("Step: 0");

        HBox controls = new HBox(playPause, addAntBtn, removeAntBtn, resetBtn, saveBtn, arrowToggle, gridLinesToggle,
                speedSlider, antCountLabel, stepLabel);
        controls.setSpacing(10);

        BorderPane root = new BorderPane(canvas, null, null, controls, null);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Langton's Ant - Parallel");
        stage.show();

        AnimationTimer timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (isRunning && now - lastUpdate > (1_000_000_000 / (long) speedSlider.getValue())) {
                    if (step < MAX_STEPS) {
                        pool.invoke(new RegionTask(ants, grid, 1));
                        draw(gc);
                        antCountLabel.setText("Ants: " + ants.size());
                        stepLabel.setText("Step: " + step);
                        step++;
                        lastUpdate = now;
                    } else {
                        stop();
                        playPause.setText("Done");
                    }
                }
            }
        };
        timer.start();
    }

    private void addAntAt(int x, int y) {
        Ant ant = new Ant(x, y);
        ants.add(ant);
        Color color = availableColors[ants.size() % availableColors.length];
        antColors.put(ant, color);
    }

    private void draw(GraphicsContext gc) {
        gc.clearRect(0, 0, GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);

        if (gridLinesToggle.isSelected()) {
            gc.setStroke(Color.LIGHTGRAY);
            for (int i = 0; i <= GRID_SIZE; i++) {
                gc.strokeLine(i * CELL_SIZE, 0, i * CELL_SIZE, GRID_SIZE * CELL_SIZE);
                gc.strokeLine(0, i * CELL_SIZE, GRID_SIZE * CELL_SIZE, i * CELL_SIZE);
            }
        }

        gc.setFill(Color.BLACK);
        for (Point p : grid.getAllPoints()) {
            gc.fillRect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        }

        for (Ant ant : ants) {
            Color color = antColors.getOrDefault(ant, Color.RED);

            // Draw trail
            List<Point> trail = ant.getTrail();
            for (int i = 0; i < trail.size(); i++) {
                Point t = trail.get(i);
                double alpha = 0.1 + 0.9 * (i + 1) / TRAIL_LENGTH;
                gc.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                gc.fillOval(t.x * CELL_SIZE, t.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }

            // Draw ant
            Point pos = ant.getPosition();
            gc.setFill(color);
            gc.fillOval(pos.x * CELL_SIZE, pos.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

            // Draw direction arrow
            if (arrowToggle.isSelected()) {
                gc.setStroke(color);
                double cx = pos.x * CELL_SIZE + CELL_SIZE / 2.0;
                double cy = pos.y * CELL_SIZE + CELL_SIZE / 2.0;
                double dx = cx + ant.getDirection().dx * (CELL_SIZE / 1.5);
                double dy = cy + ant.getDirection().dy * (CELL_SIZE / 1.5);
                gc.strokeLine(cx, cy, dx, dy);
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }

    static class Grid {
        private final int width, height;
        private final Map<Point, Boolean> blackCells = new ConcurrentHashMap<>();

        Grid(int width, int height) {
            this.width = width;
            this.height = height;
        }

        boolean getCellState(int x, int y) {
            return blackCells.getOrDefault(new Point(x, y), false);
        }

        void setCellState(int x, int y, boolean isBlack) {
            Point cell = new Point(x, y);
            if (isBlack)
                blackCells.put(cell, true);
            else
                blackCells.remove(cell);
        }

        Set<Point> getAllPoints() {
            return blackCells.keySet();
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }

        void clear() {
            blackCells.clear();
        }
    }

    static class Ant {
        private Point position;
        private Direction direction;
        private final LinkedList<Point> trail = new LinkedList<>();

        Ant(int x, int y) {
            this.position = new Point(x, y);
            this.direction = Direction.NORTH;
        }

        public synchronized void move(Grid grid) {
            int x = position.x;
            int y = position.y;

            if (trail.size() >= TRAIL_LENGTH)
                trail.removeFirst();
            trail.addLast(new Point(x, y));

            boolean isBlack = grid.getCellState(x, y);
            if (isBlack) {
                grid.setCellState(x, y, false);
                direction = direction.turnLeft();
            } else {
                grid.setCellState(x, y, true);
                direction = direction.turnRight();
            }

            int newX = (x + direction.dx + grid.getWidth()) % grid.getWidth();
            int newY = (y + direction.dy + grid.getHeight()) % grid.getHeight();
            position = new Point(newX, newY);
        }

        public Point getPosition() {
            return position;
        }

        public Direction getDirection() {
            return direction;
        }

        public List<Point> getTrail() {
            return trail;
        }
    }

    enum Direction {
        NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

        final int dx, dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Direction turnRight() {
            return values()[(ordinal() + 1) % 4];
        }

        public Direction turnLeft() {
            return values()[(ordinal() + 3) % 4];
        }
    }

    static class RegionTask extends RecursiveAction {
        private final List<Ant> ants;
        private final Grid grid;
        private final int steps;

        RegionTask(List<Ant> ants, Grid grid, int steps) {
            this.ants = ants;
            this.grid = grid;
            this.steps = steps;
        }

        @Override
        protected void compute() {
            for (int i = 0; i < steps; i++) {
                ants.parallelStream().forEach(ant -> ant.move(grid));
            }
        }
    }
}

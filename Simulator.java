import java.util.List;

public class Simulator {
    private final List<Ant> ants;
    private final Grid grid;
    private final int steps;

    public Simulator(List<Ant> ants, int steps) {
        this.ants = ants;
        this.grid = new Grid();
        this.steps = steps;
    }

    public void runSequential() {
        for (int i = 0; i < steps; i++) {
            for (Ant ant : ants) {
                ant.move(grid);
            }
        }
    }

    public Grid getGrid() {
        return grid;
    }
}
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class RegionTask extends RecursiveAction {
    private final List<Ant> ants;
    private final Grid grid;
    private final int steps;

    public RegionTask(List<Ant> ants, Grid grid, int steps) {
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

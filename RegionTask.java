import java.util.List;
import java.util.concurrent.RecursiveAction;

public class RegionTask extends RecursiveAction {
    private final List<Ant> ants;
    private final Grid grid;
    private final int steps;
    private final int startIndex;
    private final int endIndex;

    private static final int THRESHOLD = 2; // minimum ants per task

    public RegionTask(List<Ant> ants, Grid grid, int steps) {
        this(ants, grid, steps, 0, ants.size());
    }

    private RegionTask(List<Ant> ants, Grid grid, int steps, int startIndex, int endIndex) {
        this.ants = ants;
        this.grid = grid;
        this.steps = steps;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    protected void compute() {
        if (endIndex - startIndex <= THRESHOLD) {
            for (int i = 0; i < steps; i++) {
                for (int j = startIndex; j < endIndex; j++) {
                    synchronized (grid) {
                        ants.get(j).move(grid);
                    }
                }
            }
        } else {
            int mid = (startIndex + endIndex) / 2;
            RegionTask left = new RegionTask(ants, grid, steps, startIndex, mid);
            RegionTask right = new RegionTask(ants, grid, steps, mid, endIndex);
            invokeAll(left, right);
        }
    }
}

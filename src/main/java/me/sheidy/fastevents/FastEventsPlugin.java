package me.sheidy.fastevents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.java.JavaPlugin;
import me.sheidy.fastevents.metrics.Metrics;

public class FastEventsPlugin extends JavaPlugin {

    private final AtomicInteger optiCount = new AtomicInteger(0);

    private FastEvents fastEvents;

    @Override
    public void onEnable() {
        this.fastEvents = new FastEvents(getLogger());

        getServer().getScheduler().runTaskLater(this, this::optimizeEvents, 1);

        setupMetrics();
    }

    private void optimizeEvents() {
        long start = System.currentTimeMillis();

        Map<String, Integer> stats = fastEvents.execute();

        double time = (System.currentTimeMillis() - start) / 1000D;

        List<String> lines = new ArrayList<>();

        int count = 0;
        for (Entry<String, Integer> e : stats.entrySet()) {
            lines.add(e.getKey() + " (" + e.getValue() + ")");
            count += e.getValue();
        }

        optiCount.addAndGet(count);

        getLogger().info("Optimized " + count + " events in " + time + "s");
        getLogger().info("Result: " + String.join(", ", lines));
    }

    private void setupMetrics() {
        try {
            Metrics metrics = new Metrics(this, 9587);
            metrics.addCustomChart(new Metrics.SingleLineChart("optimized_events", () -> optiCount.get()));
        } catch (Exception e) { // just in case >.>
            getLogger().warning("Failed to setup Metrics: " + e.getMessage());
        }
    }
}

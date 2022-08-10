package org.processmining.estminer.specpp.supervision.monitoring;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.processmining.estminer.specpp.supervision.observations.Event;
import org.processmining.estminer.specpp.traits.RepresentsChange;

import java.util.function.ToIntFunction;

public class TimeSeriesMonitor<E extends Event> implements ChartingMonitor<E> {

    private final JFreeChart chart;

    public static <E extends RepresentsChange> AccumulatingToIntWrapper<E> delta_accumulator() {
        return new AccumulatingToIntWrapper<>(RepresentsChange::getDelta);
    }

    private final TimeSeries timeSeries;
    private final ToIntFunction<E> mapper;

    public TimeSeriesMonitor(String label, ToIntFunction<E> mapper) {
        this.mapper = mapper;
        timeSeries = new TimeSeries(label);
        TimeSeriesCollection ts = new TimeSeriesCollection(timeSeries);
        chart = ChartFactory.createTimeSeriesChart("TimeSeries of " + label, "time", label, ts);
    }

    @Override
    public void handleObservation(E observation) {
        int value = mapper.applyAsInt(observation);
        timeSeries.addOrUpdate(new TimeSeriesDataItem(new Millisecond(), value));
    }

    @Override
    public JFreeChart getMonitoringState() {
        return chart;
    }

}
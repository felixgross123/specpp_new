package org.processmining.estminer.specpp.componenting.data;

import org.processmining.estminer.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.estminer.specpp.componenting.system.ComponentType;
import org.processmining.estminer.specpp.componenting.system.Requirement;
import org.processmining.estminer.specpp.datastructures.util.NoRehashing;
import org.processmining.estminer.specpp.datastructures.util.Tuple2;
import org.processmining.estminer.specpp.util.JavaTypingUtils;

public class DataRequirement<T> extends NoRehashing<Tuple2<String, Class<T>>> implements Requirement<DataSource<T>, DataRequirement<?>> {

    @Override
    public ComponentType componentType() {
        return ComponentType.Data;
    }

    @Override
    public Class<DataSource<T>> contentClass() {
        return JavaTypingUtils.castClass(DataSource.class);
    }

    protected final String label;
    protected final Class<T> dataType;

    public DataRequirement(String label, Class<T> dataType) {
        super(new Tuple2<>(label, dataType));
        this.label = label;
        this.dataType = dataType;
    }

    public String getLabel() {
        return label;
    }

    public Class<T> getDataType() {
        return dataType;
    }

    @Override
    public boolean gt(DataRequirement<?> other) {
        return label.equals(other.label) && other.dataType.isAssignableFrom(dataType);
    }

    @Override
    public boolean lt(DataRequirement<?> other) {
        return label.equals(other.label) && dataType.isAssignableFrom(other.dataType);
    }

    public DelegatingDataSource<T> emptyDelegator() {
        return new DelegatingDataSource<>();
    }

    public DelegatingDataSource<T> defaultingDelegator(T defaultData) {
        return new DelegatingDataSource<>(StaticDataSource.of(defaultData));
    }

    public FulfilledDataRequirement<T> fulfilWith(DataSource<T> delegate) {
        return new FulfilledDataRequirement<>(this, delegate);
    }

    @Override
    public String toString() {
        return "DataRequirement(\"" + label + "\", " + dataType.getSimpleName() + ")";
    }
}
package org.processmining.estminer.specpp.datastructures.vectorization;

import org.apache.commons.lang3.ArrayUtils;
import org.processmining.estminer.specpp.datastructures.BitMask;
import org.processmining.estminer.specpp.datastructures.util.IndexedItem;
import org.processmining.estminer.specpp.datastructures.vectorization.spliterators.BitMaskSplitty;
import org.processmining.estminer.specpp.datastructures.vectorization.spliterators.IndexedBitMaskSplitty;
import org.processmining.estminer.specpp.datastructures.vectorization.spliterators.IndexedSplitty;
import org.processmining.estminer.specpp.datastructures.vectorization.spliterators.Splitty;
import org.processmining.estminer.specpp.traits.Copyable;
import org.processmining.estminer.specpp.traits.PartiallyOrdered;
import org.processmining.estminer.specpp.util.StreamUtils;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IntVectorStorage implements Copyable<IntVectorStorage>, Mathable<IntVectorStorage>, Mappable<IntUnaryOperator>, PartiallyOrdered<IntVectorStorage> {


    public static final int MIN_SPLIT_VECTOR_COUNT = 2;
    final int[] startIndices;
    final int[] storage;

    public IntVectorStorage(int[] data, int[] startIndices) {
        this.storage = data;
        this.startIndices = startIndices;
    }

    public static IntVectorStorage zeros(int[] data, int[] lengths) {
        int[] startIndices = ArrayUtils.addFirst(lengths, 0);
        Arrays.parallelPrefix(startIndices, Integer::sum);
        return new IntVectorStorage(data, startIndices);
    }

    public static IntVectorStorage zeros(int[] lengths) {
        int[] startIndices = ArrayUtils.addFirst(lengths, 0);
        Arrays.parallelPrefix(startIndices, Integer::sum);
        return new IntVectorStorage(new int[startIndices[lengths.length]], startIndices);
    }

    public int getTotalSize() {
        return storage.length;
    }

    public int getVectorCount() {
        return startIndices.length - 1;
    }

    protected boolean isValidVectorIndex(int index) {
        return 0 <= index && index < startIndices.length;
    }

    public void setVector(int index, int[] vector) {
        assert isValidVectorIndex(index);
        System.arraycopy(vector, 0, storage, startIndices[index], vector.length);
    }

    public int getVectorLength(int index) {
        assert isValidVectorIndex(index);
        return startIndices[index + 1] - startIndices[index];
    }

    public Spliterator.OfInt getVector(int index) {
        assert isValidVectorIndex(index);
        return Spliterators.spliterator(storage, startIndices[index], startIndices[index + 1], Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.CONCURRENT);
    }

    public void setVectorElement(int index, int elementIndex, int value) {
        assert isValidVectorIndex(index);
        storage[startIndices[index] + elementIndex] = value;
    }

    public void map(IntUnaryOperator mapper) {
        for (int i = 0; i < storage.length; i++) {
            storage[i] = mapper.applyAsInt(storage[i]);
        }
    }

    public void mapVector(int index, IntUnaryOperator mapper) {
        assert isValidVectorIndex(index);
        for (int i = startIndices[index]; i < startIndices[index + 1]; i++) {
            storage[i] = mapper.applyAsInt(storage[i]);
        }
    }

    public void feedInto(IntConsumer elementConsumer) {
        Arrays.stream(storage).forEach(elementConsumer);
    }

    public void feedInto(Consumer<IntStream> vectorConsumer) {
        view().forEach(vectorConsumer);
    }

    public void feedInto(IntStream indices, Consumer<IntStream> vectorConsumer) {
        view(indices).forEach(vectorConsumer);
    }

    public void differencing() {
        int startIndex = 0;
        for (int i = 0; i < getVectorCount(); ) {
            int endIndex = startIndices[++i];
            int last = storage[startIndex];
            for (int j = startIndex + 1; j < endIndex; j++) {
                int temp = storage[j];
                storage[j] = temp - last;
                last = temp;
            }
            startIndex = endIndex;
        }
    }

    public IntStream viewVector(int index) {
        assert isValidVectorIndex(index);
        return Arrays.stream(storage, startIndices[index], startIndices[index + 1]);
    }

    public IntStream indexStream() {
        return IntStream.range(0, getVectorCount());
    }

    public Stream<IntStream> view(IntStream indices) {
        return indices.mapToObj(this::viewVector);
    }

    public Stream<IndexedItem<IntStream>> viewIndexed(IntStream indices) {
        return indices.mapToObj(i -> new IndexedItem<>(i, viewVector(i)));
    }

    public Stream<IntStream> view() {
        return view(indexStream());
    }

    public Stream<IndexedItem<IntStream>> viewIndexed() {
        return viewIndexed(indexStream());
    }

    public IntStream vectorwisePredicateStream(Predicate<IntStream> predicate) {
        return vectorwisePredicateStream(indexStream(), predicate);
    }


    public IntStream vectorwisePredicateStream(IntStream indices, Predicate<IntStream> predicate) {
        return indices.filter(i -> predicate.test(viewVector(i)));
    }

    @Override
    public IntVectorStorage copy() {
        return new IntVectorStorage(Arrays.copyOf(storage, storage.length), startIndices);
    }

    public void add(IntVectorStorage other) {
        assert Arrays.equals(startIndices, other.startIndices);
        for (int i = 0; i < storage.length; i++) {
            storage[i] += other.storage[i];
        }
    }

    @Override
    public void subtract(IntVectorStorage other) {
        assert Arrays.equals(startIndices, other.startIndices);
        for (int i = 0; i < storage.length; i++) {
            storage[i] -= other.storage[i];
        }
    }

    @Override
    public void negate() {
        for (int i = 0; i < storage.length; i++) {
            storage[i] = -storage[i];
        }
    }

    @Override
    public String toString() {
        return StreamUtils.stringify(view());
    }


    @Override
    public boolean gt(IntVectorStorage other) {
        return IVSComputations.gtOn(indexStream(), this, other.indexStream(), other);
    }

    @Override
    public boolean lt(IntVectorStorage other) {
        return IVSComputations.ltOn(indexStream(), this, other.indexStream(), other);
    }

    public Spliterator<IntStream> spliterator() {
        return new Splitty(this, 0, getVectorCount());
    }

    public Spliterator<IndexedItem<IntStream>> indexedSpliterator() {
        return new IndexedSplitty(this, 0, getVectorCount(), IntUnaryOperator.identity());
    }

    public Spliterator<IntStream> spliterator(BitMask bitMask) {
        assert bitMask.length() <= getVectorCount();
        return new BitMaskSplitty(this, bitMask, 0, bitMask.cardinality());
    }

    public Spliterator<IndexedItem<IntStream>> indexedSpliterator(BitMask bitMask) {
        assert bitMask.length() <= getVectorCount();
        return new IndexedBitMaskSplitty(this, bitMask, 0, bitMask.cardinality(), IntUnaryOperator.identity());
    }

}

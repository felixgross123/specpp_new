package org.processmining.estminer.specpp.datastructures.vectorization;

import org.processmining.estminer.specpp.datastructures.BitMask;
import org.processmining.estminer.specpp.traits.Copyable;

import java.util.Arrays;
import java.util.stream.IntStream;

public class IntVector implements Copyable<IntVector> {

    private final int[] internal;
    private final int total;


    protected IntVector(int[] internal, int sum) {
        this.internal = internal;
        total = sum;
    }

    public static IntVector of(int[] frequencies) {
        return new IntVector(Arrays.copyOf(frequencies, frequencies.length), Arrays.stream(frequencies).sum());
    }

    @Override
    public IntVector copy() {
        return new IntVector(internal, total);
    }

    public IntStream view() {
        return Arrays.stream(internal, 0, internal.length);
    }

    public IntStream view(BitMask mask) {
        return mask.stream().map(i -> internal[i]);
    }

    public int sum() {
        return view().sum();
    }

    public int sum(BitMask mask) {
        return view(mask).sum();
    }

    public int get(int index) {
        return internal[index];
    }

    public int length() {
        return internal.length;
    }

    public int argMax() {
        int currentMax = -1, currentMaxIndex = -1;
        for (int i = 0; i < internal.length; i++) {
            if (internal[i] > currentMax) {
                currentMax = internal[i];
                currentMaxIndex = i;
            }
        }
        return currentMaxIndex;
    }

}

/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.ai.ndarray.types;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import software.amazon.ai.util.Pair;
import software.amazon.ai.util.PairList;

/** A class the presents the {@link software.amazon.ai.ndarray.NDArray}'s shape information. */
public class Shape {

    private long[] shape;
    private LayoutType[] layout;

    /**
     * Constructs and initializes a {@code Shape} with specified dimension as {@code (long...
     * shape)}.
     *
     * @param shape dimensions of the shape
     * @throws IllegalArgumentException Thrown if any element in Shape is invalid. It should not be
     *     less than -1. Also thrown if the shape and layout do not have equal sizes.
     */
    public Shape(long... shape) {
        this(
                shape,
                Arrays.stream(shape).mapToObj(x -> LayoutType.UNKNOWN).toArray(LayoutType[]::new));
    }

    /**
     * Constructs and initializes a {@code Shape} with specified shape and layout pairList.
     *
     * @param shape dimensions and layout of the shape
     * @throws IllegalArgumentException Thrown if any element in Shape is invalid. It should not be
     *     less than -1 .Also thrown if the shape and layout do not have equal sizes.
     */
    public Shape(PairList<Long, LayoutType> shape) {
        this(
                shape.keys().stream().mapToLong(l -> l).toArray(),
                shape.values().toArray(new LayoutType[shape.size()]));
    }

    /**
     * Constructs and initializes a {@code Shape} with specified dimension and layout.
     *
     * @param shape the size of each axis of the shape
     * @param layout the {@link LayoutType} of each axis in the shape
     * @throws IllegalArgumentException Thrown if any element in Shape is invalid. It should not be
     *     less than -1. Also thrown for an invalid layout. Also thrown if the shape and layout do
     *     not have equal sizes.
     */
    public Shape(long[] shape, String layout) {
        this(shape, LayoutType.fromValue(layout));
    }

    /**
     * Constructs and initializes a {@code Shape} with specified dimension and layout.
     *
     * @param shape the size of each axis of the shape
     * @param layout the {@link LayoutType} of each axis in the shape
     * @throws IllegalArgumentException Thrown if any element in Shape is invalid. It should not be
     *     less than -1. Also thrown if the shape and layout do not have equal sizes.
     */
    public Shape(long[] shape, LayoutType[] layout) {
        if (Arrays.stream(shape).anyMatch(s -> s < -1)) {
            throw new IllegalArgumentException("The shape must be >= -1");
        }
        if (shape.length != layout.length) {
            throw new IllegalArgumentException("The shape and layout must have the same length");
        }
        this.shape = shape;
        this.layout = layout;
    }

    /**
     * Returns dimensions of the {@code Shape}.
     *
     * @return dimensions of the {@code Shape}
     */
    public long[] getShape() {
        return shape;
    }

    /**
     * Returns the shape in the given dimension.
     *
     * @param dimension the dimension to get the shape in
     * @return Returns the shape in the given dimension
     */
    public long get(int dimension) {
        return shape[dimension];
    }

    /**
     * Returns the size of a specific dimension or several specific dimensions.
     *
     * @param dimensions The dimension or dimensions to find the size of
     * @return size of specific dimension(s) or -1 for indeterminate size
     * @throws IllegalArgumentException thrown if passed an invalid dimension
     */
    public long size(int... dimensions) {
        int total = 1;
        for (int d : dimensions) {
            if (d < 0 || d >= shape.length) {
                throw new IllegalArgumentException("Invalid dimension " + d);
            }
            if (shape[d] == -1) {
                return -1;
            }
            total *= shape[d];
        }
        return total;
    }

    /**
     * Returns the total size.
     *
     * @return total size or -1 for indeterminate size
     */
    public long size() {
        int total = 1;
        for (long v : shape) {
            if (v == -1) {
                return -1;
            }
            total *= v;
        }
        return total;
    }

    /**
     * Returns the number of dimensions of this {@code Shape}.
     *
     * @return number of dimensions of this {@code Shape}.
     */
    public int dimension() {
        return shape.length;
    }

    /**
     * Return the count of unknown value in this {@code Shape}.
     *
     * @return number of unknown value in this {@code Shape}
     */
    public long getUnknownValueCount() {
        return Arrays.stream(shape).filter(s -> s == -1).count();
    }

    /**
     * Creates a new {@code Shape} whose content is a slice of this shape.
     *
     * <p>The sub shape begins at the specified {@code beginIndex} and extends to {@code endIndex -
     * 1}.
     *
     * @param beginIndex the beginning index, inclusive.
     * @return a new {@code Shape} whose content is a slice of this shape
     */
    public Shape slice(int beginIndex) {
        return slice(beginIndex, shape.length);
    }

    /**
     * Creates a new {@code Shape} whose content is a slice of this shape.
     *
     * <p>The sub shape begins at the specified {@code beginIndex} and extends to {@code endIndex -
     * 1}.
     *
     * @param beginIndex the beginning index, inclusive.
     * @param endIndex the ending index, exclusive.
     * @return a new {@code Shape} whose content is a slice of this shape
     */
    public Shape slice(int beginIndex, int endIndex) {
        int size = endIndex - beginIndex;
        long[] out = new long[size];
        System.arraycopy(shape, beginIndex, out, 0, size);
        return new Shape(out);
    }

    /**
     * Returns only the axes of the Shape whose layout types match the predicate.
     *
     * @param predicate Predicate returning true to Shape axis to be part of the filtered Shape
     * @return Returns a new filtered Shape
     */
    public Shape filterByLayoutType(Predicate<LayoutType> predicate) {
        return new Shape(
                new PairList<>(
                        this.stream()
                                .filter(pair -> predicate.test(pair.getValue()))
                                .collect(Collectors.toList())));
    }

    /**
     * Returns a mapped shape.
     *
     * @param mapper The function to map each element of the Shape by
     * @return Returns a new filtered Shape
     */
    public Shape map(Function<Pair<Long, LayoutType>, Pair<Long, LayoutType>> mapper) {
        return new Shape(
                new PairList<>(
                        this.stream()
                                .map(pair -> mapper.apply(pair))
                                .collect(Collectors.toList())));
    }

    /**
     * Returns a stream of the Shape.
     *
     * @return Returns the stream
     */
    public Stream<Pair<Long, LayoutType>> stream() {
        return new PairList<>(
                        Arrays.stream(shape).boxed().collect(Collectors.toList()),
                        Arrays.asList(layout))
                .stream();
    }

    /**
     * Joins a this shape with specified {@code other} shape.
     *
     * @param other the shape the join
     * @return joined {@code Shape}
     */
    public Shape addAll(Shape other) {
        return new Shape(
                LongStream.concat(Arrays.stream(shape), Arrays.stream(other.shape)).toArray());
    }

    /**
     * Returns the head index of the shape.
     *
     * @return the head index of the shape
     * @throws IndexOutOfBoundsException Thrown if the shape is empty
     */
    public long head() {
        // scalar case
        if (shape.length == 0) {
            throw new IndexOutOfBoundsException("can't get value from scalar shape.");
        }
        return shape[0];
    }

    /**
     * Returns the number of trailing ones in the array shape.
     *
     * <p>For example, a rank 3 array with shape [10, 1, 1] would return 2 for this method
     *
     * @return Number of trailing ones in shape
     */
    public int getTrailingOnes() {
        for (int i = 0; i < shape.length; i++) {
            if (shape[shape.length - i - 1] != 1) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns the number of leading ones in the array shape.
     *
     * <p>For example, a rank 3 array with shape [1, 10, 1] would return value 1 for this method
     *
     * @return Number of leading ones in shape
     */
    public int getLeadingOnes() {
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] != 1) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns the number of columns in the matrix.
     *
     * @return the number of columns in the matrix
     * @throws IllegalStateException Thrown if not a 2D matrix
     */
    public long columns() {
        if (!isMatrix()) {
            throw new IllegalStateException("Not a Matrix");
        }
        return shape[1];
    }

    /**
     * Returns the number of rows in the matrix.
     *
     * @return the number of rows in the matrix
     * @throws IllegalStateException Thrown if not a 2D matrix
     */
    public long rows() {
        if (!isMatrix()) {
            throw new IllegalStateException("Not a Matrix");
        }
        return shape[0];
    }

    /**
     * Returns {@code true} if this NDArray is a matrix and the number of columns is 1.
     *
     * @return {@code true} if NDArray is a matrix and the number of columns is 1
     */
    public boolean isColumnVector() {
        return isMatrix() && columns() == 1 && size() > 1;
    }

    /**
     * Returns {@code true} if this NDArray is a matrix and the number of rows is 1.
     *
     * @return {@code true} if this NDArray is a matrix and the number of rows is 1
     */
    public boolean isRowVector() {
        return isMatrix() && rows() == 1 && size() > 1;
    }

    /**
     * Returns {@code true} if this NDArray is a vector matrix.
     *
     * @return whether this NDArray is a vector matrix
     */
    public boolean isVectorMatrix() {
        return isColumnVector() || isRowVector();
    }

    /**
     * Returns whether the matrix has the same rows and columns.
     *
     * @return {@code true} if the matrix has the same rows and columns {@code false} otherwise
     */
    public boolean isSquare() {
        return isMatrix() && columns() == rows();
    }

    /**
     * Returns {@code true} if the NDArray is a matrix.
     *
     * @return whether the NDArray is a matrix
     */
    public boolean isMatrix() {
        return dimension() == 2;
    }

    /**
     * Returns {@code true} if the NDArray is a scalar.
     *
     * @return whether the NDArray is a scalar
     */
    public boolean isScalar() {
        return dimension() == 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Shape shape1 = (Shape) o;
        return Arrays.equals(shape, shape1.shape);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Arrays.hashCode(shape);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < shape.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(shape[i]);
        }
        sb.append(')');
        return sb.toString();
    }
}

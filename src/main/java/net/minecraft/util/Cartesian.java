package net.minecraft.util;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;

public class Cartesian {
    public static <T> Product cartesianProduct(Class<T> clazz, Iterable<? extends Iterable<? extends T>> sets) {
        return new Cartesian.Product<>(clazz, toArray(sets));
    }

    public static <T> Iterable<List<T>> cartesianProduct(Iterable<? extends Iterable<? extends T>> sets) {
        return arraysAsLists(cartesianProduct(Object.class, sets));
    }

    private static <T> Iterable arraysAsLists(Iterable<Object[]> arrays) {
        return Iterables.transform(arrays, new Cartesian.GetList<>());
    }

    private static <T> T[] toArray(Iterable<? extends T> it) {
        List<T> list = Lists.newArrayList();

        for (T t : it) {
            list.add(t);
        }

        return list.toArray(createArray((Class<? super T>) Iterable.class, list.size()));
    }

    private static <T> T[] createArray(Class<? super T> p_179319_0_, int p_179319_1_) {
        return (T[]) Array.newInstance(p_179319_0_, p_179319_1_);
    }

    static class GetList<T> implements Function<Object[], List<T>> {
        private GetList() {
        }

        public List<T> apply(Object[] p_apply_1_) {
            return Arrays.asList((T[]) p_apply_1_);
        }
    }

    static class Product<T> implements Iterable<T[]> {
        private final Class<T> clazz;
        private final Iterable<? extends T>[] iterables;

        private Product(Class<T> clazz, Iterable<? extends T>[] iterables) {
            this.clazz = clazz;
            this.iterables = iterables;
        }

        public @NotNull Iterator<T[]> iterator() {

            Iterator<T[]> iterator;
            if (this.iterables.length <= 0) {
                iterator = Collections.singletonList(Cartesian.createArray(this.clazz, 0)).iterator();
            } else {
                iterator = new Cartesian.Product.ProductIterator<>(this.clazz, this.iterables);
            }
            return iterator;
        }

        static class ProductIterator<T> extends UnmodifiableIterator<T[]> {
            private int index;
            private final Iterable<? extends T>[] iterables;
            private final Iterator<? extends T>[] iterators;
            private final T[] results;

            private ProductIterator(Class<T> clazz, Iterable<? extends T>[] iterables) {
                this.index = -2;
                this.iterables = iterables;
                this.iterators = Cartesian.createArray(Iterator.class, this.iterables.length);

                for (int i = 0; i < this.iterables.length; ++i) {
                    this.iterators[i] = iterables[i].iterator();
                }

                this.results = Cartesian.createArray(clazz, this.iterators.length);
            }

            private void endOfData() {
                this.index = -1;
                Arrays.fill(this.iterators, null);
                Arrays.fill(this.results, null);
            }

            public boolean hasNext() {
                if (this.index == -2) {
                    this.index = 0;

                    for (Iterator<? extends T> iterator1 : this.iterators) {
                        if (!iterator1.hasNext()) {
                            this.endOfData();
                            break;
                        }
                    }

                    return true;
                } else {
                    if (this.index >= this.iterators.length) {
                        for (this.index = this.iterators.length - 1; this.index >= 0; --this.index) {
                            Iterator<? extends T> iterator = this.iterators[this.index];

                            if (iterator.hasNext()) {
                                break;
                            }

                            if (this.index == 0) {
                                this.endOfData();
                                break;
                            }

                            iterator = this.iterables[this.index].iterator();
                            this.iterators[this.index] = iterator;

                            if (!iterator.hasNext()) {
                                this.endOfData();
                                break;
                            }
                        }
                    }

                    return this.index >= 0;
                }
            }

            public T[] next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                } else {
                    while (this.index < this.iterators.length) {
                        this.results[this.index] = this.iterators[this.index].next();
                        ++this.index;
                    }

                    return this.results.clone();
                }
            }
        }
    }
}

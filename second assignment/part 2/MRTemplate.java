package MyMapReduce;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Marco Costa
 *
 * This class provides a parametric MapReduce framework realized using the Template Method design pattern according
 *  to the text of the exercise.
 * In order to instantiate this the `read`, `map`, `compare`, `reduce` and `write` abstract methods must be
 *  overridden by the deriving class.
 * The `map` of the framework instantiation must not produce a List as a result of the computation, since the grouping
 *  of the results is implemented by this class as a `frozen spot` of the template.
 *
 * Logically, the MapReduce framework pipeline works as follows:
 *  `read`   provides the computation input as a Stream of Key/Values pairs
 *  `map`    takes the computation input and applies a map computation to each pair, providing a new Stream of
 *           Key/(Multiple)Values pairs, possibly of different types
 *  `reduce` applies a binary function to all the elements with the same key resulting in a Stream of
 *           Key/SingleValue pairs, the new value could possibly be of different type than the function input
 *  `write`  outputs the result of the computation
 *
 * @implNote this implementation is not parallel and/or distributed
 *
 * @param <K1> the key type result of the `read` function
 * @param <V1> the value type           "
 * @param <K2> the key type result of the `map` function (must extend Comparable in order to apply sorting to the keys)
 * @param <V2> the value type           "                (NOT as List)
 * @param <V3> the value result of the binary reduce function (a -> b -> b), possibly the same as V2
 */
public abstract class MRTemplate <K1, V1, K2 extends Comparable, V2, V3> {

    /**
     * The `execute` method applies the concatenation of the MapReduce operations, from `read` to `write`.
     * The resulting Stream of the `group` method is sorted according to its keys of type K2, the result of the
     *  computation ``should`` be maintained sorted by the framework instantiation since the Template cannot
     *  guarantee a sorted Stream as a result of the `result` and `write` operations.
     *
     * NOTE: This method is the only one exposed as public.
     */
    public final void execute() {
        write(reduce(group(map(read())).sorted((a, b) -> this.compare(a.getKey(), b.getKey()))));
    }

    /**
     * Groups the Stream result of the `map` operation from a set of pair of identical keys {(k,v_i) | forall i}
     * to a single pair k/l where l is the list of values with k as a key, **foreach** key k.
     * ex.
     *  (k1, v1), (k1, v2), (k2, v1), (k1, v3) -> (k1, [v1, v2, v3]), (k2, [v1])
     *
     * @param s the stream result of the `map` operation
     * @return the grouped Stream as described above
     */
    private Stream<Pair<K2, List<V2>>> group(Stream<Pair<K2, V2>> s) {
        ArrayList<Pair<K2, List<V2>>> out = new ArrayList<>();

        /**
         * To make the grouping according to the keys we exploit the
         *  Collectors.groupingBy() method, which takes as input the field where to apply the grouping (the key) and
         *  a downstream Collector used to apply a reduction function on the values associated with the key.
         * Hence, the method constructs a List<V2> of the values associated with the grouped key and for each one
         *  adds them in an ArrayList of Pairs(K2, List(V2))
         */
        s.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())))
                .entrySet() /* constructs a Set<Map.Entry<K2, List<V2>> */
                .forEach((e) -> out.add(new Pair<>(e.getKey(), e.getValue())));

        return out.stream();
    }
    
    abstract protected Stream<Pair<K1, V1>> read();
    abstract protected Stream<Pair<K2, V2>> map(Stream<Pair<K1,V1>> s);
    abstract protected int compare(K2 a, K2 b);
    abstract protected Stream<Pair<K2,V3>> reduce(Stream<Pair<K2, List<V2>>> s);
    abstract protected void write(Stream<Pair<K2, V3>> s);
    
}

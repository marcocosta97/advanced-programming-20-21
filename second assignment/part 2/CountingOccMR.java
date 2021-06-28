package MyMapReduce;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;

/**
 * This class instantiates the MapReduce MRTemplate template class by means of the intermediate TextFileMR implementation,
 *  counting the occurrences of words (with length > 3) in a set of .txt files in the same path (provided at construction time)
 *  as specified in the exercise text.
 * The result is provided in a .csv file outputted in the same path given as input.
 *
 * @author Marco Costa
 * @implNote the Stream API is used whenever possible
 */
public class CountingOccMR extends TextFileMR <Integer, Integer> {
    private String OUT_FILE_NAME = "count.csv";

    /**
     * Constructor of the class.
     *
     * @throws IllegalArgumentException if the argument is invalid
     * @param path the absolute pathname of the input files
     */
    public CountingOccMR(String path) {
        super(path);
        super.OUT_FILE_NAME = this.OUT_FILE_NAME;
    }

    /**
     * The `map` method converts the Stream given by the pairs (filename, list of words in each line of the file)
     * to the stream of pairs (word in each line (> 3 char), number of occurrences of the word **in that line**)
     * ex.
     *  (f1.txt, ["this is a line line", "this is a line"]) ->  ("this", 1), ("line", 2), ("this", 1), ("line", 1)
     *
     * @param s the input Stream
     * @return the output Stream described above
     */
    @Override
    protected Stream<Pair<String, Integer>> map(Stream<Pair<String, List<String>>> s) {
        final ArrayList<Pair<String, Integer>> ar = new ArrayList<>();

        s.forEach((a) -> a.getValue().forEach((str) -> {
            /**
             * First of all, all the words in the document must be "normalized", in order to do this
             *  we remove all the non alphabetic characters from the String representing a single line of the document,
             *  put them on lower case and we split it in an array of Strings with one word per position.
             */
            String[] split = str.replaceAll("[^a-zA-Z ]", "")
                    .toLowerCase()
                    .split("\\s+");
            /**
             * For each word in the line, we filter it the ones with invalid length and, exploiting the groupingBy
             *  method we insert each word in an HashMap<String, Long> where the value associated with the key
             *  (the word) is the count of occurrences found.
             * Then, all the Pairs (word, occurrences) are inserted in an ArrayList.
             */
            Arrays.stream(split)
                    .filter(p -> p.length() > 3) /* removes the words with invalid length */
                    /**
                     * using a TreeMap would've returned the pair already lexicographically ordered in O(n*log n),
                     *  but in order to match the exercise constraints we use an HashMap leaving the ordering
                     *  to the Template class.
                     */
                    .collect(Collectors.groupingBy(Function.<String>identity(), HashMap::new, counting()))
                    .entrySet()
                    .forEach((e) -> ar.add(new Pair<>(e.getKey(), e.getValue().intValue())));
        }));

        return ar.stream();
    }

    /**
     * The reduction function sums all the values in the List of occurrences found for each word key and returns
     *  a Stream of Pairs given by the key word and the **total** number of occurrences found on all the files.
     * ex.
     *  ("lorem", [1, 3, 1]), ("ipsum", [1, 1, 4]) -> ("lorem", 5), ("ipsum", 6)
     *
     * @param s the input Stream
     * @return the output Stream as described above
     */
    @Override
    protected Stream<Pair<String, Integer>> reduce(Stream<Pair<String, List<Integer>>> s) {
        ArrayList<Pair<String, Integer>> out = new ArrayList<>();

        /**
         * Each list is reduced by means of the (+) operator and the result added in an ArrayList of Pairs.
         */
        s.forEach(a -> {
            int res = a.getValue().stream().reduce((b, c) -> b + c).get();
            out.add(new Pair<>(a.getKey(), res));
        });

        return out.stream();
    }

    /**
     * Main Class for testing the Counting Occurrences by command line.
     *
     * @param args
     */
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.print("Insert path: ");
        String path = scan.nextLine();

        MRTemplate t = new CountingOccMR(path);
        t.execute();
    }
}

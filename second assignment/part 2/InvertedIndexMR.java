package MyMapReduce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class instantiates the MapReduce MRTemplate template class by means of the intermediate TextFileMR implementation,
 *  producing an inverted index for all the words (with length > 3) in a set of .txt files in the same path
 *  (provided at construction time) as specified in the exercise text.
 * The result is provided in a .csv file outputted in the same path given as input.
 *
 * @author Marco Costa
 * @implNote the Stream API is used whenever possible
 */
public class InvertedIndexMR extends TextFileMR <String, String> {
    private String OUT_FILE_NAME = "inverted_index.csv";

    public InvertedIndexMR(String path) {
        super(path);
        super.OUT_FILE_NAME = this.OUT_FILE_NAME;
    }

    /**
     * The `map` method converts the Stream given by the pairs (filename, list of words in each line of the file)
     * to the stream of pairs (word in each line (> 3 char), no of occurrences of the word **in that line**)
     * ex.
     *  ("f1.txt", ["this is a line line", "this is a line"]) ->  ("this", "(f1.txt, 1)"), ("line", "(f1.txt, 1)"),
     *                                                            ("this", "(f1.txt, 2)"), ("line", "(f1.txt, 2)")
     *
     * NOTE: the output pairs for a same word are ordered wrt. 1. filename 2. line in the file
     * @param s the input Stream
     * @return the output Stream described above
     */
    @Override
    protected Stream<Pair<String, String>> map(Stream<Pair<String, List<String>>> s) {
        final ArrayList<Pair<String, String>> ar = new ArrayList<>();

        s.forEach((a) -> {
            List<String> lines = a.getValue();
            /* An IntStream stream must be used in order to keep track of the number of previous lines -> 'i' */
            IntStream.range(0, lines.size()).forEach(i -> {
                /**
                 * First of all, all the words in the document must be "normalized", in order to do this
                 *  we remove all the non alphabetic characters from the String representing a single line of the document,
                 *  put them on lower case and we split it in an array of Strings with one word per position.
                 */
                String[] split = lines.get(i).replaceAll("[^a-zA-Z ]", "")
                        .toLowerCase()
                        .split("\\s+");
                /**
                 * For each word in the line, we filter it the ones with invalid length and add it in a new Pair
                 *  (word, "(document name, line of word in the document)")
                 */
                Arrays.stream(split)
                        .filter(j -> (j.length() > 3)) /* removes the words with invalid length */
                        .forEach(j -> ar.add(new Pair<>(j, "(" + a.getKey() + ", " + (i + 1) + ")")));

            });
        });

        /* the stream does not need any further ordering since the filenames are collected already sorted and the lines
           are iterated */
        return ar.stream();
    }

    /**
     * The reduction function simply concatenates all the strings "(filename, line of word)" under a same word in a
     *  sorted manner.
     * ex.
     *  ("lorem", ["(f1.txt, 5)", "(f1.txt, 7)", "(f2.txt, 2")] -> ("lorem", "(f1.txt, 5) (f1.txt, 7) (f2.txt, 2)")
     *
     * @param s the input stream
     * @return the output stream
     */
    @Override
    protected Stream<Pair<String, String>> reduce(Stream<Pair<String, List<String>>> s) {
        ArrayList<Pair<String, String>> out = new ArrayList<>();

        /**
         * Each list is reduced by means of the (+) operator and the result added in an ArrayList of Pairs.
         */
        s.forEach(a -> {
            String res = a.getValue().stream().reduce((b, c) -> b + " " + c).get();
            out.add(new Pair<>(a.getKey(), res));
        });

        return out.stream();
    }

    /**
     * Main Class for testing the Inverted Index creation by command line.
     *
     * @param args
     */
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        System.out.print("Insert path: ");
        String path = scan.nextLine();

        MRTemplate t = new InvertedIndexMR(path);
        t.execute();
    }
}

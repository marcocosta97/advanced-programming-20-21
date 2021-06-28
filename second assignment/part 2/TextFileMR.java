package MyMapReduce;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Stream;

/**
 * This works as an intermediate common implementation of the MapReduce template class of the `read`, `write` and `compare`
 *  abstract methods when a list of .txt files must be read resulting in a Stream of Pairs (filename, [lines, of, file])
 *  and the result of the overall computation outputted in a .csv file as specified in the exercise text.
 * Since this class does not provide a full implementation of the MapReduce computation, it is parametric in the
 *  V2 and V3 types, which are respectively the resulting value types of `map` and `reduce`.
 *
 * @author Marco Costa
 * @param <V2> the value type result of the `map` function
 * @param <V3> the value type result of the `reduce` function
 */
public abstract class TextFileMR <V2, V3> extends MRTemplate<String, List<String>, String, V2, V3> {
    protected String OUT_FILE_NAME = "out.csv";
    private final File pathFile;

    /**
     * Constructor of the class. Takes as input the path of the .txt input files.
     *
     * @param path the input path
     */
    public TextFileMR(String path) {
        if (path == null) throw new IllegalArgumentException("the path cannot be null");
        pathFile = new File(path);

        if (!pathFile.exists() /*|| !pathFile.isAbsolute()*/ || !pathFile.isDirectory()
                || !pathFile.canRead() || !pathFile.canWrite())
            throw new IllegalArgumentException("path provided is not valid");
    }

    /**
     * Reads the input files, returning the Stream of Pairs (filename, lines of the document as a List)
     *
     * @return the stream described above
     */
    @Override
    protected Stream<Pair<String, List<String>>> read() {
        Reader r = new Reader(pathFile.toPath());
        try {
            Stream<Pair<String, List<String>>> read = r.read();
            return read;
        } catch (IOException e) {
            throw new IllegalArgumentException("can't read" + e.getMessage());
        }
    }

    /**
     * Compares two Strings lexicographically and returns an integer value as described in the comparison methods.
     *
     * @param a the first String to be compared
     * @param b the second String
     * @return the value 0 if the argument string is equal to this string;
     * a value less than 0 if this string is lexicographically less than the string argument;
     * and a value greater than 0 if this string is lexicographically greater than the string argument
     */
    @Override
    protected int compare(String a, String b) {
        return a.compareTo(b);
    }

    /**
     * Writes the resulting Stream in a .csv file with name ``out`` created inside the provided path.
     *
     * @param s the final Stream to be outputted
     * @implNote if the file out.csv already exists in the path it will e overwritten
     */
    @Override
    protected void write(Stream<Pair<String, V3>> s) {
        try {
            File out = new File(pathFile.getAbsolutePath() + "/" + OUT_FILE_NAME);
            out.createNewFile();

            PrintStream ps = new PrintStream(out);
            s.forEach(p -> ps.println(p.getKey() + ", " + p.getValue()));
            ps.close();
            System.out.println("Results correctly outputted in " + out);
        } catch (FileNotFoundException e) {
            /* unexpected since the existence of the file should've already been checked before the method Writer.write(..) */
            throw new RuntimeException("unexpected exception: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("can't create the output file" + e.getMessage());
        }
    }

    @Override
    protected abstract Stream<Pair<String, V2>> map(Stream<Pair<String, List<String>>> s);
    @Override
    protected abstract Stream<Pair<String, V3>> reduce(Stream<Pair<String, List<V2>>> s);
}

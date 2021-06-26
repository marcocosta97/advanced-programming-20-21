/*
 *
 */
package it.mcosta.xmlserialization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 
 * This class provides both serialization and deserialization of Java objects
 *  using introspection. The class to be serialized/deserialized must annotated with 
 *  `@XMLable` as annotation of the class itself and `@XMLfield` for identifying
 *  all the fields to be serialized (this is possible only with primitive types
 *  or strings, which must be described using the String `type` property).
 * 
 * NOTE: while a `partial` object can be serialized this is not true for
 *       deserialization, to be deserialized an object must have all its fields
 *       annotated with XMLfield. Since it won't be possible to reconstruct
 *       the initial object.
 *       ex. an object with class
 *       class C {
 *          private int value;
 *          
 *          public C() { value = rand() }
 *       }
 *       cannot be reconstructed if `value` is not @XMLfield annotated
 * 
 * @author Marco Costa
 * 
 */
public class XMLSerializer {
    /**
     * XMLable is used to indicate a Class is serializable/deserializable
     * Classes without this annotation won't be 
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME) // needs to be retained at runtime and not discarded by the vm/compiler
    public @interface XMLable {}
    
    /**
     * XMLfield indicates a serializable field
     * The type String values admitted are:
     *  - "int", "float", "boolean", "String" 
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface XMLfield {
        String type ();
        String name() default "";
    }

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static class Wrapper {
        Field field;
        XMLfield xmlField;
        String fieldName;

        private Wrapper(Field field, XMLfield xmlField, String fieldName) {
            this.field = field;
            this.xmlField = xmlField;
            this.fieldName = fieldName;
        }
    }

    /**
     * Takes an array of Field for and returns an ArrayList containing all the
     *  fields correctly annotated and with `accessibleFlag` set to true.
     * The fields are wrapped inside a Wrapper class, which also contains
     *  the XMLField annotation and the fieldName. If the `name` property of
     *  XMLField is defined it will be used as fieldName.
     * 
     * @param declaredFields the array of Field
     * @return the ArrayList of Wrapper objects
     */
    private static ArrayList<Wrapper> getValidFields(Field[] declaredFields) {
        ArrayList<Wrapper> validFields = new ArrayList<>();

        for (Field f : declaredFields) {
            /* discarding the field if no annotation is present */
            XMLfield fieldAnnotation = f.getAnnotation(XMLfield.class);
            if (fieldAnnotation == null)
                continue;
            
            /* discarding the field if cannot be set accessible */
            if (!f.trySetAccessible())
                continue;
            
            /**
             * we save as fieldName the `name` property of the XMLField
             *  if any, otherwise the canonical field name
             */
            String fieldName = (fieldAnnotation.name().isEmpty()) ? f.getName() : fieldAnnotation.name();
            validFields.add(new Wrapper(f, fieldAnnotation, fieldName));
        }

        return validFields;
    }

    /**
     * 
     * In order to be deserializable, a class must satisfy the following 
     *  conditions:
     * 1. it has to be annotated with @XMLable
     * 2. it has a constructor with no arguments
     * 3. all its fields are non-static and of primitive type or String
     * 4. all its fields are annotated with @XMLField
     * 
     * @param filename
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Object[] deserialize(String filename) throws ClassNotFoundException, IOException {
        /* checks over the filename provided */
        if (filename == null) throw new IllegalArgumentException("filename is null");

        File f = new File(filename);
        if((!f.isFile()) || (!filename.endsWith(".xml")))
            throw new FileNotFoundException("[!!] " + filename + " is not an xml file.");
        
        /* reading the file using FileChannel and ByteBuffer in a very fast way */
        String parsedFile = "";
        try (FileChannel inChannel = new RandomAccessFile(f, "r").getChannel()) {
            int buffer_size = (inChannel.size() < 1024) ? (int) inChannel.size() : 1024;
            ByteBuffer buffer = ByteBuffer.allocate(buffer_size);

            while (inChannel.read(buffer) != -1)
            {
                buffer.flip();
                parsedFile += DEFAULT_CHARSET.decode(buffer);
                buffer.clear();
            }
        } catch (FileNotFoundException e) {} // already managed
        
        /* removal of newline and tabulation characters */
        parsedFile = parsedFile.replaceAll("\\n|\\t", "");
        /* splitting the single String by '<' and '>' char */
        String[] result = Arrays.stream(parsedFile.split("<|>")).filter(str -> !str.isEmpty()).toArray(String[]::new);
        
        /* looking in the system if the class described is present */
        String className = result[0];
        Class<?> pClass = null;
        Package[] packages = Package.getPackages();
        for (Package p : packages)
            try {
                pClass = Class.forName(p.getName() + "." + className);
                // after this point the class is found, we can exit the loop
                System.out.println("Class found in: " + pClass.getName());
                if (pClass.isAnnotationPresent(XMLable.class))
                    break;
            } catch (ClassNotFoundException ex) {} // nothing to do here
        /* end for */
        if (pClass == null) /* no class found */
            throw new ClassNotFoundException("I can't find the class on the system");

        /* Looking for the constructor of the class without arguments */
        Constructor<?> constructor = null;
        try {
            constructor = Arrays.stream(pClass.getDeclaredConstructors())
                    .filter(c -> c.getParameterCount() == 0).findFirst().get();
        } catch (NullPointerException | NoSuchElementException ex) {
            throw new IllegalArgumentException("No suitable constructor found for the object");
        }
        
        /* checking if all the fields are correctly annotated */
        Field[] declaredFields;
        try {
            declaredFields = pClass.getDeclaredFields();
            if(!Arrays.stream(declaredFields).allMatch(x -> x.isAnnotationPresent(XMLfield.class)))
                throw new InvalidPropertiesFormatException("Not all fields are annotated");
        } catch (SecurityException e) {
            throw new SecurityException("The Security Manager does not allow the field reflection on the object");
        }

        
        ArrayList<Wrapper> validFields = getValidFields(declaredFields);
        /**
         * using an HashMap in order to speedup the lookup for a Field
         * object by its fieldName
         */
        HashMap<String, Wrapper> validFieldsMap = new HashMap<>();
        for (Wrapper w : validFields)
            validFieldsMap.put(w.fieldName, w);

        ArrayList<Object> instantiatedObjects = new ArrayList<>();

        int i = 0;
        Object currentObject = null;
        while(i < result.length) /* every 3 position a new xml tag declaration */
        {
            /**
             * On the ``result`` String array we should have the xml parsed as follows:
             * [pos] [data]
             * [0] [ClassName]
             * [1] [Field1]
             * [2] [Data of Field1]
             * [3] [/Field1]
             * [..] Three blocks as above for every field
             * [x] [/ClassName].
             *
             * So the loop should work as follows: take result[i] for every block i from i = 0
             *  if result[i] is equal to "ClassName" this is a new object, declare it and do i++
             *  else if result[i] isn't equal to "/ClassName" this is a new field, check if it exists in the class
             *     and does not start with '/'                if exists then
             *                                                1. parse result[i+1] and do all the type checks
             *                                                2. check if result[i+2] contains "/Field1" (XML validation)
             *                                                if 1 && 2, congrats you have a new field for the object
             *                                                now we have to increase i += 3, in order to reach the
             *                                                next field or the end of the object
             *  else if result[i] is equal to "/ClassName" the object parsing is over, we increase i += 1 in order
             *                                             to check if either a new object is in the xml or the xml is over
             *  else: the xml is not valid
             */
            String currentTag = result[i];
            if (currentTag.equals(className))
            {
                try {
                    currentObject = constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new InstantiationError("Failed to construct the object");
                }

                i += 1;
            }
            else if (!currentTag.equals(className) && (currentTag.charAt(0) != '/'))
            {
                // we can assume the xml <ClassName> is well written thanks to the next if branch
                String currentTagName = currentTag.split(" ")[0];
                if (!currentTag.contains("type")) {
                    throw new IllegalArgumentException("The XML file is not well formed");
                } else if (!result[i + 2].equals("/" + currentTagName)) {
                    throw new IllegalArgumentException("The XML file is not well formed - " + result[i + 2] + " - " + currentTag);
                }

                String typeValue = currentTag.substring(currentTag.indexOf('"')).replace("\"", "");
                Wrapper currentField = validFieldsMap.get(currentTagName);
                if (currentField == null) throw new IllegalArgumentException("The field " + currentTagName + " does not exists");

                try {
                    if (typeValue.equals("int")) currentField.field.setInt(currentObject, Integer.valueOf(result[i + 1]));
                    else if (typeValue.equals("float")) currentField.field.setFloat(currentObject, Float.valueOf(result[i + 1]));
                    else if (typeValue.equals("boolean")) {
                        Boolean b = Boolean.valueOf(result[i + 1]); // does not automatically throw NumberFormatException
                        if (b == null) throw new NumberFormatException();
                        currentField.field.setBoolean(currentObject, b);
                    } else if (typeValue.equals("String")) currentField.field.set(currentObject, result[i + 1]);
                    else throw new IllegalArgumentException("...");
                } catch (NumberFormatException ex) { throw new IllegalArgumentException("Bad validation"); }
                  catch (IllegalAccessException ex) {

                }

                i += 3;
            }
            else if (currentTag.equals("/" + className) && (currentObject != null))
            {
                instantiatedObjects.add(currentObject);
                currentObject = null;
                i += 1;
            }
            else throw new IllegalArgumentException("XML is not well formed");
        }

        return instantiatedObjects.toArray();
    }
    
    /**
     * Serialization of objects array of a same Class, annotated using valid
     *  XMLField and XMLable annotations. The result is outputted into a
     *  .xml file with name 'filename'.
     *  
     * @param arr the array of objects
     * @param filename the output filename
     */
    public static void serialize(Object[] arr, String filename) {
        /* Multiple checks for validating the array and the filename */
        if (arr == null) throw new IllegalArgumentException("The array is null");
        if (filename == null) throw new IllegalArgumentException("filename is null");
        
        Class<? extends Object> classProvided = arr[0].getClass();
        for (int i = 1; i < arr.length; i++)
            if (arr[i].getClass() != classProvided)
                throw new IllegalArgumentException("Objects are not of the same class");
        
        XMLable annotation = classProvided.getAnnotation(XMLable.class);
        if (annotation == null)
            throw new IllegalArgumentException("The class does not have the @XMLable annotation");
        
        /** 
         * Gathering all the public, protected and private fields
         *  (excluding the inherithed ones) 
         */
        Field[] declaredFields;
        try {
            declaredFields = classProvided.getDeclaredFields();
        } catch (SecurityException e) {
            throw new SecurityException("The Security Manager does not allow the field reflection over the object");
        }
        
        /* returning the valid fields for the class */
        ArrayList<Wrapper> validFields = getValidFields(declaredFields);
        
        String className = classProvided.getSimpleName();
        String finalString = "";
        
        /**
         * Writing all the `validFields` values for each object in `arr` into an
         *  intermediate String in an XML format
         */
        for (Object obj : arr) 
        {
            String upper = "<" + className + ">\n";
            String lower = "</" + className + ">\n";

            for (Wrapper w : validFields)
            {
                String typeValue = w.xmlField.type();
                String fieldValue;
                
                /* switching over the supported types */
                try {
                    if (typeValue.equals("int")) fieldValue = String.valueOf(w.field.getInt(obj));
                    else if (typeValue.equals("float")) fieldValue = String.valueOf(w.field.getFloat(obj));
                    else if (typeValue.equals("boolean")) fieldValue = String.valueOf(w.field.getBoolean(obj));
                    else if (typeValue.equals("String")) fieldValue = (String) w.field.get(obj);
                    else throw new IllegalArgumentException("...");
                } catch (IllegalAccessException e) {
                    // it should propagate an IllegalAccessException but I don't want to change the method signature
                    throw new IllegalArgumentException("Can't access the field " + w.field.getName());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("" + e.getMessage());
                }

                upper += "\t<" + w.fieldName + " type=\"" + typeValue + "\">" + fieldValue +
                        "</" + w.fieldName + ">\n";
            }

            finalString += upper + lower;
        }
        
        /**
         * Printing `finalString` into the .xml output file in a very fast way
         *  exploiting FileChannel and ByteBuffer
         */
        File f = new File(filename);
        if(f.exists())
            f.delete();
        
        try(FileChannel fileChannel = new RandomAccessFile(f, "rw").getChannel();)
        {    
            ByteBuffer fileBuffer = ByteBuffer.wrap(finalString.getBytes(DEFAULT_CHARSET));
            while(fileBuffer.hasRemaining())
                fileChannel.write(fileBuffer);

            System.out.println("Results correctly written on " + f);
        } catch (IOException ex) {

        }
    }
    
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        String filename = "output.xml";
        Student[] arr = new Student[10];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new Student("Mario" + i, "Rossi" + i, i);
        }
        XMLSerializer.serialize(arr, filename);
        /* serialization over, now we deserialize and check the equality between
           the serialization and deserialization */
                Object[] result = XMLSerializer.deserialize(filename);
        for (int i = 0; i < result.length; i++) {
            Object r = result[i];
            assert r.equals(arr[i]) && r != arr[i];
            System.out.println(r);
        }
    }
}

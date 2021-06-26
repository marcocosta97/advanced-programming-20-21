/*
 *
 */
package it.mcosta.xmlserialization;

import it.mcosta.xmlserialization.XMLSerializer.*;
import java.util.Objects;

/**
 *
 * @author Marco Costa
 */
@XMLable
public class Student {
    @XMLfield(type = "String")
    public String firstName;
    @XMLfield(type = "String", name="surname")
    public String lastName;
    @XMLfield(type = "int", name="howOld")
    private int age;

    public Student(){}
    public Student(String fn, String ln, int age) {
        this.firstName = fn;
        this.lastName = ln;
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return age == student.age &&
                Objects.equals(firstName, student.firstName) &&
                Objects.equals(lastName, student.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this);
    }

    @Override
    public String toString() {
        return "Student{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", age=" + age +
                '}';
    }
}

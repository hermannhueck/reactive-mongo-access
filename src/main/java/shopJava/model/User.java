package shopJava.model;

import org.bson.Document;

public class User {

    private static final String ID = "_id";
    private static final String NAME = "name";
    private static final String PASSWORD = "password";

    public final String name;
    public final String password;

    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public User(Document doc) {
        this(doc.getString(ID), doc.getString(PASSWORD));
    }

    @Override
    public String toString() {
        return "User{" +
                NAME + "='" + name + '\'' +
                ", " + PASSWORD + "='" + password + '\'' +
                '}';
    }

    public void print() {
        System.out.print(this);
    }

    public void println() {
        System.out.println(this);
    }

    public Document toDocument() {
        return new Document(ID, name).append(PASSWORD, password);
    }
}

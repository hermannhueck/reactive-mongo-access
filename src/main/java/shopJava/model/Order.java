package shopJava.model;

import org.bson.Document;

public class Order {

    private static final String ID = "_id";
    private static final String USERNAME = "first";
    private static final String AMOUNT = "amount";

    public final int id;
    public final String username;
    public final int amount;

    public Order(final int id, final String username, final int amount) {
        this.id = id;
        this.username = username;
        this.amount = amount;
    }

    public Order(final Document doc) {
        this(doc.getInteger(ID), doc.getString(USERNAME), doc.getInteger(AMOUNT));
    }

    @Override
    public String toString() {
        return "Order{" +
                ID + "=" + id +
                ", " + USERNAME + "='" + username + '\'' +
                ", " + AMOUNT + "=" + (amount/100.0) +
                '}';
    }

    public void print() {
        System.out.print(this);
    }

    public void println() {
        System.out.println(this);
    }

    public Document toDocument() {
        return new Document(ID, id).append(USERNAME, username).append(AMOUNT, amount);
    }
}

package shopJava.create;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.Order;
import shopJava.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mongodb.client.model.Filters.eq;
import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.*;

class UserDAO {

    private final MongoCollection<Document> collection;

    UserDAO(final MongoCollection<Document> collection) {
        this.collection = collection;
    }

    void insertUser(final User user) {
        collection.insertOne(user.toDocument());
    }

    long countUsers() {
        return collection.count();
    }

    List<User> findAllUsers() {
        final List<Document> docs = collection.find().into(new ArrayList<>());
        return docs.stream().map(doc -> new User(doc)).collect(toList());
    }

    User findUserByName(final String name) {
        final Document doc = collection.find(eq("_id", name)).first();
        return doc == null ? null : new User(doc);
    }
}

class OrderDAO {

    private final MongoCollection<Document> collection;

    OrderDAO(final MongoCollection<Document> collection) {
        this.collection = collection;
    }

    void insertOrder(Order order) {
        collection.insertOne(order.toDocument());
    }

    long countOrders() {
        return collection.count();
    }

    List<Order> findAllOrders() {
        final List<Document> docs = collection.find().into(new ArrayList<>());
        return docs.stream().map(doc -> new Order(doc)).collect(toList());
    }

    Order findOrderById(final int orderId) {
        final Document doc = collection.find(eq("_id", orderId)).first();
        return doc == null ? null : new Order(doc);
    }

    List<Order> findOrdersByUsername(final String username) {
        final List<Document> docs = collection.find(eq("username", username)).into(new ArrayList<>());
        return docs.stream().map(doc -> new Order(doc)).collect(toList());
    }
}

public class InsertUsersOrders {

    public static void main(String[] args) {
        new InsertUsersOrders();
    }

    private final Random random = new Random();

    private final UserDAO userDAO;
    private final OrderDAO orderDAO;

    public InsertUsersOrders() {

        final MongoClient client = new MongoClient(new MongoClientURI(MONGODB_URI));
        final  MongoDatabase db = client.getDatabase(SHOP_DB_NAME);

        final MongoCollection<Document> usersCollection = db.getCollection(USERS_COLLECTION_NAME);
        usersCollection.drop();
        userDAO = new UserDAO(usersCollection);

        final MongoCollection<Document> ordersCollection = db.getCollection(ORDERS_COLLECTION_NAME);
        ordersCollection.drop();
        orderDAO = new OrderDAO(ordersCollection);

        insertUsers();

        findAndPrintUsers();
        System.out.println();

        insertOrders();

        findAndPrintOrders();
    }

    private void insertUsers() {
        for (String username: SIMPSONS) {
            userDAO.insertUser(new User(username, "password"));
        }
    }

    private void findAndPrintUsers() {

        final long userCount = userDAO.countUsers();
        System.out.println("Users count: " + userCount);

        final String username = HOMER;
        final User user = userDAO.findUserByName(username);
        System.out.println("User named " + username + ":\n" + user == null ? "NOT FOUND" : user);

        System.out.println("All Users:");
        final List<User> users = userDAO.findAllUsers();
        users.forEach(u -> u.println());
    }

    private void insertOrders() {

        int orderId = 1;
        final List<User> users = userDAO.findAllUsers();

        for (User user: users) {
            final int maxUserOrders = random.nextInt(MAX_ORDERS_PER_USER);
            insertOrdersFor(user.name, orderId, maxUserOrders);
            orderId += maxUserOrders;
        }
    }

    private void insertOrdersFor(final String username, final int firstOrderId, final int maxOrders) {
        for (int orderId = firstOrderId; orderId < firstOrderId + maxOrders; orderId++) {
            int amount = random.nextInt(100000);
            orderDAO.insertOrder(new Order(orderId, username, amount));
        }
    }

    private void findAndPrintOrders() {

        final long orderCount = orderDAO.countOrders();
        System.out.println("Orders count: " + orderCount);

        final int orderId = 5;
        final Order order = orderDAO.findOrderById(orderId);
        System.out.println("Order with orderId " + orderId + ":\n" + (order == null ? "NOT FOUND" : order));

        System.out.println("All Orders:");
        final List<Order> orders = orderDAO.findAllOrders();
        orders.forEach(o -> o.println());

        final String username = MAGGIE;
        System.out.println("All Orders of user " + username + ":");
        final List<Order> orders2 = orderDAO.findOrdersByUsername(username);
        orders2.forEach(o -> o.println());
    }
}

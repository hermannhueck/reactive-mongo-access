package shopJava.queries;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.Order;
import shopJava.model.User;
import shopJava.model.Credentials;
import shopJava.model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ01Blocking {

    public static void main(String[] args) throws Exception {
        new QueryJ01Blocking();
    }

    private final DAO dao = new DAO();

    private class DAO {

        private final MongoCollection<Document> usersCollection;
        private final MongoCollection<Document> ordersCollection;

        DAO() {
            final MongoClient client = new MongoClient();
            final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
            this.usersCollection = db.getCollection(USERS_COLLECTION_NAME);
            this.ordersCollection = db.getCollection(ORDERS_COLLECTION_NAME);
        }

        Optional<User> findUserByName(final String name) {
            final Document doc = usersCollection.find(eq("_id", name)).first();
            return doc == null ? Optional.empty() : Optional.of(new User(doc));
        }

        List<Order> findOrdersByUsername(final String username) {
            final List<Document> docs = ordersCollection.find(eq("username", username)).into(new ArrayList<>());
            return docs.stream().map(doc -> new Order(doc)).collect(toList());
        }
    }   // end DAO


    private String logIn(final Credentials credentials) {
        final Optional<User> optUser = dao.findUserByName(credentials.username);
        final User user = checkUserLoggedIn(optUser, credentials);
        return user.name;
    }

    private Result processOrdersOf(final String username) {
        final List<Order> orders = dao.findOrdersByUsername(username);
        return new Result(username, orders);
    }

    private void eCommercStatistics(final Credentials credentials) {

        System.out.println("--- Calculating eCommerce statistings of user \"" + credentials.username + "\" ...");

        try {
            final String username = logIn(credentials);
            final Result result = processOrdersOf(username);
            result.display();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    private QueryJ01Blocking() throws Exception {

        eCommercStatistics(new Credentials(LISA, "password"));
        sleep(2000L);
        eCommercStatistics(new Credentials(LISA, "bad_password"));
        sleep(2000L);
        eCommercStatistics(new Credentials(LISA.toUpperCase(), "password"));
    }
}

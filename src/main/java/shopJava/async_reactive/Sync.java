package shopJava.async_reactive;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.User;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.MONGODB_URI;
import static shopJava.util.Constants.SHOP_DB_NAME;
import static shopJava.util.Constants.USERS_COLLECTION_NAME;

@SuppressWarnings("Convert2MethodRef")
public class Sync {

    public static void main(String[] args) {
        new Sync();
    }

    private final MongoClient client = new MongoClient(new MongoClientURI(MONGODB_URI));
    private final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
    private final MongoCollection<Document> usersCollection = db.getCollection(USERS_COLLECTION_NAME);

    private Sync() {

        List<String> simpsons = blockingIO_GetDataFromDB();
        simpsons.forEach(simpson -> System.out.println(simpson));
    }

    private List<String> blockingIO_GetDataFromDB() {

        final List<Document> docs = usersCollection.find().into(new ArrayList<>());

        return docs
                .stream()
                .map(doc -> new User(doc))
                .map(user -> user.name)
                .collect(toList());
    }
}

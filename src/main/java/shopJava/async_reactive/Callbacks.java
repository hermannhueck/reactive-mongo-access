package shopJava.async_reactive;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.User;

import java.util.ArrayList;
import java.util.List;

import static shopJava.util.Constants.SHOP_DB_NAME;
import static shopJava.util.Constants.USERS_COLLECTION_NAME;
import static shopJava.util.Util.sleep;

@SuppressWarnings("Convert2MethodRef")
public class Callbacks {

    public static void main(String[] args) {
        new Callbacks();
    }

    private final MongoClient client = MongoClients.create();
    private final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
    private final MongoCollection<Document> usersCollection = db.getCollection(USERS_COLLECTION_NAME);

    private Callbacks() {

        SingleResultCallback<List<String>> callback = new SingleResultCallback<List<String>>() {
            @Override
            public void onResult(List<String> simpsons, Throwable t) {
                if (t != null) {
                    t.printStackTrace();
                } else {
                    simpsons.forEach(simpson -> System.out.println(simpson));
                }
            }
        };

        nonblockingIOWithCallbacks_GetDataFromDB(callback);

        sleep(2);
    }

    private void nonblockingIOWithCallbacks_GetDataFromDB(SingleResultCallback<List<String>> callback) {

        usersCollection
                .find()
                .map(doc -> new User(doc))
                .map(user -> user.name)
                .into(new ArrayList<>(), callback);
    }
}

package shopScala.queries

import com.mongodb.{MongoClient, MongoClientURI}
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._


object QueryS01JDriverSync extends App {

  type JList[T] = java.util.List[T]
  type JArrayList[T] = java.util.ArrayList[T]

  object dao {

    val client: MongoClient = new MongoClient(new MongoClientURI(MONGODB_URI))
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    private def _findUserByName(name: String): Option[User] = {
      val doc: Document = usersCollection
        .find(Filters.eq("_id", name))
        .first
      Option(doc).map(User(_))
    }

    private def _findOrdersByUsername(username: String): Seq[Order] = {
      val jDocs: JList[Document] = ordersCollection
        .find(Filters.eq("username", username))
        .into(new JArrayList[Document])
      jListToSeq(jDocs).map(Order(_))
    }

    def findUserByName(name: String): Option[User] = {
      _findUserByName(name)
    }

    def findOrdersByUsername(username: String): Seq[Order] = {
      _findOrdersByUsername(username)
    }
  }   // end dao


  def logIn(credentials: Credentials): String = {
    val optUser: Option[User] = dao.findUserByName(credentials.username)
    val user: User = checkUserLoggedIn(optUser, credentials)
    user.name
  }

  private def processOrdersOf(username: String): Result = {
    val (totalAmount, orderCount) = dao.findOrdersByUsername(username)
      .map(order => (order.amount, 1))
      .fold(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
    Result(username, orderCount, totalAmount)
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    try {
      val username: String = logIn(credentials)
      val result: Result = processOrdersOf(username)
      result.display()
    }
    catch {
      case t: Throwable =>
        Console.err.println(t.toString)
    }
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}

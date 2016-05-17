package shopScala.queries

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.collection.JavaConversions

object QueryS01JavaDriverBlocking extends App {

  object dao {

    val client: MongoClient = new MongoClient
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    def findUserByName(name: String): Option[User] = {
      val doc: Document = usersCollection
        .find(Filters.eq("_id", name))
        .first
      if (doc == null) None else Some(User(doc))
    }

    def findOrdersByUsername(username: String): Seq[Order] = {
      val docs: java.util.List[Document] = ordersCollection
        .find(Filters.eq("username", username))
        .into(new java.util.ArrayList[Document])
      val seq: Seq[Document] = Seq.empty ++ JavaConversions.asScalaBuffer(docs)
      seq.map(doc => Order(doc))
    }
  }   // end dao


  def logIn(credentials: Credentials): String = {
    val optUser: Option[User] = dao.findUserByName(credentials.username)
    val user: User = checkUserLoggedIn(optUser, credentials)
    user.name
  }

  private def processOrdersOf(username: String): Result = {
    Result(username, dao.findOrdersByUsername(username))
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...")

    try {
      val username: String = logIn(credentials)
      val result: Result = processOrdersOf(username)
      result.display()
    }
    catch {
      case e: Exception => {
        System.err.println(e.toString)
      }
    }
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}

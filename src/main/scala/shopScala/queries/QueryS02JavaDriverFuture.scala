package shopScala.queries

import java.util.concurrent.CountDownLatch

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.collection.JavaConversions
import scala.concurrent.Future
import scala.util.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global

object QueryS02JavaDriverFuture extends App {

  object dao {

    val client: MongoClient = new MongoClient
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    def findUserByName(name: String): Future[Option[User]] = {
      Future {
        val doc: Document = usersCollection
          .find(Filters.eq("_id", name))
          .first
        if (doc == null) None else Some(User(doc))
      }
    }

    def findOrdersByUsername(username: String): Future[Seq[Order]] = {
      Future {
        val docs: java.util.List[Document] = ordersCollection
          .find(Filters.eq("username", username))
          .into(new java.util.ArrayList[Document])
        val seq: Seq[Document] = Seq.empty ++ JavaConversions.asScalaBuffer(docs)
        seq.map(doc => Order(doc))
      }
    }
  }   // end dao


  def logIn(credentials: Credentials): Future[String] = {
    dao.findUserByName(credentials.username)
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  private def processOrdersOf(username: String): Future[Result] = {
    dao.findOrdersByUsername(username)
      .map(orders => Result(username, orders))
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials)
      .flatMap(username => processOrdersOf(username))
      .onComplete {
        case Success(result) =>
          result.display()
          latch.countDown()
        case Failure(t) =>
          Console.err.println(t.toString)
          latch.countDown()
      }

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}

package shopScala.queries

import java.util.concurrent.CountDownLatch

import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}


object QueryS03JDriverSyncWithFutureAndPromise extends App {

  type JList[T] = java.util.List[T]
  type JArrayList[T] = java.util.ArrayList[T]

  object dao {

    val client: MongoClient = new MongoClient
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

    def findUserByName(name: String): Future[Option[User]] = {
      val p = Promise[Option[User]]
      p.complete(Try {
        _findUserByName(name)
      })
      p.future
    }

    def findOrdersByUsername(username: String): Future[Seq[Order]] = {
      val p = Promise[Seq[Order]]
      p.complete(Try {
        _findOrdersByUsername(username)
      })
      p.future
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
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}

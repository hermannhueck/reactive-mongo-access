package shopScala.queries

import java.util.concurrent.CountDownLatch

import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import com.mongodb.client.model.Filters
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}


object QueryS05aJDriverAyncCallbackWithFutureAndPromise extends App {

  type JList[T] = java.util.List[T]
  type JArrayList[T] = java.util.ArrayList[T]

  object dao {

    val client: MongoClient = MongoClients.create
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    private def _findUserByName(name: String, callback: SingleResultCallback[Option[User]]): Unit = {
      usersCollection
        .find(Filters.eq("_id", name))
        .map(scalaMapper2MongoMapper { doc => Option(doc).map(User(_)) })
        .first(callback)
    }

    private def _findOrdersByUsername(username: String, callback: SingleResultCallback[JList[Order]]): Unit = {
      ordersCollection
        .find(Filters.eq("username", username))
        .map(scalaMapper2MongoMapper { doc => Order(doc) })
        .into(new JArrayList[Order], callback)
    }

    def findUserByName(name: String): Future[Option[User]] = {

      val promise = Promise[Option[User]]

      _findUserByName(name, new SingleResultCallback[Option[User]]() {

        override def onResult(optUser: Option[User], t: Throwable): Unit = {
          if (t == null) {
            promise.success(optUser)
          } else {
            promise.failure(t)
          }
        }
      })

      promise.future
    }

    def findOrdersByUsername(username: String): Future[Seq[Order]] = {

      val promise = Promise[Seq[Order]]

      _findOrdersByUsername(username, new SingleResultCallback[JList[Order]]() {

        override def onResult(orders: JList[Order], t: Throwable): Unit = {
          if (t == null) {
            promise.success(jListToSeq(orders))
          } else {
            promise.failure(t)
          }
        }
      })

      promise.future
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

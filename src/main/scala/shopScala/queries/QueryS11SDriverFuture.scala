package shopScala.queries

import java.util.concurrent.CountDownLatch

import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


object QueryS11SDriverFuture extends App {

  type MongoObservable[T] = org.mongodb.scala.Observable[T]

  object dao {

    val client: MongoClient = MongoClient()
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    private def _findUserByName(name: String): MongoObservable[User] = {
      usersCollection
        .find(Filters.eq("_id", name))
        .first()
        .map(doc => User(doc))
    }

    private def _findOrdersByUsername(username: String): MongoObservable[Order] = {
      ordersCollection
        .find(Filters.eq("username", username))
        .map(doc => Order(doc))
    }

    def findUserByName(name: String): Future[Option[User]] = {
      _findUserByName(name)
        .toFuture
        .map(seq => seq.headOption)
    }

    def findOrdersByUsername(username: String): Future[Seq[Order]] = {
      _findOrdersByUsername(username)
        .toFuture
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

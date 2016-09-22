package shopScala.queries

import java.util.concurrent.CountDownLatch

import play.api.libs.iteratee.{Enumerator, Iteratee}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.BSONDocument
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/*
    For ReactiveMongo see:
      http://reactivemongo.org/
      http://reactivemongo.org/releases/0.11/documentation/index.html
      https://github.com/ReactiveMongo/ReactiveMongo
      https://github.com/sgodbillon/reactivemongo-demo-app
 */

object QueryS13ReactiveMongoWithEnumeratorAndFuture extends App {

  object dao {

    val driver: MongoDriver = new MongoDriver
    val connection: MongoConnection = driver.connection(List(MONGODB_HOST_PORT))
    val fDatabase: Future[DefaultDB] = connection.database(SHOP_DB_NAME)
    val database = Await.result(fDatabase, Duration.Inf)
    val usersCollection = database.collection[BSONCollection](USERS_COLLECTION_NAME)
    val ordersCollection = database.collection[BSONCollection](ORDERS_COLLECTION_NAME)

    def close(): Unit = {
      connection.close()
      connection.actorSystem.terminate()
    }

    private def _findUserByName(name: String): Enumerator[User] = {
      usersCollection
        .find(BSONDocument("_id" -> name))
        .cursor[BSONDocument]()
        .enumerate()
        .map(User(_))
    }

    private def _findOrdersByUsername(username: String): Enumerator[Order] = {
      ordersCollection
        .find(BSONDocument("username" -> username))
        .cursor[BSONDocument]()
        .enumerate()
        .map(Order(_))
    }

    private def enumeratorToFutureSeq[T](enumerator: Enumerator[T]): Future[Seq[T]] =
      enumerator.run {
        Iteratee.fold(List.empty[T])((list: List[T], element: T) => element::list)
      }

    def findUserByName(name: String): Future[Option[User]] = {
      enumeratorToFutureSeq(_findUserByName(name))
        .map(seq => seq.headOption)
    }

    def findOrdersByUsername(username: String): Future[Seq[Order]] = {
      enumeratorToFutureSeq(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): Future[String] = {
    dao.findUserByName(credentials.username)
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): Future[Result] = {
    dao.findOrdersByUsername(username)
      .map(seq => new Result(username, seq))
  }

  def eCommerceStatistics(credentials: Credentials, isLastInvocation: Boolean = false): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials)
      .flatMap(username => processOrdersOf(username))
      .onComplete {
        case Success(result) =>
          result.display()
          latch.countDown()
          if (isLastInvocation)
            dao.close()
        case Failure(t) =>
          Console.err.println(t.toString)
          latch.countDown()
          if (isLastInvocation)
            dao.close()
      }

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"), isLastInvocation = true)
}

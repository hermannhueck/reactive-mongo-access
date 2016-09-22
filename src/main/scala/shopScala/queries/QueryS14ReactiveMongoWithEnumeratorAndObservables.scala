package shopScala.queries

import java.util.concurrent.CountDownLatch

import org.reactivestreams.Publisher
import play.api.libs.iteratee.Enumerator
import play.api.libs.streams.Streams
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.BSONDocument
import rx.lang.scala.{JavaConversions, Observable}
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/*
    For ReactiveMongo see:
      http://reactivemongo.org/
      http://reactivemongo.org/releases/0.11/documentation/index.html
      https://github.com/ReactiveMongo/ReactiveMongo
      https://github.com/sgodbillon/reactivemongo-demo-app
 */

object QueryS14ReactiveMongoWithEnumeratorAndObservables extends App {

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

    private def enumeratorToObservable[T](enumerator: Enumerator[T]): Observable[T] = {

      def toPublisher[T](enumerator: Enumerator[T]): Publisher[T] =
        Streams.enumeratorToPublisher(enumerator)

      def toJavaObservable[T](publisher: Publisher[T]): rx.Observable[T] =
        rx.RxReactiveStreams.toObservable(publisher)

      def toObservable[T](publisher: Publisher[T]): Observable[T] =
        JavaConversions.toScalaObservable(toJavaObservable(publisher))

      toObservable(toPublisher(enumerator))
    }

    def findUserByName(name: String): Observable[User] = {
      enumeratorToObservable(_findUserByName(name))
        .single
    }

    def findOrdersByUsername(username: String): Observable[Order] = {
      enumeratorToObservable(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): Observable[String] = {
    dao.findUserByName(credentials.username)
      .map(user => checkCredentials(user, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): Observable[Result] = {
    dao.findOrdersByUsername(username)
      .map(order => (order.amount, 1))
      .foldLeft(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
      .map(pair => Result(username, pair._2, pair._1))
  }

  def eCommerceStatistics(credentials: Credentials, isLastInvocation: Boolean = false): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials)
      .flatMap(username => processOrdersOf(username))
      .subscribe(
        result => {
          result.display()
        },
        t => {
          Console.err.println(t.toString)
          latch.countDown()
          if (isLastInvocation)
            dao.close()
        },
        () => {
          latch.countDown()
          if (isLastInvocation)
            dao.close()
        }
      )

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"), isLastInvocation = true)
}

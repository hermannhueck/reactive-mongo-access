package shopScala.queries.old

import java.util.concurrent.CountDownLatch

import org.reactivestreams.Publisher
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.BSONDocument
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._
import shopScala.util.conversion.FutureToRxStreamsConversion

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

object QueryS14ReactiveMongoWithPublisherAndObservables extends App {

  object dao {

    val driver: MongoDriver = new MongoDriver
    val connection: MongoConnection = driver.connection(List("localhost"))
    val fDatabase: Future[DefaultDB] = connection.database(SHOP_DB_NAME)
    val database = Await.result(fDatabase, Duration.Inf)
    val usersCollection = database.collection[BSONCollection](USERS_COLLECTION_NAME)
    val ordersCollection = database.collection[BSONCollection](ORDERS_COLLECTION_NAME)

    def close(): Unit = {
      connection.close()
      connection.actorSystem.terminate()
    }

    private def _findUserByName(name: String): Future[Option[User]] = {
      usersCollection
        .find(BSONDocument("_id" -> name))
        .one[BSONDocument]
        .map { optDoc =>
          optDoc map { doc =>
            User(doc)
          }
        }
    }

    private def _findOrdersByUsername(username: String): Future[Seq[Order]] = {
      ordersCollection
        .find(BSONDocument("username" -> username))
        .cursor[BSONDocument]()
        .collect[Seq]()
        .map { seqOptDoc =>
          seqOptDoc map { doc =>
            Order(doc)
          }
        }
    }

    def findUserByName(name: String): Publisher[_ <: Option[User]] = {
      FutureToRxStreamsConversion.futureToPublisher(_findUserByName(name))
    }

    def findOrdersByUsername(username: String): Publisher[_ <: Seq[Order]] = {
      FutureToRxStreamsConversion.futureToPublisher(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): rx.lang.scala.Observable[String] = {
    publisherToObservable(dao.findUserByName(credentials.username))
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): rx.lang.scala.Observable[Result] = {
    publisherToObservable(dao.findOrdersByUsername(username))
      .map(orders => new Result(username, orders))
  }

  def publisherToObservable[T](pub: Publisher[T]): rx.lang.scala.Observable[T] = {
    val javaObs: rx.Observable[T] = rx.RxReactiveStreams.toObservable(pub)
    rx.lang.scala.JavaConversions.toScalaObservable(javaObs)
  }

  def eCommerceStatistics(credentials: Credentials, isLastInvocation: Boolean = false): Unit = {

    println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...")

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

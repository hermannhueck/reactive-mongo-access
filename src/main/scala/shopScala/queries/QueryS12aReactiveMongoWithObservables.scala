package shopScala.queries

import java.util.concurrent.CountDownLatch

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.BSONDocument
import rx.lang.{scala => rx}
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

object QueryS12aReactiveMongoWithObservables extends App {

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

    def findUserByName(name: String): rx.Observable[Option[User]] = {
      rx.Observable.from(_findUserByName(name))
    }

    def findOrdersByUsername(username: String): rx.Observable[Seq[Order]] = {
      rx.Observable.from(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): rx.Observable[String] = {
    dao.findUserByName(credentials.username)
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): rx.Observable[Result] = {
    dao.findOrdersByUsername(username)
      .map(orders => new Result(username, orders))
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

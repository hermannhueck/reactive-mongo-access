package shopScala.async_reactive

import java.util.concurrent.CountDownLatch

import org.mongodb.scala._
import shopScala.util.Constants._
import shopScala.util.User

object Streams extends App {

  type MongoObservable[T] = org.mongodb.scala.Observable[T]

  val client: MongoClient = MongoClient()
  val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
  val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)

  def nonblockingIOWithStreams_GetDataFromDB(): MongoObservable[String] = {
    usersCollection
      .find()
      .map(User(_))
      .map(_.name)
  }

  val latch: CountDownLatch = new CountDownLatch(1)

  val obsSimpsons: MongoObservable[String] = nonblockingIOWithStreams_GetDataFromDB()

  obsSimpsons.subscribe(new Observer[String] {

    override def onNext(simpson: String): Unit = println(simpson)

    override def onComplete(): Unit = {
      println("----- DONE -----")
      latch.countDown()
    }

    override def onError(t: Throwable): Unit = {
      t.printStackTrace()
      latch.countDown()
    }
  })

  latch.await()
}

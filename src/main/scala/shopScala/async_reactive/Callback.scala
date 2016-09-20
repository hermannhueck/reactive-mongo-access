package shopScala.async_reactive

import java.util
import java.util.concurrent.CountDownLatch

import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.User
import shopScala.util.Util._

object Callback extends App {

  type JList[T] = util.List[T]
  type JArrayList[T] = util.ArrayList[T]

  val client: MongoClient = MongoClients.create(MONGODB_URI)
  val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
  val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)

  def nonblockingIOWithCallbacks_GetDataFromDB(callback: SingleResultCallback[JList[String]]): Unit = {
    usersCollection
      .find()
      .map(scalaMapper2MongoMapper { doc => User(doc) })
      .map(scalaMapper2MongoMapper { user => user.name })
      .into(new JArrayList[String], callback)
  }

  val latch: CountDownLatch = new CountDownLatch(1)

  val callback = new SingleResultCallback[JList[String]]() {
    def onResult(simpsons: JList[String], t: Throwable): Unit = {
      if (t != null) {
        t.printStackTrace()
      } else {
        jListToSeq(simpsons).foreach(println)
      }
      latch.countDown()
    }
  }

  nonblockingIOWithCallbacks_GetDataFromDB(callback)

  latch.await()
}

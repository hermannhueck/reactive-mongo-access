package shopScala.async_reactive

import java.util

import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util.User

object SyncJDriver extends App {

  type JList[T] = util.List[T]
  type JArrayList[T] = util.ArrayList[T]

  val client: MongoClient = new MongoClient
  val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
  val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)

  def blockingIO_GetDataFromDB: Seq[String] = {
    val jDocs: JList[Document] = usersCollection
      .find()
      .into(new JArrayList[Document])
    jListToSeq(jDocs).map(User(_)).map(_.name)
  }

  val simpsons: Seq[String] = blockingIO_GetDataFromDB
  simpsons.foreach(println)
}

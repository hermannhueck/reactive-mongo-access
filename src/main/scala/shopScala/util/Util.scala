package shopScala.util

import org.bson.Document

import scala.collection.JavaConversions


object Util {

  def checkUserLoggedIn(optUser: Option[User], credentials: Credentials): User = {
    // print("checkUserLoggedIn for user: ")
    optUser match {
      case None =>
        //println("--- NOT FOUND ---")
        throw new IllegalAccessException("User unknown: " + credentials.username)
      case Some(user) =>
        //println(user.toString)
        checkCredentials(user, credentials)
    }
  }

  def checkCredentials(user: User, credentials: Credentials): User = {
    if (user.name != credentials.username)
      throw new IllegalAccessException("Incorrect username: " + credentials.username)
    else if (user.password != credentials.password)
      throw new IllegalAccessException("Bad password supplied for user: " + credentials.username)
    else
      user
  }

  def jListToSeq[T](javaList: java.util.List[T]): Seq[T] = Seq.empty ++ JavaConversions.asScalaBuffer(javaList)

  def scalaMapper2MongoMapper[A, B](scalaMapper: A => B): com.mongodb.Function[A, B] with Object {def apply(doc: A): B} = {
    new com.mongodb.Function[A, B] {
      def apply(a: A): B = scalaMapper(a)
    }
  }

  def doc2OptUser: com.mongodb.Function[Document, Option[User]] with Object {def apply(doc: Document): Option[User]} = {
    scalaMapper2MongoMapper {
      doc => Option(doc).map(User(_))
    }
  }

  def docToOrder: com.mongodb.Function[Document, Order] with Object {def apply(doc: Document): Order} = {
    scalaMapper2MongoMapper {
      doc => Order(doc)
    }
  }
}

package shopScala.promises

import java.lang.Thread.sleep

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object PromiseSync extends App {

  def now() = new java.util.Date()

  def hello = {
    sleep(5000)
    "hello"
  }

  def helloSync: Future[String] = {
    println(now() + ": constructing the future ...")
    val p = Promise[String]
    p.complete(Try {
      hello
    })
    println(now() + ": returning the future ...")
    p.future
  }

  val f: Future[String] = helloSync

  f onComplete {
    case Success(result) => println(now() + ": result = " + result)
    case Failure(e) => println(now() + ": exception = " + e); e.printStackTrace()
  }
}

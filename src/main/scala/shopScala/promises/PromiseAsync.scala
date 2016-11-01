package shopScala.promises

import java.lang.Thread.sleep

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

object PromiseAsync extends App {

  def now() = new java.util.Date()

  def hello = {
    sleep(5000)
    "hello"
  }

  def helloAsync: Future[String] = {
    println(now() + ": constructing the future ...")
    val p = Promise[String]
    Future {
      p.complete(Try {
        hello
      })
    }
    println(now() + ": returning the future ...")
    p.future
  }

  val f: Future[String] = helloAsync

  f onComplete {
    case Success(result) => println(now() + ": result = " + result)
    case Failure(e) =>  println(now() + ": exception = " + e); e.printStackTrace()
  }

  Await.ready(f, Duration.Inf)
}

package shopScala.util.conversion

import scala.concurrent.Future
import org.{reactivestreams => rxStreams}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object FutureToRxStreamsConversion {

  implicit def futureToPublisher[T](future: Future[T]): rxStreams.Publisher[T] = FutureToPublisher(future)

  case class FutureToPublisher[T](future: Future[T]) extends rxStreams.Publisher[T] {

    override def subscribe(subscriber: rxStreams.Subscriber[_ >: T]): Unit = {

      future.onComplete {

        case Failure(t) =>
          subscriber.onError(t)

        case Success(result) =>
          subscriber.onNext(result)
          subscriber.onComplete()
      }
    }
  }
}

package shopScala

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.reactivestreams.Publisher
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.streams.Streams
import rx.lang.scala._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

package object queries {

/*
  type JavaObservable[T] = rx.Observable[T]
  type ScalaObservable[T] = rx.lang.scala.Observable[T]

  implicit def toPublisher[T](enumerator: Enumerator[T]): Publisher[T] =
    Streams.enumeratorToPublisher(enumerator)

  implicit def toObservable[T](publisher: Publisher[T]): ScalaObservable[T] =
    JavaConversions.toScalaObservable(toJavaObservable(publisher))

  private def toJavaObservable[T](publisher: Publisher[T]): rx.Observable[T] =
    rx.RxReactiveStreams.toObservable(publisher)

  implicit def toObservable[T](enumerator: Enumerator[T]): Observable[T] =
    toObservable(toPublisher(enumerator))

  implicit def toSource[T](publisher: Publisher[T]): Source[T, NotUsed] =
    Source.fromPublisher(publisher)

  implicit def toSource[T](enumerator: Enumerator[T]): Source[T, NotUsed] =
    toSource(toPublisher(enumerator))
*/
}

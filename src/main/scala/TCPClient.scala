import scala.concurrent._
import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Tcp, _}
import akka.util._
import java.io.File
import java.nio.file.StandardOpenOption._

import scala.util.{Failure, Success, Try}

object TCPClient extends App {

  val systemClient= ActorSystem("Client")
  val serverAddress = "127.0.0.1"
  val serverPort = 6002

  client(systemClient,serverAddress,serverPort)

  def client(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val inputFile= new File("src/main/resources/medium.csv")

    val testInput= FileIO.fromFile(inputFile)


    /**
      * My RunnableGraph is:
      *  Source     ~>            Flow            ~>      Sink
      * testInput      Tcp().OutgoingConnection()       Sink.ignore
      *
      */
    //val src= SynchronousFileSource.

    val printResults= Flow[ByteString] to Sink.foreach(x => println(x.toString()))
    val result = testInput.via(Tcp().outgoingConnection(address, port)).runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }

    result.onComplete{
      case Success(result) =>
        println(s"Result: " + result.utf8String)
        println("Shutting down client")
        system.terminate()
      case Failure(e) =>
        println(s"Error in client: ${e.getMessage}")
        system.terminate()
    }


    //val sendFileFlow = Flow.fromSinkAndSourceMat(Sink.ignore, FileIO.fromFile(inputFile))(Keep.right)

  }



}
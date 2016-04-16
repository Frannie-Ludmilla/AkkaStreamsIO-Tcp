import scala.concurrent._
import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Tcp, _}
import akka.util._
import java.io.File
import scala.util.{Failure, Success}

object TCPClient extends App {

  val systemClient= ActorSystem("SendingClient")
  val serverAddress = "127.0.0.1"
  val serverPort = 6002

  client(systemClient,serverAddress,serverPort)

  def client(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val inputFile= new File("src/main/resources/medium.csv")
    val fileSize = inputFile.length
    val testInput= FileIO.fromFile(inputFile)

    /**
      * My RunnableGraph is:
      *  Source     ~>            Flow             ~>           Sink
      * testInput      Tcp().OutgoingConnection()         totalTransferredBytes
      *                                               where totalTransferredBytes += sizeof(x)
      *                                               with x as the incoming ByteStreams
      */

    val result = testInput.via(Tcp().outgoingConnection(address, port)).runFold(ByteString.empty) { (acc, in) ⇒ acc ++ in }

    result.onComplete{
      case Success(result) =>
        println(s"Transferred Bytes: ${result.utf8String} - filesize: $fileSize")
        println("Shutting down client")
        system.terminate()
      case Failure(e) =>
        println(s"Error in client: ${e.getMessage}")
        system.terminate()
    }
  }
}
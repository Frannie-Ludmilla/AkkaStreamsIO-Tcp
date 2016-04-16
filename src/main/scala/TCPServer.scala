import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._

import scala.util._
import akka.NotUsed
import java.nio.file.StandardOpenOption._
import java.io.File
import java.nio.file.{Files, Path, Paths}

object TCPServer extends App {

  val systemServer = ActorSystem("ReceivingServer")
  val serverAddress = "127.0.0.1"
  val serverPort = 6002
  server(systemServer, serverAddress, serverPort)

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    /**
      * How to understand Akka Streams with Ascii Art
      *
      * A Tcp Stream could be defined by [IncomingConnection, OutgoingConnection]
      * whose flow must comply to Flow[ByteString, ByteString, NotUsed]
      *
      * +->(inbytes)-->+
      * |              |
      * [TCP]       [HandlerFlow]
      * |              |
      * +<-(outbytes)<-+
      *
      *Now, the Receiver (the server in this case) want to get the ByteString content to:
      * A - send it to file_sink = FileIO.toFile (it is a Sink)
      * B - reply to the Sender with the amount of received bytes:
      *     logic takes the ByteString incoming string, gets its length (Int) and
      *     writes it back as a ByteString.
      * The HandlerFlow is now composed by both modules but only B output will
      * be wrapped inside the OutgoingConnection.
      *
      *
      * +->(inbytes)-->+            +--------->file_sink (A)
      * |              |            |
      * [TCP]          +---->bcast--+--------->logic (B)
      * |                                        |
      * +<-(outbytes)<-+-------------------------+
      *
      * */

    def serverLogic(output:File)
    : Flow[ByteString, ByteString, NotUsed]
      = Flow.fromGraph(GraphDSL.create(){ implicit b =>
      import GraphDSL.Implicits._
      val bcast = b.add(Broadcast[ByteString](2))
      val file_sink= b.add(FileIO.toFile(output, options = Set(CREATE, WRITE)))
      val logic = b.add(Flow[ByteString]
        .map(_.length.toString).map(ByteString(_)))
      bcast.out(0) ~> file_sink
      bcast.out(1) ~> logic
      FlowShape(bcast.in, logic.out)
    })

    def parseAddress(address: String) :String = {
      val index= address.indexOf(":")
      address.substring(1,address.length).replace('.','_').replace(':','@')
      /** NOTE: the address will be /x.x.x.x:y e.g. /127.0.0.1:49755
        * and I want to discard the first char('/') of the address
        */
    }

    def createDir(path: String): String = {
      val resultDir: File = new File(path)
      if (!resultDir.exists()) resultDir.mkdir()
      path
    }

      val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
        /** The flow of each incoming connection will be saved to a file.
          *  Since the IP address is x.x.x.x:y, the filename will be
          *  x_x_x_x@y.fileExtension (csv in my case)
          */
        println(conn.remoteAddress.toString)
        val stringRemoteAddress = conn.remoteAddress.toString
        val directory= createDir("target/results/")

        val outputFile = new File(s"$directory${parseAddress(stringRemoteAddress)}.csv")
        /** NOTE:
          * def handleWith[Mat](handler: Flow[ByteString, ByteString, Mat])(implicit materializer: Materializer): Mat =
          * flow.joinMat(handler)(Keep.right).run()
          **/
        conn handleWith(serverLogic(outputFile))
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        println("Server started, listening on: " + b.localAddress)
      case Failure(e) =>
        println(s"Server could not bind to $address:$port: ${e.getMessage}")
        system.terminate()
    }
  }
}


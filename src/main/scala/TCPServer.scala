import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.util._
import scala.util._
import akka.stream.stage._
import akka.NotUsed
import java.nio.file.StandardOpenOption._
import java.io.File

object TCPServer extends App {

  val systemServer = ActorSystem("Server")
  //implicit val materializer = ActorMaterializer()
  val serverAddress = "127.0.0.1"
  val serverPort = 6002
  server(systemServer, serverAddress, serverPort)

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()


    import java.nio.file.StandardOpenOption._
    //val saveFileFlow = Flow.fromSinkAndSourceMat(FileIO.toFile(output, options = Set(CREATE, WRITE)), Source.repeat(ByteString.empty))(Keep.left)
    //val saveFileFlow = Flow.fromSinkAndSourceMat(FileIO.toFile(output, options=Set(CREATE, WRITE)), Source.empty)(Keep.right)

    //def serverLogic(conn: Tcp.IncomingConnection)
    def serverLogic(output:File)
    : Flow[ByteString, ByteString, NotUsed]
      = Flow.fromGraph(GraphDSL.create(){ implicit b =>
      import GraphDSL.Implicits._
      //val source = b.add(Flow[ByteString])
      val bcast = b.add(Broadcast[ByteString](2))
      //val output = new File(s"res.csv")
      val file_sink= b.add(FileIO.toFile(output, options = Set(CREATE, WRITE)))
      val echo = b.add(Flow[ByteString])
      bcast.out(0) ~> file_sink
      bcast.out(1) ~> echo.in
      FlowShape(bcast.in, echo.out)
    })



      val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
        println(conn.remoteAddress.toString)
        val stringToChange = conn.remoteAddress.toString
        val findPort= stringToChange.indexOf(":")
        val stringRemoteAddressNoPoints = stringToChange.substring(1,findPort)
        println(stringRemoteAddressNoPoints)
        val stringName=stringRemoteAddressNoPoints.replace('.','_')
        val outputFile = new File(s"res_$stringName.csv")
      //conn handleWith Flow[ByteString]
        //conn handleWith(serverLogic(conn))
        conn handleWith(serverLogic(outputFile))
      //conn.flow.toMat(FileIO.toFile(output))(Keep.right)
      //conn.flow.joinMat(saveFileFlow)(Keep.right).run()
        //val source= Source.empty
        val output = new File(s"./src/main/resources/res_${conn.remoteAddress}.csv")
        val saveFileFlow = Flow.fromSinkAndSourceMat(FileIO.toFile(output, options = Set(CREATE, WRITE)), Source.repeat(ByteString.empty))(Keep.both)
        //conn handleWith(saveFileFlow)

      //conn.flow.join(saveFileFlow).run()
    }

    //Flow.fromSinkAndSourceMat(FileIO.toFile(storageFile, options=Set(CREATE, WRITE)), Source.repeat(ByteString.empty))(Keep.left)
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


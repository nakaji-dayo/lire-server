import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, File, FileInputStream}
import java.nio.file.Paths
import javax.imageio.ImageIO

import net.semanticmetadata.lire.builders.{DocumentBuilder, GlobalDocumentBuilder}
import com.twitter.finagle.Http
import com.twitter.util.Await
import io.finch._
import net.semanticmetadata.lire.imageanalysis.features.global.{EdgeHistogram, JCD}
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher
import net.semanticmetadata.lire.utils.LuceneUtils
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.store.FSDirectory
import io.finch.circe._, io.circe.generic.auto._
import sun.misc.BASE64Decoder

import scala.collection.mutable.ListBuffer

/**
  * Created by daishi on 2016/03/03.
  */
object Main {
  val hello: Endpoint[String] = get("hello") {
    Ok("hello")
  }


  def getFeatureClass(feature: String) = {
    feature match {
      case "JCD" => classOf[JCD]
      case "EdgeHistogram" => classOf[EdgeHistogram]
      case _ => classOf[JCD]
    }
  }

  case class SearchQuery(image: String, limit: Option[Int], feature: String)

  val search: Endpoint[List[SearchResult]] = post("search" :: body.as[SearchQuery]) { q: SearchQuery =>
    search("./index", q.image, q.limit.getOrElse(10), q.feature) match {
      case Some(res) => Ok(res)
      case _ => InternalServerError(new Exception("any error"))
    }
  }

  val service = (hello :+: search).toService

  def main(args: Array[String]) {
    indexing("./index", args(0))
    Await.ready(Http.server.serve(":9090", service))
  }

  def indexing(indexDir: String, imageDir: String): Unit = {
    var passed = false
    val f = new File(imageDir)
    System.out.println("Indexing images in " + imageDir)
    if (f.exists() && f.isDirectory()) passed = true
    if (!passed) {
      System.out.println("No directory given as first argument.")
      System.out.println("Run \"Indexer <directory>\" to index files of a directory.")
      System.exit(1)
    }
    // Getting all images from a directory and its sub directories.
    val images = recursiveListFiles(f)

    // Creating a CEDD document builder and indexing all files.
    val globalDocumentBuilder = new GlobalDocumentBuilder(classOf[JCD])
    globalDocumentBuilder.addExtractor(classOf[EdgeHistogram])
    new GlobalDocumentBuilder()
    // Creating an Lucene IndexWriter
    val iw = LuceneUtils.createIndexWriter(indexDir, true, LuceneUtils.AnalyzerType.WhitespaceAnalyzer);
    // Iterating through images building the low level features
    for (imageFilePath <- images) {
      System.out.println("Indexing " + imageFilePath);
      try {
        val str = imageFilePath.toString
        val img = ImageIO.read(new FileInputStream(str))
        val document = globalDocumentBuilder.createDocument(img, str)
        iw.addDocument(document)
      } catch {
        case e: Throwable => {
          System.err.println("Error reading image or indexing it.")
          e.printStackTrace()
        }
      }
    }
    // closing the IndexWriter
    LuceneUtils.closeWriter(iw)
    System.out.println("Finished indexing.")
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  def search(indexDir: String, base64Image: String, limit: Int, feature: String): Option[List[SearchResult]] = {
    // Checking if arg[0] is there and if it is an image.
    return decodeBase64Image(base64Image).map(image => {
        val ir = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
        val searcher = new GenericFastImageSearcher(limit, getFeatureClass(feature));
        // searching with a image file ...
        val hits = searcher.search(image, ir);
        // searching with a Lucene document instance ...
        var result = ListBuffer[SearchResult]()
        for (i <- 0 until hits.length()) {
          val doc = ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER);
          val fileName = doc(0)
          result += SearchResult(fileName, hits.score(i))
        }
        println("search result:" + result.toList.length + " hits")
        result.toList
      })
    }

    def decodeBase64Image(str: String): Option[BufferedImage] = {
    try {
      val decoder = new BASE64Decoder()
      val imageByte = decoder.decodeBuffer(str)
      val bis = new ByteArrayInputStream(imageByte)
      val image = ImageIO.read(bis)
      bis.close()
      return Some(image);
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        return None
      }
    }
  }
}

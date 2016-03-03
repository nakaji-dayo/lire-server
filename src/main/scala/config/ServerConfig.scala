package config

/**
  * Created by daishi on 2016/03/04.
  */
trait ServerConfig {
  val port: Int
  val indexPath: String
  val imagesDir: Option[String]
}
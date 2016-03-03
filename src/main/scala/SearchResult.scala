import argonaut.{Argonaut, CodecJson}

/**
  * Created by daishi on 2016/03/04.
  */
case class SearchResult(name: String, score: Double)

object SearchResult {
  implicit def SearchResultCodecJson: CodecJson[SearchResult] =
    Argonaut.casecodec2(SearchResult.apply, SearchResult.unapply)("name", "score")
}
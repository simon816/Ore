package util

import org.pegdown.PegDownProcessor

object Markdown {

  private val processor = new PegDownProcessor

  /**
    * Converts the specified markdown string to HTML.
    *
    * @param markdown   Markdown string
    * @return           HTML string
    */
  def process(markdown: String): String = processor.markdownToHtml(markdown)

}

package util

import org.pegdown.PegDownProcessor

object Markdown {

  private val processor = new PegDownProcessor

  def process(markdown: String): String = processor.markdownToHtml(markdown)

}

package com.leapfin.lpfnfinder

trait InfiniteStreamProvider {
  def randomAlphaGen: Iterator[Char]

  def stream: Stream[Char] = {
    def loop(s: Char): Stream[Char] = s #:: loop(randomAlphaGen.next())
    loop(randomAlphaGen.next())
  }
}

trait AlphabeticGenerator extends InfiniteStreamProvider {
  def randomAlphaGen: Iterator[Char] = new scala.util.Random().alphanumeric.dropWhile(_.isDigit).iterator
}

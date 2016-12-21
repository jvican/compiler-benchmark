/*                                                                      *\
** Squants                                                              **
**                                                                      **
** Scala Quantities and Units of Measure Library and DSL                **
** (c) 2013-2015, Gary Keorkunian                                       **
**                                                                      **
\*                                                                      */

package squants.time

import squants._

/**
 * Represents a quantity of frequency, which is the number cycles (count) over time
 *
 * @author  garyKeorkunian
 * @since   0.1
 *
 * @param value Double
 */
final class Frequency private (val value: Double, val unit: FrequencyUnit)
    extends Quantity[Frequency]
    with TimeDerivative[Dimensionless] {

  def dimension = Frequency

  protected[squants] def timeIntegrated = Each(toHertz)
  protected[squants] def time = Seconds(1)

  def toHertz = to(Hertz)
  def toKilohertz = to(Kilohertz)
  def toMegahertz = to(Megahertz)
  def toGigahertz = to(Gigahertz)
  def toTerahertz = to(Terahertz)
  def toRevolutionsPerMinute = to(RevolutionsPerMinute)
}

object Frequency extends Dimension[Frequency] {
  private[time] def apply[A](n: A, unit: FrequencyUnit)(
      implicit num: Numeric[A]) = new Frequency(num.toDouble(n), unit)
  def apply = parse _
  def name = "Frequency"
  def primaryUnit = Hertz
  def siUnit = Hertz
  def units =
    Set(Hertz,
        Kilohertz,
        Megahertz,
        Gigahertz,
        Terahertz,
        RevolutionsPerMinute)
}

trait FrequencyUnit extends UnitOfMeasure[Frequency] with UnitConverter {
  def apply[A](n: A)(implicit num: Numeric[A]) = Frequency(n, this)
}

object Hertz extends FrequencyUnit with PrimaryUnit with SiUnit {
  val symbol = "Hz"
}

object Kilohertz extends FrequencyUnit {
  val conversionFactor = MetricSystem.Kilo
  val symbol = "kHz"
}

object Megahertz extends FrequencyUnit {
  val conversionFactor = MetricSystem.Mega
  val symbol = "MHz"
}

object Gigahertz extends FrequencyUnit {
  val conversionFactor = MetricSystem.Giga
  val symbol = "GHz"
}

object Terahertz extends FrequencyUnit {
  val conversionFactor = MetricSystem.Tera
  val symbol = "THz"
}

object RevolutionsPerMinute extends FrequencyUnit {
  val conversionFactor = 1d / 60d
  val symbol = "rpm"
}

object FrequencyConversions {
  implicit class FrequencyConversions[A](n: A)(implicit num: Numeric[A]) {
    def hertz = Hertz(n)
    def kilohertz = Kilohertz(n)
    def megahertz = Megahertz(n)
    def gigahertz = Gigahertz(n)
    def terahertz = Terahertz(n)
    def rpm = RevolutionsPerMinute(n)
  }

  implicit object FrequencyNumeric
      extends AbstractQuantityNumeric[Frequency](Frequency.primaryUnit)
}

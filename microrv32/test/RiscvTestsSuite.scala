package core.microrv32

import spinal.core._
import spinal.core.sim._
import org.scalatest.funsuite.AnyFunSuite
import scala.io.Source

// =====================================================================
//  Regresja po porcie na Scala 2.13 / SpinalHDL 1.12: oficjalne
//  riscv-tests (sw/riscv-test-rv32imc-prebuilt, rv32ui/um/uc) na
//  MicroRV32Top (rv32imc). SoC kompilowany raz; przed kazdym testem
//  program wpisywany do bankow RAM, potem reset. Wynik: pierwszy fetch
//  spod adresu <pass> albo <fail> z pliku .dump testu.
//    sbt test
// =====================================================================
class RiscvTestsSuite extends AnyFunSuite {
  val dir   = new java.io.File("sw/riscv-test-rv32imc-prebuilt")
  val tests = dir.listFiles().map(_.getName).filter(_.endsWith(".hex")).map(_.stripSuffix(".hex")).sorted.toSeq
  val words = 8704                                  // najwiekszy prebuilt test (rv32uc-p-rvc ma 3092 - dopelniany zerami)
  def lines(name : String) = Source.fromFile(new java.io.File(dir, s"$name.hex")).getLines().count(_.trim.nonEmpty)
  val initHex = tests.find(lines(_) == words).get   // NIE lazy: elaboracja Spinala biegnie w osobnym watku, a `compiled` trzyma monitor obiektu

  def hexWords(name : String) : Seq[Long] =
    Source.fromFile(new java.io.File(dir, s"$name.hex")).getLines().map(_.trim).filter(_.nonEmpty).map { l =>
      val b = l.grouped(2).map(java.lang.Long.parseLong(_, 16)).toSeq          // bajty w kolejnosci adresow
      b(0) | (b(1) << 8) | (b(2) << 16) | (b(3) << 24)
    }.toSeq

  /** Adres etykiety z .dump; None, gdy test jej nie ma (rv32ui-p-simple konczy sie
    * wprost zapisem do Shutdown: 0x02010000 = koniec, +4 = blad). */
  def label(name : String, sym : String) : Option[Long] =
    Source.fromFile(new java.io.File(dir, s"$name.dump")).getLines()
      .collectFirst { case l if l.trim.endsWith(s"<$sym>:") => java.lang.Long.parseLong(l.trim.split(" ")(0), 16) }

  lazy val compiled = SimConfig
    .withConfig(SpinalConfig(defaultClockDomainFrequency = FixedFrequency(12 MHz)))
    .withVerilator.workspacePath("simWorkspace").workspaceName("riscv_tests")
    .compile {
      val top = new MicroRV32Top(new java.io.File(dir, s"$initHex.hex").getPath, words,
                                 RV32CoreConfig(generateMultiply = true, generateDivide = true, generateCompressed = true))
      top.cpu.cpu.programCounter.simPublic()
      top.cpu.cpu.CSRLogic.newFetch.simPublic()
      top.ram.memBank0.simPublic()
      top.ram.memBank1.simPublic()
      top.shutdown_periph.io.halt.simPublic()
      top.shutdown_periph.io.haltErr.simPublic()
      top
    }

  for (t <- tests) test(t) {
    val prog = hexWords(t).padTo(words, 0L)
    assert(prog.size == words, s"$t: ${prog.size} words > $words")
    val (pass, fail) = (label(t, "pass"), label(t, "fail"))
    compiled.doSim(t) { d =>
      for ((w, i) <- prog.zipWithIndex) {
        d.ram.memBank0.setBigInt(i, BigInt(w & 0xffff))
        d.ram.memBank1.setBigInt(i, BigInt(w >>> 16))
      }
      d.io.uart.rxd #= true
      d.clockDomain.forkStimulus(10)
      d.clockDomain.assertReset(); d.clockDomain.waitSampling(5); d.clockDomain.deassertReset()
      var result : Option[Boolean] = None
      var n = 0
      while (result.isEmpty && n < 200000) {
        d.clockDomain.waitSampling(); n += 1
        if (d.cpu.cpu.CSRLogic.newFetch.toBoolean) {
          val pc = d.cpu.cpu.programCounter.toLong
          if (pass.contains(pc)) result = Some(true)
          if (fail.contains(pc)) result = Some(false)
        }
        if (d.shutdown_periph.io.haltErr.toBoolean) result = Some(false)
        else if (pass.isEmpty && d.shutdown_periph.io.halt.toBoolean) result = Some(true)
      }
      assert(result.contains(true), s"$t: ${result.fold("timeout")(_ => "reached <fail>")} after $n cycles")
    }
  }
}

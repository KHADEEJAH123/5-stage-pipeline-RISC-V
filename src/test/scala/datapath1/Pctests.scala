package datapath1
import chisel3._
import org.scalatest._
import chiseltest._

class Pctests extends FreeSpec with ChiselScalatestTester{
    "pc tests" in {
    test(new Pc()){c=>
    c.io.in.poke(0.U)
    c.clock.step(1)
    c.io.pc4.expect(4.U)
    c.io.pc.expect(0.U)

}}}
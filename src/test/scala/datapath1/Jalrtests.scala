package datapath1
import chisel3._
import org.scalatest._
import chiseltest._

class Jalrtests extends FreeSpec with ChiselScalatestTester{
    "JALR tests" in {
    test(new Jalr()){c=>
        c.io.imm.poke(1.S)
        c.io.rs1.poke(1.S)
	    c.clock.step(1)
	    c.io.output.expect(2.S)
}}}
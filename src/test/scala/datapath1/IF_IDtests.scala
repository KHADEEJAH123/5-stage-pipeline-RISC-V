package datapath1
import chisel3._
import org.scalatest._
import chiseltest._

class IF_IDtests extends FreeSpec with ChiselScalatestTester{
    "if id tests" in {
    test(new IF_ID()){c=>
         c.io.pc_in.poke(0.U)
        c.io.pc4_in.poke(0.U)
        c.io.ins_in.poke(10.U)
        c.io.ins_out.expect(0.U)
        c.clock. step(1)
        c.io.ins_out.expect(10.U)
}}}
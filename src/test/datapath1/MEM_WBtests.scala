package datapath1

import chisel3._
import org.scalatest._
import chiseltest._

class  MEM_WBtests extends FreeSpec with ChiselScalatestTester{
    "MEM WR TESTS" in {
    test(new MEM_WB()){c=>
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)

}}}
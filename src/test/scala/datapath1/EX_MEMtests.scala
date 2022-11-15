package datapath1

import chisel3._
import org.scalatest._
import chiseltest._

class  EXE_MEMtests extends FreeSpec with ChiselScalatestTester{
    "EXE MEM TESTS" in {
    test(new EXE_MEM()){c=>
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)

}}}
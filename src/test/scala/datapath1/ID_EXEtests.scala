package datapath1

import chisel3._
import org.scalatest._
import chiseltest._

class  ID_EXEtests extends FreeSpec with ChiselScalatestTester{
    "ID EXE TESTS" in {
    test(new ID_EXE()){c=>
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)
       c.clock.step(1)

}}}
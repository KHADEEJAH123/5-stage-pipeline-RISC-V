package datapath1
import chisel3._
import org.scalatest._
import chiseltest._

class Toptests extends FreeSpec with ChiselScalatestTester{
    "top tests" in {
    test(new Top()){c=>
    c.clock.step(100)
}}}
  
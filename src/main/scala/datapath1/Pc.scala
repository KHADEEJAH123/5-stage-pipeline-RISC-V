package datapath1
import chisel3._

class Pc extends Module {
    val io = IO(new Bundle{
    val in = Input(UInt(32.W))
    val pc4 = Output(UInt(32.W))
    val pc = Output(UInt(32.W))

  })
  val reg = RegInit(0.U(32.W))
  reg := io.in
  io.pc4 := reg + 4.U
  io.pc := reg
}
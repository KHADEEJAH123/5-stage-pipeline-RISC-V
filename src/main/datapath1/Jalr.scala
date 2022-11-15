package datapath1
import chisel3._

class Jalr extends Module {
    val io = IO(new Bundle {
            val imm = Input(SInt(32.W))
            val rs1 = Input(SInt(32.W))
            val output = Output(SInt(32.W))
    })

    val sum = io.imm + io.rs1
    io.output := sum & 4284967294L.S 
}
package torus

import chisel3._
import chisel3.util._
import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy.LazyModule
import freechips.rocketchip.config.{Config, Parameters}
import chisel3.util.HasBlackBoxResource
import chisel3.experimental.IntParam
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.InOrderArbiter

class PE extends Module{
    val io = IO(new Bundle{
        val cmd = Flipped(Decoupled(new Command)) // inputs : bits, valid (out : ready)
        val resp = Decoupled(new Response) // inputs : ready (out : bits, valid)
        val done = Output(Bool())
        val ready = Input(Bool())
    })

    //register memory
    val reg_memory = Mem(32, UInt(32.W))

    // conventions
    // load: funct == 0.U , rd := mem(rs1)
    // store: funct == 1.U, mem(rs2) := rs1

    val is_load = io.cmd.bits.funct === 0.U
    val is_store = io.cmd.bits.funct === 1.U 


    when(is_load){
        io.resp.bits.data := reg_memory(io.cmd.bits.rs1)
    }.elsewhen(is_store){
        reg_memory(io.cmd.bits.rs2) := io.cmd.bits.rs1
        io.resp.bits.data := io.cmd.bits.rs1

    }

    val idle :: exec :: Nil = Enum(2)

    val state = Reg(Bits(2.W))
    
    state := idle

    when(state === idle){
        io.done := false.B 
        when(io.ready){
            state := exec
        }.otherwise{
            state := idle
        }

    }.elsewhen(state === exec){
        io.done := true.B
        when(io.done){
            state := idle
        }.otherwise{
            state := exec
        }
    }


}
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


class Command extends Bundle{
    val funct = Bits(7.W)
    val rd = Bits(5.W)
    val opcode = Bits(7.W) 
    val rs1 = Bits(32.W)
    val rs2 = Bits(32.W)
}

class Response extends Bundle{
    val data = Bits(32.W)
}

class Controller extends Module{
    val io = IO(new Bundle{
        val rocc = new RoCCCoreIOCustom
        val cmd = new Command // inputs : bits, valid (out : ready)
        val resp = new Response // inputs : ready (out : bits, valid)
        val done = Input(Bool())
        val ready = Output(Bool())
        
    })

    // renaiming to not get crazy

    val rocc = io.rocc
    val cmd = io.cmd
    val resp = io.resp
    val done = io.done
    val ready = io.ready
    val in_valid = io.rocc.cmd.valid 

    // connecting inputs
    io.cmd.funct := io.rocc.cmd.bits.inst.funct
    io.cmd.rd := io.rocc.cmd.bits.inst.rd 
    io.cmd.opcode := io.rocc.cmd.bits.inst.opcode
    io.cmd.rs1 := io.rocc.cmd.bits.rs1
    io.cmd.rs2 := io.rocc.cmd.bits.rs2

    // connecting outputs
    io.rocc.cmd.ready := state === idle
    io.rocc.resp.bits.data := io.resp.data
    io.rocc.resp.valid := state === give_result

    // handling the return register
    val rd_address = Reg(Bits(5.W))

    // if state is idle, it means there is no op waiting to do anything, otherwise 
    //this value has to be stored till the next one will be required to be stored(next idle state)
    rd_address := Mux( state === idle, io.rocc.cmd.bits.inst.rd, rd_address)

    io.rocc.resp.bits.rd := rd_address

    // pseudo-default values
    io.rocc.interrupt := false.B 

    // state machine logic
    val idle :: exec :: give_result :: Nil = Enum(3)

    val state = Reg(Bits(2.W))

    state := idle

    when(state === idle){
    rocc.busy := false.B 
    rocc.cmd.ready := true.B 
    rocc.resp.valid := false.B
    io.ready := false.B 
    when(in_valid){
      state := exec
    }.otherwise{
      state := idle
    }
  }.elsewhen(state === exec){
    io.ready := true.B 
    rocc.busy := true.B
    rocc.cmd.ready := false.B 
    rocc.resp.valid := false.B
    when(done){
        state := give_result
    }.otherwise{
        state := exec
    }
    
  }.elsewhen(state === give_result){
    io.ready := false.B 
    rocc.busy := true.B
    rocc.cmd.ready := false.B 
    rocc.resp.valid := true.B
    state := idle
  }.otherwise{ // shouldn't happen
  io.ready := false.B 
  rocc.busy := false.B
  rocc.cmd.ready := false.B 
  rocc.resp.valid := false.B 
  state := idle
  
  }

    

}
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




/*
this module represents the controller, it forwards and broadcasts the main 
signals coming from the rocCC core, and eventually filters them
then it waits until every PE is done thrugh the "done" signal and returns the values

it has the bundle interface:
    rocc : interface through which receives the signals from the prcessor
    cmd : custom command containing only the useful info used by the PEs coming from the core
    resp : cosutom response containing only the useful info computed by the PEs
    done : signal used by the PEs to communicate the fact that they are done with the computation
            , when all the PEs are done, this signal is 1
    ready : signal used to communicate with the PEs, in particular, it is 1 during the 
            whole computation, and when it is 1 it gives the input to all the PEs to
            start their computaion
*/
class Controller extends Module{
    val io = IO(new Bundle{
        val rocc = new RoCCCoreIOCustom
        val cmd = new Command 
        val resp = new Response 
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
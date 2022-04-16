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
        val cmd = Output(new Command) // here cmd is an output because it filters the rocc signals and forewards them to PE
        val resp = Input(new Response) // here resp is an input because it gets the relevant data from PE
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

    // state machine declaration
    val idle :: exec :: give_result :: wait_result :: Nil = Enum(4)
    val state = Reg(Bits(2.W))

    // output data buffer, needed because we have to locally store the result
    // until the core is ready to receive it
    val reg_resp_data = Reg(Bits(32.W))


    // connecting inputs
    cmd.funct := rocc.cmd.bits.inst.funct
    cmd.opcode := rocc.cmd.bits.inst.opcode
    cmd.rs1 := rocc.cmd.bits.rs1
    cmd.rs2 := rocc.cmd.bits.rs2

    // connecting outputs
    rocc.cmd.ready := state === idle // the controller is ready when it is in idle state
    reg_resp_data := Mux(state === wait_result, reg_resp_data, resp.data)
    rocc.resp.bits.data := reg_resp_data
    rocc.resp.valid := state === give_result || state === wait_result // the controller has relevant data to return when the state is give_result

    // handling the return register
    val rd_address = Reg(Bits(5.W))

    // if state is idle, it means there is no op waiting to do anything, otherwise 
    //this value has to be stored till the next one will be required to be stored(next idle state)
    rd_address := Mux( state === idle, rocc.cmd.bits.inst.rd, rd_address)

    rocc.resp.bits.rd := rd_address

    // pseudo-default values
    rocc.interrupt := false.B 

    // state machine logic

    state := idle

    when(state === idle){
    rocc.busy := false.B 
    rocc.cmd.ready := true.B
    ready := false.B 
    when(in_valid){
      state := exec
    }.otherwise{
      state := idle
    }
  }.elsewhen(state === exec){
    rocc.busy := true.B
    ready := true.B
    when(done && rocc.resp.ready){
        state := give_result
    }.elsewhen(done && !rocc.resp.ready){
      state := wait_result
    }.otherwise{
        state := exec
    }
    
  }.elsewhen(state === wait_result){

    rocc.busy := true.B 
    ready := false.B 
    when(rocc.resp.ready){
      state := give_result
    }.otherwise{
      state := wait_result
    }
    
  
  }.elsewhen(state === give_result){
    rocc.busy := true.B
    ready := false.B 
    state := idle

  }.otherwise{ // shouldn't happen
  
  ready := false.B 
  rocc.busy := false.B
  state := idle
  
  }

    

}
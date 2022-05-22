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
import java.io.ObjectInputFilter


/*
this module represents the controller, it forwards and broadcasts the main 
signals coming from the rocCC core, and eventually filters them
then it waits until every PE is done thrugh the "done" signal and returns the values

it has the bundle interface:
    rocc : interface through which receives the signals from the prcessor
    cmd : custom command containing only the useful info used by the PEs coming from the core
    resp : cosutom response containing only the useful info computed by the PEs
*/



class Controller(queue_size : Int = 5) extends Module{
    val io = IO(new Bundle{
        val rocc = new RoCCCoreIOCustom
        val cmd = Decoupled(new Command) // here cmd is an output because it filters the rocc signals and forewards them to PE
        val resp = Flipped(Decoupled(new Response)) // here resp is an input because it gets the relevant data from PE
        // cmd: inputs: ready, outputs: bits, valid
        // resp: inputs: bits, valid, outputs: ready
    })

    // convention:
      // load: funct === 0.U
      // store: funct === 1.U 
      // get_load: funct === 2.U
    

    // renaiming to not get crazy
    val rocc = io.rocc
    val cmd = io.cmd
    val resp = io.resp
    val in_valid = rocc.cmd.valid 
    
    val is_get_load = rocc.cmd.bits.inst.funct === 2.U 

    val q_in_cmd = Wire(Flipped(Decoupled(new Command)))
    val in_q = Queue(q_in_cmd, queue_size)
    val out_q = Queue(resp, queue_size)

    q_in_cmd.valid := in_valid && !is_get_load
    q_in_cmd.bits.rs1 := rocc.cmd.bits.rs1
    q_in_cmd.bits.rs2 := rocc.cmd.bits.rs2
    q_in_cmd.bits.funct := rocc.cmd.bits.inst.funct

    rocc.cmd.ready := Mux(in_valid && is_get_load , rocc.resp.ready, q_in_cmd.ready)

    cmd <> in_q

    out_q.ready := rocc.resp.ready && in_valid && is_get_load 

    rocc.resp.valid := (out_q.valid || !is_get_load) && in_valid // makes sense to return a valid resp only if the command is valid as well???
    rocc.resp.bits.data := Mux(in_valid && is_get_load, out_q.bits.data, 13.U)
    rocc.resp.bits.rd := rocc.cmd.bits.inst.rd


    rocc.interrupt := false.B 
    rocc.busy := !rocc.cmd.ready // true when command is not ready

}

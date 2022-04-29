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
      // do_load: funct === 0.U
      // get_load: funct === 2.U
    

    // renaiming to not get crazy
    val rocc = io.rocc
    val cmd = io.cmd
    val resp = io.resp
    val in_valid = io.rocc.cmd.valid 

    val in_filter = Module(new InputFilter())
    val out_filter = Module(new OutputFilter())

    val f_in_cmd = Wire(Decoupled(new Command))
    val f_out_resp = Wire(Flipped(Decoupled(new Response)))

    in_filter.io.rocc_cmd <> rocc.cmd
    in_filter.io.cmd <> f_in_cmd

    val q_in_cmd = Wire(Flipped(Decoupled(new Command)))
    val q_out_resp = Wire(Decoupled(new Response))

    val in_q = Queue(q_in_cmd, queue_size)
    val out_q = Queue(resp, queue_size)

    out_q <> q_out_resp

    val get_load = in_filter.io.cmd.bits.funct === 2.U

    // connecting left components
    q_in_cmd.bits.rs1 := f_in_cmd.bits.rs1
    q_in_cmd.bits.rs2 := f_in_cmd.bits.rs2
    q_in_cmd.bits.funct := f_in_cmd.bits.funct
    q_in_cmd.valid := !(get_load) && f_in_cmd.valid

    in_filter.io.cmd.ready := Mux(get_load && f_in_cmd.valid, f_out_resp.ready, q_in_cmd.ready)

    out_q.ready := f_out_resp.ready && get_load && f_in_cmd.valid

    


    out_filter.io.rocc_resp <> rocc.resp
    out_filter.io.resp <> f_out_resp
    out_filter.io.rd := /*RegNext(*/ rocc.cmd.bits.inst.rd /*)*/ ????

    f_out_resp.valid := q_out_resp.valid
    f_out_resp.bits.data := Mux( get_load && f_in_cmd.valid, q_out_resp.bits.data, 0.U )

    io.rocc.interrupt := false.B 
    io.rocc.busy := false.B ????

}


class InputFilter() extends Module{

  val io = IO(new Bundle{

    val rocc_cmd = Flipped(Decoupled(new RoCCCommandCustom)) // inputs : bits, valid (out : ready)
    val cmd = Decoupled(new Command) // inputs : ready (out : bits, valid)
  })

  val cmd = io.cmd
  val rocc_cmd = io.rocc_cmd

  // connecting inputs
  cmd.bits.rs1 := rocc_cmd.bits.rs1
  cmd.bits.rs2 := rocc_cmd.bits.rs2
  cmd.bits.funct := rocc_cmd.bits.inst.funct
  cmd.valid := rocc_cmd.valid

  // connecting outputs
  rocc_cmd.ready := cmd.ready

}

class OutputFilter() extends Module{

  val io = IO(new Bundle{

    val rocc_resp = Decoupled(new RoCCResponseCustom) // inputs : ready (out : bits, valid) 
    val resp = Flipped(Decoupled(new Response)) // resp: inputs: bits, valid, (out: ready)
    val rd = Input(Bits(32.W))

  })

  val rocc_resp = io.rocc_resp
  val resp = io.resp 

  // connecting inputs
  rocc_resp.bits.rd := io.rd
  rocc_resp.bits.data := resp.bits.data
  rocc_resp.valid := resp.valid
  // connecting outputs
  resp.ready := rocc_resp.ready


}
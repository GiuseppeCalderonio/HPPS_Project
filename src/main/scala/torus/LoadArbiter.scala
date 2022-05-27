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
import javax.lang.model.element.ModuleElement


// n should be already the n*n value
class LoadArbiter(width : Int = 16, n : Int) extends Module{

    val io = IO(new Bundle{

        val in_vec = Vec(n, Input(Bits(width.W )))
        val in_valid = Input(Bool())
        val in_ready = Output(Bool())

        val out = Decoupled(new Response())
        val test = Output(Bits(width.W ))

    })


    // renaiming

    val in_vec = io.in_vec
    val in_valid = io.in_valid
    val in_ready = io.in_ready
    val out = io.out

    // counter

    val counter = Module(new Counter(width))

    val count_done = Wire(Bool())
    val counter_result = Wire(Bits(width.W ))
    val stall = !out.ready

    counter.io.stall := stall
    counter.io.reset_ := in_valid
    counter.io.input := (n).U 

    count_done := counter.io.done
    counter_result := counter.io.value

    

    // set of buffers

    val buffer_memory = Reg(Vec(n, Bits(width.W )))

    io.test := buffer_memory(1)
/*
    val first_buffer_output = Mux(in_valid, in_vec(0), buffer_memory(0))

    buffer_memory(0) := Mux(count_done, first_buffer_output, in_vec(0))
*/
    for (i <- 0/*1*/ until n){
        buffer_memory(i) := Mux(Mux(in_valid, false.B , !count_done), buffer_memory(i), in_vec(i))
    }

    // output logic

    val is_first_iteration = in_valid === 1.U 

    when(is_first_iteration){
        out.bits.data := in_vec(0)
    }.otherwise{
        out.bits.data := buffer_memory(counter_result)
    }

    // output signals

    in_ready := (count_done || in_valid) && out.ready
    out.valid := !count_done // && out.ready

}
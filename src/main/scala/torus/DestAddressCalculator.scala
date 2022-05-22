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

class DestAddressCalculator(width: Int = 32) extends Module{


    val io = IO(new Bundle{

        val in_dest_addr = Input(Bits(width.W))
        val in_dest_offset = Input(Bits(width.W ))
        val count_done = Input(Bool())
        val out_dest_addr = Output(Bits(width.W ))


    })

    val in = Wire(Flipped(Decoupled(Bits(width.W))))
    in.valid := true.B 
    in.bits := io.in_dest_addr

    val out = Queue(in, 1)

    out.ready := io.count_done
    io.out_dest_addr := Mux(io.count_done, io.in_dest_addr, out.bits) + io.in_dest_offset

}
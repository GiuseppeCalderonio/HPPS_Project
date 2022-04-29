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
this module iplements a particular type of register,
it is initialized with 1(no particluar eason for this choice), and:
    if in the i-CK keep = 0, in = val-i, out = 30 -> in the i+1-CK out = val-i
    if in the j-CK keep = 1, in = val-j, out = 30 -> in the j+1-CK out = 30

*/

class KeepReg(width : Int) extends Module{

    val io = IO(new Bundle{
        val keep = Input(Bool())
        val in = Input(Bits(width.W))
        val out = Output(Bits(width.W))
    })

    val reg = RegInit(Bits(width.W), 1.U)

    reg := Mux(io.keep, reg, io.in)

    io.out := reg
}
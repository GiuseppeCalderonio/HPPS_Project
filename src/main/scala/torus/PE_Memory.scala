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

/*
this module executes basic memory operations:
    load: when is_load === true
    store: when is_store === true

    if is_load === true and is_store === true the behaviour is udefined
*/
class PE_memory(width : Int = 32) extends Module{

    val io = IO(new Bundle{
        val rs1 = Input(Bits(width.W))
        val rs2 = Input(Bits(width.W))
        val is_load = Input(Bool())
        val is_store = Input(Bool())
        val result = Output(Bits(width.W))
    })

    // conventions
    // load: funct == 0.U , result := mem(rs2)
    // store: funct == 1.U, mem(rs2) := rs1

    //register memory
    val reg_memory = Mem(width, UInt(width.W))

    // renaming
    val rs1 = io.rs1
    val rs2 = io.rs2
    val is_load = io.is_load
    val is_store = io.is_store
    val result = io.result
    
    when(is_load){ // does a load
        result := reg_memory(rs2)
    }.elsewhen(is_store){ // does a store
        reg_memory(rs2) := rs1
        result := 100.U // assuming width <= 7
    }.otherwise{
        result := 50.U 
    }

}
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

class Neighbour() extends Bundle{
    val data = Bits(32.W) 
    val address = Bits(32.W )
}

class PE_SideMemory(width: Int = 32, pe_address: Int) extends Module{

    val io = IO(new Bundle{

        val neighbour = Flipped(Decoupled(new Neighbour()))
        val rs1 = Input(Bits(width.W ))
        val rs2 = Input(Bits(width.W ))
        val is_load = Input(Bool())
        val is_store = Input(Bool())
        val cmd_valid = Input(Bool())
        val busy = Input(Bool())

        val pe_busy = Output(Bool())
        val op_result = Output(Bits(width.W ))

    })

    // renaiming
    val neighbour = io.neighbour
    val rs1 = io.rs1
    val rs2 = io.rs2
    val is_load = io.is_load
    val is_store = io.is_store
    val cmd_valid = io.cmd_valid
    val pe_busy = io.pe_busy

    val busy = io.busy
    val op_result = io.op_result


    val reg_memory = Module(new PE_memory(width))

    val mem_rs1 = Wire(Bits(width.W))
    val mem_rs2 = Wire(Bits(width.W ))
    val mem_load = Wire(Bool())
    val mem_store = Wire(Bool())

    // this signal says whether the PE in which the load/store has to be done is this or not
    // basically each pe is addressed by the 3 bits of rs2 (18, 16) and if it is equal to the 
    // address specified at creation time the load/store is accepted
    val is_pe_address_valid = rs2(18, 16) === pe_address.U 

    reg_memory.io.rs1 := mem_rs1
    reg_memory.io.rs2 := mem_rs2
    reg_memory.io.is_load := mem_load
    reg_memory.io.is_store := mem_store

    op_result := reg_memory.io.result

    pe_busy := neighbour.valid
    neighbour.ready := true.B 

    when(neighbour.valid){
        mem_store := true.B 
        mem_load := false.B 
        mem_rs1 := neighbour.bits.data
        mem_rs2 := neighbour.bits.address
    }.elsewhen(cmd_valid && is_pe_address_valid && !busy){
        mem_store := is_store
        mem_load := is_load
        mem_rs1 := rs1
        mem_rs2 := rs2
    }.otherwise{
        mem_load := false.B 
        mem_store := false.B 
        mem_rs1 := 0.U 
        mem_rs2 := 0.U 
    }

} 

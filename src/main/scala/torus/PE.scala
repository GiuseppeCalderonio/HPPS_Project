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

class PE extends Module{
    val io = IO(new Bundle{
        val cmd = Input(new Command) 
        val resp = Output(new Response) 
        val done = Output(Bool())
        val ready = Input(Bool())
    })

    //register memory
    val reg_memory = Mem(32, UInt(32.W))

    // conventions
    // load: funct == 0.U , rd := mem(rs1)
    // store: funct == 1.U, mem(rs2) := rs1

    // registers are needen otherwise they are not synchronized with the 
    // state machine
    val is_load = Reg(Bool())
    val is_store = Reg(Bool())

    

    // state machine logic

    val idle :: exec :: Nil = Enum(2)

    val state = Reg(Bits(2.W))
    
    state := idle

    when(state === idle){
        io.done := false.B 
        when(io.ready){
            state := exec
        }.otherwise{
            state := idle
        }

    }.elsewhen(state === exec){
        io.done := true.B 
        
        when(io.done){
            state := idle
        }.otherwise{
            state := exec
        }
        
    }.otherwise{ // it should never happen
        io.done := false.B 
    }

    val reg_rs1 = Reg(Bits(32.W))
    val reg_rs2 = Reg(Bits(32.W))

    reg_rs1 := Mux(state === idle, io.cmd.rs1, reg_rs1)
    reg_rs2 := Mux(state === idle, io.cmd.rs2, reg_rs2)

    is_load := Mux(state === idle, io.cmd.funct === 0.U, is_load)
    is_store := Mux(state === idle, io.cmd.funct === 1.U, is_store)


    // data path

    when(is_load && state === exec){
        io.resp.data := reg_memory(reg_rs1)
    }.elsewhen(is_store && state === exec){
        reg_memory(reg_rs2) := reg_rs1
        io.resp.data := reg_rs1
    }.otherwise{
        io.resp.data := 0.U 
    }


}
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
class Counter(width: Int = 32) extends Module{

    val io = IO(new Bundle{
        val input = Input(Bits(width.W))
        val reset_ = Input(Bool())
        val stall = Input(Bool())
        val value = Output(Bits(width.W))
        val done = Output(Bool())
        val equal_out = Output(Bool())
        val output = Output(Bits(width.W))
    })

    val input = io.input
    val reset_ = io.reset_
    val stall = io.stall
    val value = io.value
    val done = io.done

    // useful additional signlas

    val input_buffer = Reg(Bits(width.W))
    val register_counter = RegInit(Bits(width.W), 0.U )
    val count = Wire(Bits(width.W))
    val in = Wire(Bits(width.W))

    val equal = Wire(Bool()) 

    val is_input_zero = input === 0.U 

    

    val mux = Wire(Bool())

    // state machine 

    val state_machine = Module(new Counter_StateMachine())

    state_machine.io.equal := equal
    state_machine.io.reset_ := reset_
    state_machine.io.is_input_zero := is_input_zero 
    
    done := /*RegNext(*/state_machine.io.done//)
    mux := state_machine.io.mux 

    // datapath

    register_counter := Mux(mux, 0.U, count)

    in := input_buffer

    count :=  register_counter + Mux(stall, 0.U, 1.U )

    input_buffer := Mux(mux, input, in)

    value :=  register_counter

    equal := in === register_counter

    // for testing purposes
    io.equal_out := equal
    io.output := input_buffer

}*/

class Counter(width: Int = 32) extends Module{

    val io = IO(new Bundle{
        val input = Input(Bits(width.W))
        val reset_ = Input(Bool())
        val stall = Input(Bool())
        val value = Output(Bits(width.W))
        val done = Output(Bool())
        val equal_out = Output(Bool())
        val output = Output(Bits(width.W))
    })

    val input = io.input
    val reset_ = io.reset_
    val stall = io.stall
    val value = io.value
    val done = io.done

    // useful additional signlas

    val input_buffer = RegInit(Bits(width.W), 6.U )
    val register_counter = RegInit(Bits(width.W), 6.U )
    val count = Wire(Bits(width.W))
    val in = Wire(Bits(width.W))

    val equal = Wire(Bool()) 

    val is_input_zero = input === 0.U 

    val res = !is_input_zero && reset_

    // datapath

    register_counter := Mux(res, 1.U, count)

    in := input_buffer

    count :=  register_counter + Mux(stall || equal, 0.U, 1.U )

    input_buffer := Mux(res, input, in)

    value := Mux(res, 0.U, register_counter)

    equal := in === register_counter

    done := Mux(res, false.B , equal)

    // for testing purposes
    io.equal_out := equal
    io.output := input_buffer

}


class Counter_StateMachine() extends Module{


    val io = IO(new Bundle{
        val equal = Input(Bool())
        val reset_ = Input(Bool())
        val is_input_zero = Input(Bool())
        val done = Output(Bool())
        val mux = Output(Bool())
    })

    val equal = io.equal
    val reset_ = io.reset_
    val done = io.done
    val is_input_zero = io.is_input_zero
    val mux = io.mux

    val idle :: counting_first :: counting :: Nil = Enum(3)

    val state = RegInit(Bits(3.W), idle)
    val is_idle = state === idle
    val is_counting_first = state === counting_first
    val is_counting = state === counting

    when(is_idle){ 
        when(!reset_ || (reset_ && is_input_zero)){
            done := true.B 
            mux := true.B 
            state := idle
        }.otherwise{ // reset = true && input != 0
            done := false.B 
            mux := true.B 
            state := counting_first
        }
    }.elsewhen(is_counting_first){
        when(equal){
            done := true.B
            mux := true.B 
            state := idle
        }.otherwise{
            done := false.B 
            mux := false.B 
            state := counting
        }
    
    }.elsewhen(is_counting){
        when(!equal){ 
            done := false.B 
            mux := false.B 
            state := counting
        }.elsewhen(equal && !reset_){ // equal === true.B 
            done := true.B
            mux := true.B 
            state := idle
        }.otherwise{
            done := true.B
            mux := true.B 
            state := counting_first
        }
    }.otherwise{ // should never happen
        done := true.B 
        mux := true.B 
        state := idle
    }
}
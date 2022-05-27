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

class SourceAddressCalculator(width : Int = 32) extends Module{


    val io = IO(new Bundle{
        val offset = Input(Bits(width.W ))
        val address = Input(Bits(width.W ))
        val count_done = Input(Bool())
        
        val rs1 = Output(Bits(width.W ))
    })

    // reniming of signals

    val offset = io.offset
    val address = io.address
    val count_done = io.count_done
    val rs1 = io.rs1

    /*
    val addressCalculator = Module(new DestAddressCalculator(width))

    addressCalculator.io.in_dest_addr := address
    addressCalculator.io.in_dest_offset := offset
    addressCalculator.io.count_done := count_done

    rs1 := addressCalculator.io.out_dest_addr
    */

    // additional signals

    
    val mux1 = Wire(Bool())
    val mux2 = Wire(Bool())
    val register_buffer = RegInit(Bits(width.W), 0.U )
    val mux1_result = Wire(Bits(width.W ))
    val mux2_result = Wire(Bits(width.W ))

    // state machine

    
    val state_machine = Module(new AddressCalculator_StateMachine())

    state_machine.io.count_done := count_done

    mux1 := state_machine.io.mux1
    mux2 := state_machine.io.mux2
    
    // datapath

    mux1_result := Mux(mux1, register_buffer, address)
    mux2_result := Mux(mux2, offset, 0.U )

    rs1 := mux1_result + mux2_result

    register_buffer := mux1_result

}


class AddressCalculator_StateMachine() extends Module{

    val io = IO(new Bundle{

        val count_done = Input(Bool())
        val mux1 = Output(Bool())
        val mux2 = Output(Bool())
    })

    val idle :: first_count :: count :: Nil = Enum(3)

    val state = RegInit(Bits(3.W ), idle)

    // renaiming

    val count_done = io.count_done
    val mux1 = io.mux1
    val mux2 = io.mux2

    val is_idle = state === idle
    val is_first_count = state === first_count
    val is_count = state === count

    when(is_idle){
        mux1 := false.B 
        mux2 := false.B 

        when(count_done){
            mux1 := false.B 
            mux2 := false.B
            state := idle
        }.otherwise{
            mux1 := false.B 
            mux2 := true.B
            state := first_count
        }
    }.elsewhen(is_first_count){
        mux1 := false.B 
        mux2 := true.B 

        when(count_done){
            mux1 := false.B 
            mux2 := false.B
            state := idle
        }.otherwise{
            mux1 := true.B 
            mux2 := true.B 
            state := count
        }
    }.elsewhen(is_count){
        mux1 := true.B 
        mux2 := true.B 

        when(count_done){
            mux1 := false.B 
            mux2 := false.B
            state := idle
        }.otherwise{
            mux1 := true.B 
            mux2 := true.B 
            state := count
        }

    }.otherwise{
        mux1 := false.B 
        mux2 := false.B 
        state := idle
    }


}